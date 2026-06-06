import { ref, computed, onUnmounted } from 'vue'

export interface BatteryCellState {
  id: number
  temperature: number
  status: 'normal' | 'warning' | 'danger' | 'critical'
  smokeIntensity: number
  fireIntensity: number
}

export interface SimulationEvent {
  time: number
  type: 'warning' | 'danger' | 'critical'
  message: string
  cellId?: number
}

export interface SimulationState {
  currentTime: number
  isPlaying: boolean
  cells: BatteryCellState[]
  events: SimulationEvent[]
  environment: {
    temperature: number
    humidity: number
    gasLevel: number
  }
}

export function useSimulation(initDuration = 120, initCellCount = 16, initTemp = 30) {
  const totalDuration = ref(initDuration)
  const initialTemp = ref(initTemp)
  const currentTime = ref(0)
  const isPlaying = ref(false)
  const speed = ref(1)
  let animFrame: number | null = null
  let lastTimestamp = 0

  const cells = ref<BatteryCellState[]>(
    Array.from({ length: initCellCount }, (_, i) => ({
      id: i,
      temperature: initTemp,
      status: 'normal' as const,
      smokeIntensity: 0,
      fireIntensity: 0,
    }))
  )

  const events = ref<SimulationEvent[]>([])

  const environment = ref({
    temperature: 25,
    humidity: 45,
    gasLevel: 100,
  })

  const progress = computed(() => totalDuration.value === 0 ? 0 : currentTime.value / totalDuration.value)

  function getStatus(temp: number): BatteryCellState['status'] {
    if (temp >= 200) return 'critical'
    if (temp >= 100) return 'danger'
    if (temp >= 60) return 'warning'
    return 'normal'
  }

  function updateState(time: number) {
    const t = time / totalDuration

    cells.value = cells.value.map((cell, i) => {
      const hotspot = i === 3
      const neighbor = Math.abs(i - 3) <= 1

      const base = initialTemp.value
      let temp = base
      if (hotspot) {
        temp = base + t * t * 400
      } else if (neighbor) {
        temp = base + t * t * 150 * (1 - Math.abs(i - 3) * 0.3)
      } else {
        temp = base + t * 30
      }

      const smokeIntensity = hotspot ? Math.min(1, t * 1.5) : neighbor ? Math.min(0.5, t) : 0
      const fireIntensity = hotspot && t > 0.4 ? Math.min(1, (t - 0.4) * 2.5) : 0

      return {
        ...cell,
        temperature: Math.round(temp),
        status: getStatus(temp),
        smokeIntensity,
        fireIntensity,
      }
    })

    environment.value = {
      temperature: 25 + t * 15,
      humidity: Math.max(20, 45 - t * 30),
      gasLevel: 100 + t * t * 5000,
    }

    const newEvents: SimulationEvent[] = []
    if (time >= 20 && !events.value.find(e => e.time === 20)) {
      newEvents.push({ time: 20, type: 'warning', message: '电池#4温度异常升高', cellId: 3 })
    }
    if (time >= 45 && !events.value.find(e => e.time === 45)) {
      newEvents.push({ time: 45, type: 'danger', message: '电池#4达到热失控阈值', cellId: 3 })
    }
    if (time >= 60 && !events.value.find(e => e.time === 60)) {
      newEvents.push({ time: 60, type: 'warning', message: '相邻电池温度传导升高' })
    }
    if (time >= 90 && !events.value.find(e => e.time === 90)) {
      newEvents.push({ time: 90, type: 'critical', message: '电池#4发生热失控，产生大量烟雾', cellId: 3 })
    }
    if (newEvents.length) {
      events.value = [...events.value, ...newEvents]
    }
  }

  function tick(timestamp: number) {
    if (!isPlaying.value) return
    if (lastTimestamp === 0) lastTimestamp = timestamp
    const delta = (timestamp - lastTimestamp) / 1000
    lastTimestamp = timestamp

    currentTime.value = Math.min(totalDuration.value, currentTime.value + delta * speed.value)
    updateState(currentTime.value)

    if (currentTime.value >= totalDuration.value) {
      isPlaying.value = false
      return
    }
    animFrame = requestAnimationFrame(tick)
  }

  function play() {
    if (currentTime.value >= totalDuration.value) currentTime.value = 0
    isPlaying.value = true
    lastTimestamp = 0
    animFrame = requestAnimationFrame(tick)
  }

  function pause() {
    isPlaying.value = false
    if (animFrame) cancelAnimationFrame(animFrame)
  }

  function togglePlay() {
    if (isPlaying.value) pause()
    else play()
  }

  function reset() {
    pause()
    currentTime.value = 0
    events.value = []
    cells.value = cells.value.map(c => ({
      ...c,
      temperature: initialTemp.value,
      status: 'normal' as const,
      smokeIntensity: 0,
      fireIntensity: 0,
    }))
    environment.value = { temperature: 25, humidity: 45, gasLevel: 100 }
  }

  function seek(time: number) {
    currentTime.value = Math.max(0, Math.min(totalDuration.value, time))
    events.value = []
    updateState(currentTime.value)
  }

  function setSpeed(s: number) {
    speed.value = s
  }

  function reinit(duration: number, cellCount: number, temp: number) {
    pause()
    totalDuration.value = duration
    initialTemp.value = temp
    currentTime.value = 0
    events.value = []
    cells.value = Array.from({ length: cellCount }, (_, i) => ({
      id: i,
      temperature: temp,
      status: 'normal' as const,
      smokeIntensity: 0,
      fireIntensity: 0,
    }))
    environment.value = { temperature: 25, humidity: 45, gasLevel: 100 }
  }

  onUnmounted(() => {
    if (animFrame) cancelAnimationFrame(animFrame)
  })

  return {
    currentTime,
    isPlaying,
    speed,
    cells,
    events,
    environment,
    progress,
    totalDuration,
    play,
    pause,
    togglePlay,
    reset,
    seek,
    setSpeed,
    reinit,
  }
}
