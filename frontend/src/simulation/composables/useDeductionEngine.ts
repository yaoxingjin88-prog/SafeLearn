import { ref, shallowRef, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { SimulationEngine } from '@/simulation/engine/SimulationEngine'
import { SceneBridge } from '@/simulation/bridge/SceneBridge'
import type { BatteryCellState } from '@/composables/useSimulation'
import type { DeductionContext, DeductionEventLogEntry, DecisionPointDef } from '@/simulation/types/deduction.types'
import { singleBatteryThermalRunaway } from '@/simulation/scenarios/singleBatteryThermalRunaway'
import request from '@/api/request'

export interface DeductionAlert {
  time: number
  type: 'warning' | 'danger' | 'critical' | 'info'
  message: string
}

function initCells(): BatteryCellState[] {
  return Array.from({ length: singleBatteryThermalRunaway.initialConditions.cellCount }, (_, i) => ({
    id: i,
    temperature: singleBatteryThermalRunaway.initialConditions.initialTemperature,
    status: 'normal' as const,
    smokeIntensity: 0,
    fireIntensity: 0,
  }))
}

export function useDeductionEngine(scenarioId: string) {
  const cells = ref<BatteryCellState[]>(initCells())
  const context = ref<DeductionContext | null>(null)
  const phase = ref('idle')
  const isPlaying = ref(false)
  const events = ref<DeductionAlert[]>([])
  const sessionId = ref<string | null>(null)
  const lastSyncedSeq = ref(0)

  let timeoutTimer: ReturnType<typeof setTimeout> | null = null
  let syncTimer: ReturnType<typeof setInterval> | null = null
  let unbindBridge: (() => void) | null = null

  const scenario = singleBatteryThermalRunaway
  const totalDuration = computed(() => scenario.durationSec)
  const currentTime = computed(() => Math.floor((context.value?.elapsedMs ?? 0) / 1000))

  const environment = computed(() => {
    const env = context.value?.environment
    const maxTemp = context.value?.maxTemperature ?? 35
    return {
      temperature: Math.max(env?.temperature ?? 25, maxTemp * 0.3),
      humidity: env?.humidity ?? 45,
      gasLevel: env?.ventilationOn
        ? Math.max(80, (env?.gasLevel ?? 100) - 20)
        : Math.min(800, (env?.gasLevel ?? 100) + maxTemp),
    }
  })

  const pendingDecision = computed<DecisionPointDef | null>(
    () => context.value?.pendingDecision ?? null,
  )

  const isCompleted = computed(() => phase.value === 'completed')

  function refreshContext(engine: SimulationEngine) {
    context.value = engine.getContext()
    phase.value = context.value.phase
    isPlaying.value = phase.value !== 'completed' && phase.value !== 'idle'
  }

  function bindEngine(engine: SimulationEngine) {
    if (unbindBridge) unbindBridge()
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
  }

  const engineRef = shallowRef<SimulationEngine>(new SimulationEngine())
  bindEngine(engineRef.value)

  watch(pendingDecision, dp => {
    if (timeoutTimer) {
      clearTimeout(timeoutTimer)
      timeoutTimer = null
    }
    if (!dp) return
    pause()
    timeoutTimer = setTimeout(() => {
      engineRef.value.submitTimeout()
      refreshContext(engineRef.value)
      ElMessage.warning('决策超时，事故可能扩大')
    }, dp.timePressureSec * 1000)
  })

  async function startSession() {
    try {
      const res = await request.post('/deduction/sessions', { scenarioId })
      sessionId.value = res.data.sessionId as string
      events.value = []
      lastSyncedSeq.value = 0
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
    if (phase.value === 'completed' || pendingDecision.value) return
    engineRef.value.resume()
    isPlaying.value = true
  }

  function togglePlay() {
    if (phase.value === 'completed' || pendingDecision.value) return
    if (isPlaying.value) pause()
    else resume()
  }

  function setSpeed(speed: number) {
    engineRef.value.setSpeed(speed)
  }

  function submitDecision(decisionPointId: string, optionId: string) {
    if (timeoutTimer) {
      clearTimeout(timeoutTimer)
      timeoutTimer = null
    }
    engineRef.value.submitDecision(decisionPointId, optionId)
    refreshContext(engineRef.value)
    syncEvents()
    if (!isCompleted.value && !pendingDecision.value) {
      engineRef.value.resume()
      isPlaying.value = true
    }
  }

  async function finishSession() {
    engineRef.value.pause()
    refreshContext(engineRef.value)
    isPlaying.value = false
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

  async function reset() {
    if (timeoutTimer) clearTimeout(timeoutTimer)
    engineRef.value.destroy()
    cells.value = initCells()
    engineRef.value = new SimulationEngine()
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

  syncTimer = setInterval(() => {
    if (isPlaying.value) refreshContext(engineRef.value)
  }, 400)

  onUnmounted(() => {
    if (timeoutTimer) clearTimeout(timeoutTimer)
    if (syncTimer) clearInterval(syncTimer)
    unbindBridge?.()
    engineRef.value.destroy()
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
