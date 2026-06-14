import { onBeforeUnmount, ref, shallowRef } from 'vue'
import { TimelineEngine } from '../engine/TimelineEngine'
import { buildDebriefReport } from '../engine/ScoringEngine'
import type { DebriefReport, TimelineEngineState } from '../types'
import * as api from '@/api/timelineDeduction'

export function useTimelineDeduction(scenarioCode: string) {
  const engine = shallowRef<TimelineEngine | null>(null)
  const state = ref<TimelineEngineState | null>(null)
  const report = ref<DebriefReport | null>(null)
  const sessionId = ref('')
  const loading = ref(false)
  const error = ref('')
  const sessionStarted = ref(false)

  let unsub: (() => void) | null = null

  function initEngine() {
    const eng = new TimelineEngine(scenarioCode, 'pending')
    engine.value = eng
    state.value = eng.getState()
    sessionId.value = ''
    sessionStarted.value = false
    bindFinishWatcher()
  }

  async function ensureBackendSession() {
    if (sessionId.value && sessionId.value !== 'pending' && !sessionId.value.startsWith('local-')) {
      return sessionId.value
    }
    const res = await api.startSession(scenarioCode)
    sessionId.value = res.data.sessionId
    sessionStarted.value = true
    return sessionId.value
  }

  async function setup() {
    loading.value = true
    error.value = ''
    report.value = null
    initEngine()
    loading.value = false
  }

  async function start() {
    try {
      await ensureBackendSession()
      engine.value?.start()
    } catch (e: unknown) {
      if (!sessionId.value || sessionId.value === 'pending') {
        sessionId.value = `local-${Date.now()}`
        sessionStarted.value = true
      }
      engine.value?.start()
      error.value = e instanceof Error ? e.message : '后端未连接，使用本地推演模式'
    }
  }

  function pause() {
    engine.value?.pause()
  }

  function resume() {
    engine.value?.resume()
  }

  function setSpeed(s: number) {
    engine.value?.setSpeed(s)
  }

  async function choose(optionId: string) {
    const eng = engine.value
    if (!eng) return
    const log = eng.submitDecision(optionId)
    if (log && sessionStarted.value && !sessionId.value.startsWith('local-')) {
      try {
        await api.recordDecision(sessionId.value, {
          nodeKey: log.gateId,
          optionId,
          isCorrect: log.isCorrect,
          responseMs: log.responseMs,
          riskDelta: log.riskDelta,
          consequence: log.consequence,
        })
      } catch { /* offline */ }
    }
    await checkFinished()
  }

  async function checkFinished() {
    const eng = engine.value
    const s = eng?.getState()
    if (!eng || !s || s.status !== 'finished') return
    const debrief = buildDebriefReport(eng.getScenario(), s)
    report.value = debrief
    if (sessionStarted.value && !sessionId.value.startsWith('local-')) {
      try {
        await api.finishSession(sessionId.value, debrief as unknown as Record<string, unknown>)
      } catch { /* offline */ }
    }
  }

  function bindFinishWatcher() {
    unsub?.()
    unsub = engine.value?.subscribe(s => {
      state.value = s
      if (s.status === 'finished' && !report.value) void checkFinished()
    }) ?? null
  }

  async function abandonIfRunning() {
    const s = engine.value?.getState()
    if (
      sessionStarted.value &&
      sessionId.value &&
      !sessionId.value.startsWith('local-') &&
      s &&
      s.status !== 'finished'
    ) {
      try {
        await api.abandonSession(sessionId.value)
      } catch { /* ignore */ }
    }
  }

  async function destroy() {
    await abandonIfRunning()
    unsub?.()
    engine.value?.destroy()
    engine.value = null
    sessionStarted.value = false
  }

  async function restart() {
    await destroy()
    await setup()
    await start()
  }

  onBeforeUnmount(() => {
    void destroy()
  })

  return {
    engine,
    state,
    report,
    sessionId,
    loading,
    error,
    setup,
    start,
    pause,
    resume,
    setSpeed,
    choose,
    destroy,
    restart,
  }
}
