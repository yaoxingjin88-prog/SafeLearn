import { ref, shallowRef, onUnmounted } from 'vue'
import { ReplayEngine } from '@/simulation/engine/ReplayEngine'
import { SceneBridge } from '@/simulation/bridge/SceneBridge'
import type { BatteryCellState } from '@/composables/useSimulation'
import type { DeductionEventLogEntry } from '@/simulation/types/deduction.types'
import { singleBatteryThermalRunaway } from '@/simulation/scenarios/singleBatteryThermalRunaway'
import { deductionApi } from '@/api/deduction'

function initCells(): BatteryCellState[] {
  return Array.from({ length: singleBatteryThermalRunaway.initialConditions.cellCount }, (_, i) => ({
    id: i,
    temperature: singleBatteryThermalRunaway.initialConditions.initialTemperature,
    status: 'normal' as const,
    smokeIntensity: 0,
    fireIntensity: 0,
  }))
}

export function useDeductionReplay(sessionId: string) {
  const cells = ref<BatteryCellState[]>(initCells())
  const isPlaying = ref(false)
  const currentMs = ref(0)
  const durationMs = ref(0)
  const phase = ref('replay')
  const alerts = ref<{ time: number; message: string }[]>([])
  const sessionMeta = ref<Record<string, unknown>>({})
  const speed = ref(1)

  const replayEngine = shallowRef(new ReplayEngine())
  let unbind: (() => void) | null = null

  function bind() {
    if (unbind) unbind()
    const bridge = new SceneBridge({
      cells,
      onAlert: (_level, message) => {
        alerts.value.unshift({ time: Math.floor(currentMs.value / 1000), message })
        if (alerts.value.length > 20) alerts.value.pop()
      },
      onPhaseChange: p => {
        phase.value = p
      },
    })
    const unsubScene = replayEngine.value.subscribe(event => bridge.handle(event))
    const unsubProgress = replayEngine.value.onProgress((ms, dur) => {
      currentMs.value = ms
      durationMs.value = dur
    })
    const unsubEnded = replayEngine.value.onEnded(() => {
      isPlaying.value = false
    })
    unbind = () => {
      unsubScene()
      unsubProgress()
      unsubEnded()
    }
  }

  async function load() {
    const res = await deductionApi.getReplay(sessionId)
    sessionMeta.value = res.data as Record<string, unknown>
    const events = (res.data.events || []) as DeductionEventLogEntry[]
    const cap = (res.data.durationMs as number) || 0
    durationMs.value = cap
    replayEngine.value.load(events, [], cap)
    bind()
  }

  function play() {
    replayEngine.value.play(currentMs.value)
    isPlaying.value = true
  }

  function pause() {
    replayEngine.value.pause()
    isPlaying.value = false
  }

  function togglePlay() {
    if (isPlaying.value) pause()
    else play()
  }

  function seek(sec: number) {
    replayEngine.value.seek(sec * 1000)
    isPlaying.value = replayEngine.value.isPlaying()
  }

  function setSpeed(v: number) {
    speed.value = v
    replayEngine.value.setSpeed(v)
  }

  function reset() {
    cells.value = initCells()
    currentMs.value = 0
    replayEngine.value.seek(0)
    pause()
  }

  onUnmounted(() => {
    unbind?.()
    replayEngine.value.destroy()
  })

  return {
    cells,
    isPlaying,
    currentMs,
    durationMs,
    phase,
    alerts,
    sessionMeta,
    speed,
    load,
    play,
    pause,
    togglePlay,
    seek,
    setSpeed,
    reset,
  }
}
