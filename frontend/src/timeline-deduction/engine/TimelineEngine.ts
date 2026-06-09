import { beijing416Scenario } from '../scenarios/beijing416'
import type {
  BranchPath,
  DecisionGate,
  OutcomeType,
  TelemetryPoint,
  TimelineEngineState,
  TimelineNode,
  TimelineScenario,
  UserDecisionLog,
} from '../types'

const SCENARIOS: Record<string, TimelineScenario> = {
  [beijing416Scenario.code]: beijing416Scenario,
  [beijing416Scenario.id]: beijing416Scenario,
}

function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n))
}

function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t
}

function interpolateTelemetry(a: TelemetryPoint, b: TelemetryPoint, t: number): TelemetryPoint {
  return {
    t: lerp(a.t, b.t, t),
    temp: lerp(a.temp, b.temp, t),
    voltage: lerp(a.voltage, b.voltage, t),
    soc: lerp(a.soc, b.soc, t),
    h2: lerp(a.h2, b.h2, t),
    smoke: lerp(a.smoke, b.smoke, t),
  }
}

function resolveBranch(risk: number, correctCount: number): BranchPath {
  if (correctCount >= 3 && risk < 30) return 'optimal'
  if (correctCount >= 2 && risk < 55) return 'delayed'
  if (risk < 80) return 'escalated'
  return 'catastrophic'
}

function resolveOutcome(branch: BranchPath): OutcomeType {
  if (branch === 'optimal') return 'contained'
  if (branch === 'delayed') return 'partial_loss'
  return 'major_accident'
}

export class TimelineEngine {
  private scenario: TimelineScenario
  private state: TimelineEngineState
  private tickTimer: ReturnType<typeof setInterval> | null = null
  private decisionStartedAt = 0
  private listeners = new Set<(s: TimelineEngineState) => void>()

  constructor(scenarioId: string, sessionId: string) {
    const scenario = SCENARIOS[scenarioId] ?? beijing416Scenario
    this.scenario = scenario
    const first = scenario.nodes[0]
    this.state = {
      sessionId,
      scenarioId: scenario.id,
      phase: first.phase,
      currentNodeKey: first.key,
      offsetMin: 0,
      riskIndex: 20,
      branch: 'delayed',
      outcome: 'pending',
      paused: false,
      pendingDecision: null,
      decisions: [],
      telemetryHistory: [{ ...first.telemetry }],
      scene: { ...first.scene },
      playbackSpeed: 1,
      status: 'idle',
    }
  }

  getScenario() {
    return this.scenario
  }

  getState(): TimelineEngineState {
    return structuredClone(this.state)
  }

  subscribe(fn: (s: TimelineEngineState) => void) {
    this.listeners.add(fn)
    return () => this.listeners.delete(fn)
  }

  private emit() {
    const snap = this.getState()
    this.listeners.forEach(fn => fn(snap))
  }

  start() {
    if (this.state.status === 'running') return
    this.state.status = 'running'
    this.state.paused = false
    this.tickTimer = setInterval(() => this.tick(), 500)
    this.emit()
  }

  pause() {
    this.state.paused = true
    this.emit()
  }

  resume() {
    this.state.paused = false
    this.emit()
  }

  setSpeed(speed: number) {
    this.state.playbackSpeed = clamp(speed, 0.5, 4)
    this.emit()
  }

  destroy() {
    if (this.tickTimer) clearInterval(this.tickTimer)
    this.tickTimer = null
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

  private pendingGate(): DecisionGate | null {
    const decided = new Set(this.state.decisions.map(d => d.gateId))
    return (
      this.scenario.decisions.find(
        d => d.offsetMin <= this.state.offsetMin + 0.01 && !decided.has(d.id),
      ) ?? null
    )
  }

  private applyBranchModifiers() {
    const branch = this.state.branch
    if (branch === 'optimal') {
      this.state.scene.fireLevel *= 0.55
      this.state.scene.smokeLevel *= 0.5
      if (this.state.phase === 'explosion') {
        this.state.phase = 'spread'
        this.state.scene.explosionFlash = 0
      }
    } else if (branch === 'catastrophic') {
      this.state.scene.fireLevel = Math.min(1, this.state.scene.fireLevel * 1.2)
      this.state.scene.smokeLevel = Math.min(1, this.state.scene.smokeLevel * 1.15)
    }
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

    const step = 0.25 * this.state.playbackSpeed
    this.state.offsetMin += step

    const node = this.findNodeAt(this.state.offsetMin)
    const nextNode = this.scenario.nodes.find(n => n.offsetMin > node.offsetMin)
    let telemetry = node.telemetry
    if (nextNode && this.state.offsetMin < nextNode.offsetMin) {
      const span = nextNode.offsetMin - node.offsetMin
      const t = span > 0 ? (this.state.offsetMin - node.offsetMin) / span : 0
      telemetry = interpolateTelemetry(node.telemetry, nextNode.telemetry, clamp(t, 0, 1))
    }

    const riskFactor = 1 + this.state.riskIndex / 100
    telemetry = {
      ...telemetry,
      temp: telemetry.temp * (this.state.branch === 'optimal' ? 0.85 : riskFactor * 0.95),
      h2: telemetry.h2 * (this.state.branch === 'catastrophic' ? 1.35 : 1),
      smoke: clamp(telemetry.smoke * riskFactor * 0.9, 0, 1),
    }

    this.state.phase = node.phase
    this.state.currentNodeKey = node.key
    this.state.scene = { ...node.scene }
    this.applyBranchModifiers()
    this.state.telemetryHistory.push(telemetry)
    if (this.state.telemetryHistory.length > 120) {
      this.state.telemetryHistory = this.state.telemetryHistory.slice(-120)
    }

    const correctCount = this.state.decisions.filter(d => d.isCorrect).length
    this.state.branch = resolveBranch(this.state.riskIndex, correctCount)
    this.state.outcome = resolveOutcome(this.state.branch)

    if (this.state.offsetMin >= this.scenario.durationMinutes) {
      this.state.phase = 'debrief'
      this.state.status = 'finished'
      this.state.paused = true
      if (this.tickTimer) clearInterval(this.tickTimer)
    }

    this.emit()
  }

  submitDecision(optionId: string): UserDecisionLog | null {
    const gate = this.state.pendingDecision
    if (!gate) return null

    const option = gate.options.find(o => o.id === optionId)
    if (!option) return null

    const log: UserDecisionLog = {
      gateId: gate.id,
      optionId,
      isCorrect: option.isOptimal,
      responseMs: Date.now() - this.decisionStartedAt,
      riskDelta: option.riskDelta,
      consequence: option.consequence,
      atOffsetMin: this.state.offsetMin,
    }

    this.state.decisions.push(log)
    this.state.riskIndex = clamp(this.state.riskIndex + option.riskDelta, 0, 100)
    this.state.pendingDecision = null
    this.state.paused = false
    this.state.status = 'running'

    const correctCount = this.state.decisions.filter(d => d.isCorrect).length
    this.state.branch = resolveBranch(this.state.riskIndex, correctCount)
    this.state.outcome = resolveOutcome(this.state.branch)

    if (!this.tickTimer && this.state.status === 'running') {
      this.tickTimer = setInterval(() => this.tick(), 500)
    }

    this.emit()
    return log
  }

  jumpToDebrief() {
    const last = this.scenario.nodes[this.scenario.nodes.length - 1]
    this.state.offsetMin = last.offsetMin
    this.state.phase = 'debrief'
    this.state.currentNodeKey = last.key
    this.state.scene = { ...last.scene }
    this.state.status = 'finished'
    this.state.paused = true
    if (this.tickTimer) clearInterval(this.tickTimer)
    this.emit()
  }
}

export function getScenarioByCode(code: string) {
  return SCENARIOS[code] ?? beijing416Scenario
}
