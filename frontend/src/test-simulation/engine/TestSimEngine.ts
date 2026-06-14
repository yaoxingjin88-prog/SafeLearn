import { guangzhou614Scenario } from '../scenarios/guangzhou614'
import type {
  DecisionGate,
  DecisionLog,
  RiskLevel,
  SceneState,
  SimEngineState,
  SimOutcome,
  SimPhase,
  TelemetrySnapshot,
  TestSimScenario,
  TimelineNode,
} from '../types'

const SCENARIOS: Record<string, TestSimScenario> = {
  [guangzhou614Scenario.code]: guangzhou614Scenario,
  [guangzhou614Scenario.id]: guangzhou614Scenario,
}

const TICK_MS = 450
/** 与北京推演接近：45 模拟分钟 ≈ 80s 跑完（1x） */
const BASE_STEP = 0.28

function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n))
}

function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t
}

function riskLevel(idx: number): RiskLevel {
  if (idx < 30) return 'low'
  if (idx < 55) return 'medium'
  if (idx < 80) return 'high'
  return 'critical'
}

function resolveOutcome(risk: number, branch: string | null, phase: SimPhase): SimOutcome {
  if (branch === 'early_success') return 'controlled'
  if (phase === 'debrief' && risk < 40) return 'controlled'
  if (risk < 60) return 'partial'
  return 'catastrophic'
}

function formatClock(offsetMin: number): string {
  const base = 14 * 60
  const total = Math.floor(base + offsetMin)
  const h = Math.floor(total / 60)
  const m = total % 60
  return `${h}:${String(m).padStart(2, '0')}`
}

function interpolateTelemetry(a: TelemetrySnapshot, b: TelemetrySnapshot, t: number, clock: string): TelemetrySnapshot {
  return {
    clock,
    envTemp: lerp(a.envTemp, b.envTemp, t),
    packTemp: lerp(a.packTemp, b.packTemp, t),
    voltage: lerp(a.voltage, b.voltage, t),
    current: lerp(a.current, b.current, t),
    soc: lerp(a.soc, b.soc, t),
    soh: lerp(a.soh, b.soh, t),
    insulation: t < 0.5 ? a.insulation : b.insulation,
    gasPpm: lerp(a.gasPpm, b.gasPpm, t),
    vocPpm: lerp(a.vocPpm, b.vocPpm, t),
    pressure: lerp(a.pressure, b.pressure, t),
    alarmCount: Math.round(lerp(a.alarmCount, b.alarmCount, t)),
  }
}

function interpolateScene(a: SceneState, b: SceneState, t: number): SceneState {
  const heat = lerp(a.packHeat, b.packHeat, t)
  let packColor = a.packColor
  if (heat > 0.75) packColor = 'critical'
  else if (heat > 0.55) packColor = 'danger'
  else if (heat > 0.3) packColor = 'warning'
  else packColor = 'normal'
  return {
    packHeat: heat,
    packColor,
    smoke: lerp(a.smoke, b.smoke, t),
    flash: lerp(a.flash, b.flash, t),
    ventOn: t < 0.5 ? a.ventOn : b.ventOn,
    personnelEvacuated: t < 0.5 ? a.personnelEvacuated : b.personnelEvacuated,
  }
}

export class TestSimEngine {
  private scenario: TestSimScenario
  private state: SimEngineState
  private tickTimer: ReturnType<typeof setInterval> | null = null
  private branch: string | null = null
  private decisionStartedAt = 0
  private listeners = new Set<(s: SimEngineState) => void>()
  /** 成功分支：缓慢推进至调查阶段 */
  private windDownMode = false

  constructor(code: string, sessionId: string) {
    this.scenario = SCENARIOS[code] ?? guangzhou614Scenario
    const n0 = this.scenario.nodes[0]
    this.state = {
      sessionId,
      phase: n0.phase,
      currentNodeKey: n0.key,
      clock: n0.clock,
      offsetMin: 0,
      riskIndex: 15,
      riskLevel: 'low',
      outcome: 'pending',
      status: 'idle',
      paused: false,
      pendingDecision: null,
      decisions: [],
      telemetryHistory: [{ ...n0.telemetry }],
      scene: { ...n0.scene },
      evidenceUnlocked: [],
      playbackSpeed: 1,
    }
  }

  getScenario() {
    return this.scenario
  }

  getState(): SimEngineState {
    return structuredClone(this.state)
  }

  subscribe(fn: (s: SimEngineState) => void) {
    this.listeners.add(fn)
    return () => this.listeners.delete(fn)
  }

  private emit() {
    this.listeners.forEach(fn => fn(this.getState()))
  }

  start() {
    if (this.state.status === 'running') return
    this.state.status = 'running'
    this.state.paused = false
    this.scheduleTick()
    this.emit()
  }

  private scheduleTick() {
    if (this.tickTimer) clearInterval(this.tickTimer)
    this.tickTimer = setInterval(() => this.tick(), TICK_MS)
  }

  pause() {
    this.state.paused = true
    this.emit()
  }

  resume() {
    this.state.paused = false
    this.emit()
  }

  setSpeed(s: number) {
    this.state.playbackSpeed = clamp(s, 0.5, 3)
    this.emit()
  }

  destroy() {
    if (this.tickTimer) clearInterval(this.tickTimer)
    this.listeners.clear()
  }

  private findNodeAt(offsetMin: number): TimelineNode {
    const nodes = this.scenario.nodes
    let current = nodes[0]
    for (const n of nodes) {
      if (n.offsetMin <= offsetMin) current = n
      else break
    }
    return current
  }

  private unlockEvidence(phase: SimPhase) {
    for (const ev of this.scenario.evidence) {
      if (ev.phase === phase && !this.state.evidenceUnlocked.includes(ev.id)) {
        this.state.evidenceUnlocked.push(ev.id)
      }
    }
  }

  private pendingGate(): DecisionGate | null {
    const done = new Set(this.state.decisions.map(d => d.gateId))
    return (
      this.scenario.decisions.find(d => {
        const node = this.scenario.nodes.find(n => n.key === d.nodeKey)
        if (!node || done.has(d.id)) return false
        return this.state.offsetMin >= node.offsetMin - 0.05
      }) ?? null
    )
  }

  private applyInterpolatedState() {
    const nodes = this.scenario.nodes
    const node = this.findNodeAt(this.state.offsetMin)
    const nextNode = nodes.find(n => n.offsetMin > node.offsetMin)
    const clock = formatClock(this.state.offsetMin)

    let telemetry = { ...node.telemetry, clock }
    let scene = { ...node.scene }

    if (nextNode && this.state.offsetMin < nextNode.offsetMin) {
      const span = nextNode.offsetMin - node.offsetMin
      const t = span > 0 ? clamp((this.state.offsetMin - node.offsetMin) / span, 0, 1) : 0
      telemetry = interpolateTelemetry(node.telemetry, nextNode.telemetry, t, clock)
      scene = interpolateScene(node.scene, nextNode.scene, t)
    }

    if (this.branch === 'catastrophe' && this.state.offsetMin >= 24) {
      telemetry.packTemp *= 1.08
      telemetry.gasPpm *= 1.12
      scene.packHeat = Math.min(1, scene.packHeat * 1.1)
    }

    if (this.windDownMode) {
      telemetry.packTemp = Math.min(telemetry.packTemp, 40)
      scene.smoke = Math.max(0, scene.smoke * 0.6)
      scene.flash = 0
      scene.packHeat = Math.min(scene.packHeat, 0.25)
      scene.packColor = 'normal'
    }

    this.state.phase = node.phase
    this.state.currentNodeKey = node.key
    this.state.clock = clock
    this.state.scene = scene
    this.state.telemetryHistory.push(telemetry)
    if (this.state.telemetryHistory.length > 150) {
      this.state.telemetryHistory = this.state.telemetryHistory.slice(-150)
    }
    this.unlockEvidence(node.phase)
  }

  private tick() {
    if (this.state.paused || this.state.status !== 'running') return

    const gate = this.pendingGate()
    if (gate) {
      this.state.status = 'decision'
      this.state.pendingDecision = gate
      this.state.paused = true
      this.decisionStartedAt = Date.now()
      this.emit()
      return
    }

    let step = BASE_STEP * this.state.playbackSpeed

    if (this.windDownMode) {
      step = 0.5 * this.state.playbackSpeed
    } else if (this.branch === 'catastrophe' && this.state.offsetMin >= 24 && this.state.offsetMin < 30) {
      step = 0.4 * this.state.playbackSpeed
    } else if (this.state.offsetMin >= 10 && this.state.offsetMin < 24) {
      step = 0.34 * this.state.playbackSpeed
    } else if (this.state.offsetMin >= 27 && this.state.offsetMin < 31) {
      step = 0.22 * this.state.playbackSpeed
    }

    this.state.offsetMin += step
    this.applyInterpolatedState()

    if (!this.windDownMode && this.state.offsetMin >= 24) {
      const drift = this.branch === 'catastrophe' ? 0.35 : 0.12
      this.state.riskIndex = clamp(this.state.riskIndex + drift * this.state.playbackSpeed, 5, 100)
    }

    this.state.riskLevel = riskLevel(this.state.riskIndex)
    this.state.outcome = resolveOutcome(this.state.riskIndex, this.branch, this.state.phase)

    const endAt = this.windDownMode
      ? this.scenario.durationMinutes
      : this.scenario.durationMinutes

    if (this.state.offsetMin >= endAt) {
      this.state.phase = 'debrief'
      this.state.status = 'finished'
      this.state.paused = true
      if (this.tickTimer) clearInterval(this.tickTimer)
    }

    this.emit()
  }

  private enterWindDown() {
    this.windDownMode = true
    this.state.riskIndex = clamp(this.state.riskIndex, 8, 28)
    if (this.state.offsetMin < 38) {
      this.state.offsetMin = 38
    }
    this.applyInterpolatedState()
  }

  submitDecision(optionId: string): DecisionLog | null {
    const gate = this.state.pendingDecision
    if (!gate) return null
    const opt = gate.options.find(o => o.id === optionId)
    if (!opt) return null

    const log: DecisionLog = {
      gateId: gate.id,
      optionId,
      isCorrect: opt.isOptimal,
      responseMs: Date.now() - this.decisionStartedAt,
      consequence: opt.consequence,
    }
    this.state.decisions.push(log)
    this.state.riskIndex = clamp(this.state.riskIndex + opt.riskDelta, 5, 100)
    this.state.riskLevel = riskLevel(this.state.riskIndex)
    if (opt.branch) this.branch = opt.branch
    this.state.pendingDecision = null
    this.state.paused = false
    this.state.status = 'running'

    if (opt.branch === 'early_success') {
      this.enterWindDown()
    }

    if (!this.tickTimer) this.scheduleTick()
    this.emit()
    return log
  }

  askMentor(question: string): string {
    const faq = this.scenario.mentorFaqs.find(f =>
      question.includes(f.q.slice(0, 4)) || f.q.includes(question.slice(0, 4)),
    )
    if (faq) return `${faq.a}${faq.ref ? `\n\n依据：${faq.ref}` : ''}`
    const hints: Record<string, string> = {
      温度: '温升是热失控核心征象，65℃ 以上应立即中止测试。',
      撤离: '闪爆后首要任务是人员撤离与警戒，严禁靠近涉事设备。',
      规范: '测试作业须执行三查：设备、电池、消防与人员资质。',
    }
    for (const [k, v] of Object.entries(hints)) {
      if (question.includes(k)) return v
    }
    return '建议结合当前风险指数与 BMS 告警等级判断。异常温升时优先停机隔离，生命第一。'
  }
}

export function getTestScenario(code: string) {
  return SCENARIOS[code] ?? guangzhou614Scenario
}
