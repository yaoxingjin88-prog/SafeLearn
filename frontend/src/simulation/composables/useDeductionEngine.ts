import { ref, shallowRef, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { SimulationEngine } from '@/simulation/engine/SimulationEngine'
import { SceneBridge } from '@/simulation/bridge/SceneBridge'
import { resolveNextDecision } from '@/simulation/composables/classicDecisionScheduler'
import type { BatteryCellState } from '@/composables/useSimulation'
import type { DeductionContext, DeductionEventLogEntry, DecisionPointDef } from '@/simulation/types/deduction.types'
import { getDeductionScenario } from '@/simulation/scenarios'
import { singleBatteryThermalRunaway } from '@/simulation/scenarios/singleBatteryThermalRunaway'
import request from '@/api/request'

export interface DeductionAlert {
  time: number
  type: 'warning' | 'danger' | 'critical' | 'info'
  message: string
}

function createInitialCells(count: number, temp: number): BatteryCellState[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i,
    temperature: temp,
    status: 'normal' as const,
    smokeIntensity: 0,
    fireIntensity: 0,
  }))
}

export function useDeductionEngine(scenarioId: string) {
  const scenario = getDeductionScenario(scenarioId) ?? singleBatteryThermalRunaway

  const cells = ref<BatteryCellState[]>(
    createInitialCells(
      scenario.initialConditions.cellCount,
      scenario.initialConditions.initialTemperature,
    ),
  )
  const context = ref<DeductionContext | null>(null)
  const phase = ref('idle')
  const isPlaying = ref(false)
  const events = ref<DeductionAlert[]>([])
  const sessionId = ref<string | null>(null)
  const lastSyncedSeq = ref(0)
  const activeDecision = ref<DecisionPointDef | null>(null)

  let timeoutTimer: ReturnType<typeof setTimeout> | null = null
  let unbindBridge: (() => void) | null = null
  let unbindContext: (() => void) | null = null

  const totalDuration = computed(() => scenario.durationSec)
  const currentTime = computed(() => Math.floor((context.value?.elapsedMs ?? 0) / 1000))

  const environment = computed(() => {
    const env = context.value?.environment
    const maxTemp = context.value?.maxTemperature ?? scenario.initialConditions.initialTemperature
    return {
      temperature: Math.max(env?.temperature ?? 25, maxTemp * 0.3),
      humidity: env?.humidity ?? 45,
      gasLevel: env?.ventilationOn
        ? Math.max(80, (env?.gasLevel ?? 100) - 20)
        : Math.min(800, (env?.gasLevel ?? 100) + maxTemp),
    }
  })

  const pendingDecision = computed<DecisionPointDef | null>(() => activeDecision.value)

  const isCompleted = computed(() => phase.value === 'completed')

  function clearDecisionTimer() {
    if (timeoutTimer) {
      clearTimeout(timeoutTimer)
      timeoutTimer = null
    }
  }

  function armDecisionTimer(dp: DecisionPointDef) {
    clearDecisionTimer()
    timeoutTimer = setTimeout(() => {
      engineRef.value.submitTimeout()
      refreshContext(engineRef.value)
      activeDecision.value = null
      ElMessage.warning('决策超时，事故可能扩大')
    }, dp.timePressureSec * 1000)
  }

  function refreshContext(engine: SimulationEngine) {
    context.value = engine.getContext()
    phase.value = context.value.phase
    isPlaying.value = engine.isTimePlaying() && !activeDecision.value
    syncDecisionGate(engine)
  }

  function checkCompletion(engine: SimulationEngine) {
    if (!context.value || context.value.phase === 'completed') return
    if (context.value.decisions.length === 0) return
    if (resolveNextDecision(scenario, context.value)) return
    engine.finish()
    refreshContext(engine)
  }

  function syncDecisionGate(engine: SimulationEngine) {
    if (!context.value || context.value.phase === 'completed') {
      activeDecision.value = null
      clearDecisionTimer()
      return
    }

    if (activeDecision.value) return

    const next = resolveNextDecision(scenario, context.value)
    if (!next) return

    activeDecision.value = next
    engine.pause()
    isPlaying.value = false
    phase.value = next.phase
    armDecisionTimer(next)

    events.value.unshift({
      time: Math.floor(engine.getElapsedSec()),
      type: 'warning',
      message: next.question,
    })
    if (events.value.length > 30) events.value.pop()
  }

  function bindEngine(engine: SimulationEngine) {
    if (unbindBridge) unbindBridge()
    if (unbindContext) unbindContext()

    const bridge = new SceneBridge({
      cells,
      onAlert: (level, message) => {
        events.value.unshift({
          time: Math.floor(engine.getElapsedSec()),
          type: level === 'danger' ? 'critical' : level === 'info' ? 'info' : 'warning',
          message,
        })
        if (events.value.length > 30) events.value.pop()
      },
      onPhaseChange: () => refreshContext(engine),
    })
    unbindBridge = bridge.bind(engine)
    unbindContext = engine.onContextChange(() => refreshContext(engine))
  }

  const engineRef = shallowRef(new SimulationEngine(scenario))
  bindEngine(engineRef.value)

  async function startSession() {
    try {
      const res = await request.post('/deduction/sessions', { scenarioId })
      sessionId.value = res.data.sessionId as string
      events.value = []
      lastSyncedSeq.value = 0
      activeDecision.value = null
      clearDecisionTimer()
      engineRef.value.start(sessionId.value, scenarioId)
      refreshContext(engineRef.value)
      isPlaying.value = true
      ElMessage.success('推演已开始，请密切关注温度变化')
    } catch {
      ElMessage.error('启动推演失败，请确认已登录且后端服务正常')
    }
  }

  function pause() {
    engineRef.value.pause()
    isPlaying.value = false
  }

  function resume() {
    if (phase.value === 'completed' || activeDecision.value) return
    engineRef.value.resume()
    isPlaying.value = true
  }

  function togglePlay() {
    if (phase.value === 'completed' || activeDecision.value) return
    if (isPlaying.value) pause()
    else resume()
  }

  function setSpeed(speed: number) {
    engineRef.value.setSpeed(speed)
  }

  function submitDecision(decisionPointId: string, optionId: string) {
    clearDecisionTimer()
    activeDecision.value = null
    engineRef.value.submitDecision(decisionPointId, optionId)
    refreshContext(engineRef.value)
    syncEvents()
    if (!isCompleted.value) {
      engineRef.value.resume()
      isPlaying.value = true
      syncDecisionGate(engineRef.value)
      checkCompletion(engineRef.value)
    }
  }

  async function finishSession() {
    engineRef.value.pause()
    refreshContext(engineRef.value)
    isPlaying.value = false
    activeDecision.value = null
    clearDecisionTimer()
    if (!sessionId.value || !context.value) return null
    await syncEvents()
    try {
      const ctx = context.value
      const res = await request.post(`/deduction/sessions/${sessionId.value}/finish`, {
        outcome: ctx.outcome === 'success' ? 'success' : 'failure',
        branch: ctx.branch,
        elapsedMs: ctx.elapsedMs,
        maxTemperature: ctx.maxTemperature,
        ruleScore: ctx.score,
        machineState: ctx.phase,
        decisions: ctx.decisions,
      })
      return res.data
    } catch {
      ElMessage.error('提交评分失败')
      return null
    }
  }

  async function abandonSession() {
    if (!sessionId.value || phase.value === 'completed') return
    try {
      await request.post(`/deduction/sessions/${sessionId.value}/abandon`)
    } catch {
      /* ignore */
    }
  }

  async function reset() {
    clearDecisionTimer()
    activeDecision.value = null
    await abandonSession()
    engineRef.value.destroy()
    cells.value = createInitialCells(
      scenario.initialConditions.cellCount,
      scenario.initialConditions.initialTemperature,
    )
    engineRef.value = new SimulationEngine(scenario)
    bindEngine(engineRef.value)
    sessionId.value = null
    context.value = null
    phase.value = 'idle'
    isPlaying.value = false
    events.value = []
    lastSyncedSeq.value = 0
    await startSession()
  }

  async function startReplay(replayEvents: DeductionEventLogEntry[]) {
    engineRef.value.startReplay(replayEvents)
    isPlaying.value = true
    phase.value = 'replay'
  }

  async function syncEvents() {
    if (!sessionId.value) return
    const all = engineRef.value.getEventLog()
    const newEvents = all.filter(e => e.seq > lastSyncedSeq.value)
    if (!newEvents.length) return
    try {
      await request.post(`/deduction/sessions/${sessionId.value}/events`, { events: newEvents })
      lastSyncedSeq.value = Math.max(...newEvents.map(e => e.seq))
    } catch {
      // finish 时重试
    }
  }

  watch(isCompleted, completed => {
    if (completed) {
      activeDecision.value = null
      clearDecisionTimer()
    }
  })

  onUnmounted(() => {
    clearDecisionTimer()
    unbindBridge?.()
    unbindContext?.()
    engineRef.value.destroy()
    void abandonSession()
  })

  return {
    cells,
    context,
    phase,
    isPlaying,
    events,
    environment,
    pendingDecision,
    isCompleted,
    scenario,
    totalDuration,
    currentTime,
    sessionId,
    startSession,
    pause,
    resume,
    togglePlay,
    setSpeed,
    submitDecision,
    finishSession,
    reset,
    startReplay,
    getEventLog: () => engineRef.value.getEventLog(),
  }
}
