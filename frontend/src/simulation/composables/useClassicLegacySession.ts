import { ref, onUnmounted } from 'vue'
import { deductionApi } from '@/api/deduction'
import type { BatteryCellState } from '@/composables/useSimulation'

/** 多电池演示推演：创建会话、结束时评分入库 */
export function useClassicLegacySession(scenarioId: string) {
  const sessionId = ref<string | null>(null)
  const finished = ref(false)

  async function start() {
    const res = await deductionApi.startSession(scenarioId)
    sessionId.value = res.data.sessionId
    finished.value = false
  }

  function computeRuleScore(
    cells: BatteryCellState[],
    gasLevel: number,
    durationSec: number,
    elapsedSec: number,
  ) {
    const maxTemp = cells.length ? Math.max(...cells.map(c => c.temperature)) : 25
    const critical = cells.filter(c => c.status === 'critical' || c.status === 'danger').length
    const spreadRatio = cells.length ? critical / cells.length : 0
    const watchedRatio = durationSec > 0 ? Math.min(1, elapsedSec / durationSec) : 1
    const severity = Math.min(
      100,
      (maxTemp - 40) * 0.35 + spreadRatio * 45 + Math.min(gasLevel / 100, 30),
    )
    const base = Math.round(100 - severity * 0.75)
    const participation = Math.round(watchedRatio * 15)
    return Math.max(0, Math.min(100, base + participation))
  }

  async function finishFromObservation(
    cells: BatteryCellState[],
    environment: { gasLevel: number },
    durationSec: number,
    elapsedSec: number,
  ) {
    if (!sessionId.value || finished.value) return null
    finished.value = true
    const maxTemp = cells.length ? Math.max(...cells.map(c => c.temperature)) : 25
    const critical = cells.filter(c => c.status === 'critical' || c.status === 'danger').length
    const ruleScore = computeRuleScore(cells, environment.gasLevel, durationSec, elapsedSec)
    const outcome = critical <= 1 && maxTemp < 120 ? 'success' : 'failure'
    try {
      const res = await deductionApi.finishSession(sessionId.value, {
        outcome,
        branch: 'observation',
        elapsedMs: Math.round(elapsedSec * 1000),
        maxTemperature: maxTemp,
        ruleScore,
        machineState: 'completed',
        decisions: [],
        mode: 'legacy_demo',
      })
      return res.data
    } catch {
      finished.value = false
      return null
    }
  }

  async function abandon() {
    if (!sessionId.value || finished.value) return
    try {
      await deductionApi.abandonSession(sessionId.value)
    } catch {
      /* ignore */
    }
    sessionId.value = null
  }

  onUnmounted(() => {
    void abandon()
  })

  return {
    sessionId,
    finished,
    start,
    finishFromObservation,
    abandon,
    computeRuleScore,
  }
}
