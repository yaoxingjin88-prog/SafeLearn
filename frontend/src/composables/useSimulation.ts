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

export interface ScenarioEventInput {
  id?: string
  triggerTime: number
  type: string
  description: string
  location?: { x: number; y: number; z: number }
  parameters?: {
    temperature?: number
    spreadRate?: number
    gasConcentration?: number
  }
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

export function normalizeScenarioEvents(input: unknown): ScenarioEventInput[] {
  if (!input) return []
  if (Array.isArray(input)) return input as ScenarioEventInput[]
  if (typeof input === 'string') {
    try {
      const parsed = JSON.parse(input)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

export function useSimulation(initDuration = 120, initCellCount = 16, initTemp = 30) {
  const totalDuration = ref(initDuration)
  const initialTemp = ref(initTemp)
  const currentTime = ref(0)
  const isPlaying = ref(false)
  const speed = ref(1)
  let animFrame: number | null = null
  let lastTimestamp = 0

  let scenarioEvents: ScenarioEventInput[] = []
  let hotspotCellId = 0

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

  function mapEventType(type: string): SimulationEvent['type'] {
    switch (type) {
      case 'temperature_rise':
        return 'warning'
      case 'smoke':
      case 'gas_leak':
        return 'danger'
      case 'fire':
      case 'explosion':
        return 'critical'
      default:
        return 'warning'
    }
  }

  function resolveHotspot(cellCount: number, events: ScenarioEventInput[]): number {
    const first = events.find(e => e.type === 'temperature_rise' || e.type === 'fire')
    if (first?.location) {
      const idx = Math.round(first.location.x * cellCount) % cellCount
      return Math.max(0, Math.min(cellCount - 1, idx))
    }
    return Math.max(0, Math.floor(cellCount / 2))
  }

  function heatIntensityAt(time: number): number {
    const progress = totalDuration.value === 0 ? 0 : time / totalDuration.value
    const triggered = scenarioEvents.filter(e => e.triggerTime <= time)
    if (triggered.length === 0) return progress * 0.2

    let intensity = progress
    for (const evt of triggered) {
      const elapsed = Math.max(0, time - evt.triggerTime)
      const spread = evt.parameters?.spreadRate ?? 1
      const factor = Math.min(1, (elapsed / Math.max(totalDuration.value * 0.3, 1)) * spread)
      switch (evt.type) {
        case 'temperature_rise':
          intensity = Math.max(intensity, 0.3 + factor * 0.4)
          break
        case 'smoke':
          intensity = Math.max(intensity, 0.45 + factor * 0.35)
          break
        case 'fire':
        case 'explosion':
          intensity = Math.max(intensity, 0.55 + factor * 0.45)
          break
        case 'gas_leak':
          intensity = Math.max(intensity, 0.35 + factor * 0.25)
          break
      }
    }
    return Math.min(1, intensity)
  }

  function updateState(time: number) {
    const heat = heatIntensityAt(time)
    const triggered = scenarioEvents.filter(e => e.triggerTime <= time)

    cells.value = cells.value.map((cell, i) => {
      const hotspot = i === hotspotCellId
      const neighbor = Math.abs(i - hotspotCellId) <= 1
      const base = initialTemp.value
      let temp = base

      if (hotspot) {
        temp = base + heat * heat * 400
      } else if (neighbor) {
        temp = base + heat * heat * 150 * (1 - Math.abs(i - hotspotCellId) * 0.3)
      } else {
        temp = base + heat * 30
      }

      for (const evt of triggered) {
        const targetTemp = evt.parameters?.temperature
        if (targetTemp && (hotspot || neighbor)) {
          temp = Math.max(temp, targetTemp * (hotspot ? 1 : 0.55))
        }
      }

      const smokeEvents = triggered.filter(e => e.type === 'smoke' || e.type === 'gas_leak').length
      const fireEvents = triggered.filter(e => e.type === 'fire' || e.type === 'explosion').length
      const smokeIntensity = hotspot
        ? Math.min(1, heat * 1.2 + smokeEvents * 0.15)
        : neighbor
          ? Math.min(0.6, heat * 0.6 + smokeEvents * 0.08)
          : 0
      const fireIntensity = hotspot && heat > 0.4
        ? Math.min(1, (heat - 0.35) * 2 + fireEvents * 0.2)
        : 0

      return {
        ...cell,
        temperature: Math.round(temp),
        status: getStatus(temp),
        smokeIntensity,
        fireIntensity,
      }
    })

    const envHeat = triggered.filter(e =>
      e.type === 'temperature_rise' || e.type === 'fire' || e.type === 'smoke'
    ).length
    const gasLeak = triggered
      .filter(e => e.type === 'gas_leak')
      .reduce((sum, e) => sum + (e.parameters?.gasConcentration ?? 800), 0)

    environment.value = {
      temperature: 25 + heat * 15 + envHeat * 3,
      humidity: Math.max(20, 45 - heat * 30 - triggered.filter(e => e.type === 'fire').length * 5),
      gasLevel: 100 + heat * heat * 5000 + gasLeak,
    }

    syncScenarioEvents(time)
  }

  function syncScenarioEvents(time: number) {
    if (scenarioEvents.length === 0) {
      syncFallbackEvents(time)
      return
    }

    const visible = scenarioEvents
      .filter(e => e.triggerTime <= time)
      .map(e => ({
        time: e.triggerTime,
        type: mapEventType(e.type),
        message: e.description,
        cellId: hotspotCellId,
      }))

    events.value = visible
  }

  function syncFallbackEvents(time: number) {
    const newEvents: SimulationEvent[] = []
    const checkpoints = [
      { time: 20, type: 'warning' as const, message: '电池温度异常升高', cellId: hotspotCellId },
      { time: 45, type: 'danger' as const, message: '达到热失控阈值', cellId: hotspotCellId },
      { time: 90, type: 'critical' as const, message: '发生热失控，产生大量烟雾', cellId: hotspotCellId },
    ]
    for (const cp of checkpoints) {
      if (time >= cp.time) newEvents.push(cp)
    }
    events.value = newEvents
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
    updateState(0)
  }

  function seek(time: number) {
    currentTime.value = Math.max(0, Math.min(totalDuration.value, time))
    updateState(currentTime.value)
  }

  function setSpeed(s: number) {
    speed.value = s
  }

  function reinit(duration: number, cellCount: number, temp: number, scenarioEventList: unknown = []) {
    pause()
    totalDuration.value = duration
    initialTemp.value = temp
    scenarioEvents = normalizeScenarioEvents(scenarioEventList)
      .sort((a, b) => a.triggerTime - b.triggerTime)
    hotspotCellId = resolveHotspot(cellCount, scenarioEvents)
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
    updateState(0)
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
