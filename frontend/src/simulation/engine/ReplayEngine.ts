import type { DeductionEventLogEntry, DeductionSceneEvent, SimulationSnapshot } from '../types/deduction.types'

export type ReplayListener = (event: DeductionSceneEvent) => void
export type ProgressListener = (elapsedMs: number, durationMs: number) => void

export class ReplayEngine {
  private events: DeductionEventLogEntry[] = []
  private snapshots: SimulationSnapshot[] = []
  private playing = false
  private cursor = 0
  private playheadMs = 0
  private lastFrameTs = 0
  private speed = 1
  private rafId: number | null = null
  private listeners = new Set<ReplayListener>()
  private progressListeners = new Set<ProgressListener>()

  load(events: DeductionEventLogEntry[], snapshots: SimulationSnapshot[] = []): void {
    this.events = [...events].sort((a, b) => a.seq - b.seq)
    this.snapshots = [...snapshots].sort((a, b) => a.elapsedMs - b.elapsedMs)
    this.cursor = 0
    this.playheadMs = 0
  }

  subscribe(fn: ReplayListener): () => void {
    this.listeners.add(fn)
    return () => this.listeners.delete(fn)
  }

  onProgress(fn: ProgressListener): () => void {
    this.progressListeners.add(fn)
    return () => this.progressListeners.delete(fn)
  }

  play(fromMs = 0): void {
    this.pause()
    this.playheadMs = fromMs
    this.cursor = this.events.findIndex(e => e.elapsedMs >= fromMs)
    if (this.cursor < 0) this.cursor = 0
    this.lastFrameTs = 0
    this.playing = true
    this.emitProgress()
    this.tick()
  }

  pause(): void {
    this.playing = false
    if (this.rafId !== null) {
      cancelAnimationFrame(this.rafId)
      this.rafId = null
    }
  }

  setSpeed(speed: number): void {
    this.speed = Math.max(0.25, Math.min(8, speed))
  }

  seek(targetMs: number): void {
    const wasPlaying = this.playing
    this.pause()
    this.playheadMs = targetMs
    this.cursor = 0
    while (this.cursor < this.events.length && this.events[this.cursor].elapsedMs <= targetMs) {
      this.dispatch(this.events[this.cursor])
      this.cursor++
    }
    this.emitProgress()
    if (wasPlaying) this.play()
  }

  getDurationMs(): number {
    if (!this.events.length) return 0
    return this.events[this.events.length - 1].elapsedMs
  }

  getPlayheadMs(): number {
    return this.playheadMs
  }

  isPlaying(): boolean {
    return this.playing
  }

  destroy(): void {
    this.pause()
    this.listeners.clear()
    this.progressListeners.clear()
  }

  private tick = (): void => {
    if (!this.playing) return

    const now = performance.now()
    if (this.lastFrameTs === 0) this.lastFrameTs = now
    const delta = (now - this.lastFrameTs) * this.speed
    this.lastFrameTs = now
    this.playheadMs += delta

    while (this.cursor < this.events.length && this.events[this.cursor].elapsedMs <= this.playheadMs) {
      this.dispatch(this.events[this.cursor])
      this.cursor++
    }

    this.emitProgress()

    if (this.cursor >= this.events.length) {
      this.pause()
      return
    }

    this.rafId = requestAnimationFrame(this.tick)
  }

  private emitProgress(): void {
    const duration = this.getDurationMs()
    for (const fn of this.progressListeners) fn(this.playheadMs, duration)
  }

  private dispatch(entry: DeductionEventLogEntry): void {
    let payload = entry.payload
    if (typeof payload === 'string') {
      try {
        payload = JSON.parse(payload)
      } catch {
        payload = {}
      }
    }
    const sceneEvent: DeductionSceneEvent = {
      seq: entry.seq,
      elapsedMs: entry.elapsedMs,
      type: this.mapEventType(entry.eventType),
      payload: (payload || {}) as Record<string, unknown>,
    }
    for (const fn of this.listeners) fn(sceneEvent)
  }

  private mapEventType(type: string): DeductionSceneEvent['type'] {
    const map: Record<string, DeductionSceneEvent['type']> = {
      'cell:temperature': 'cell:temperature',
      'cell:status': 'cell:status',
      'fx:smoke': 'fx:smoke',
      'fx:fire': 'fx:fire',
      'fx:ventilation': 'fx:ventilation',
      'fx:isolation': 'fx:isolation',
      'env:gasLevel': 'env:gasLevel',
      'ui:alert': 'ui:alert',
      'camera:focus': 'camera:focus',
      'phase:change': 'phase:change',
    }
    return map[type] ?? 'phase:change'
  }
}
