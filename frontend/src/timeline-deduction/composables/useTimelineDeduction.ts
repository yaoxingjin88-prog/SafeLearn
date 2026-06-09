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

  let unsub: (() => void) | null = null

  async function initSession() {
    loading.value = true
    error.value = ''
    report.value = null
    try {
      const res = await api.startSession(scenarioCode)
      sessionId.value = res.data.sessionId
      const eng = new TimelineEngine(scenarioCode, res.data.sessionId)
      engine.value = eng
      state.value = eng.getState()
      bindFinishWatcher()
    } catch (e: unknown) {
      const eng = new TimelineEngine(scenarioCode, `local-${Date.now()}`)
      engine.value = eng
      sessionId.value = eng.getState().sessionId
      state.value = eng.getState()
      bindFinishWatcher()
      error.value = e instanceof Error ? e.message : '后端未连接，使用本地推演模式'
    } finally {
      loading.value = false
    }
  }

  function start() {
    engine.value?.start()
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
    if (log && !sessionId.value.startsWith('local-')) {
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
    if (!sessionId.value.startsWith('local-')) {
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

  function destroy() {
    unsub?.()
    engine.value?.destroy()
    engine.value = null
  }

  onBeforeUnmount(destroy)

  return {
    engine,
    state,
    report,
    sessionId,
    loading,
    error,
    initSession,
    start,
    pause,
    resume,
    setSpeed,
    choose,
    destroy,
  }
}
