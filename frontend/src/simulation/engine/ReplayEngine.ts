import type { DeductionEventLogEntry, DeductionSceneEvent, SimulationSnapshot } from '../types/deduction.types'

export type ReplayListener = (event: DeductionSceneEvent) => void
export type ProgressListener = (elapsedMs: number, durationMs: number) => void
export type EndListener = () => void

export class ReplayEngine {
  private events: DeductionEventLogEntry[] = []
  private snapshots: SimulationSnapshot[] = []
  private durationCapMs = 0
  private playing = false
  private cursor = 0
  private playheadMs = 0
  private lastFrameTs = 0
  private speed = 1
  private rafId: number | null = null
  private listeners = new Set<ReplayListener>()
  private progressListeners = new Set<ProgressListener>()
  private endListeners = new Set<EndListener>()

  load(events: DeductionEventLogEntry[], snapshots: SimulationSnapshot[] = [], durationCapMs?: number): void {
    this.events = [...events].sort((a, b) => a.seq - b.seq)
    this.snapshots = [...snapshots].sort((a, b) => a.elapsedMs - b.elapsedMs)
    const lastEventMs = this.events.length ? this.events[this.events.length - 1].elapsedMs : 0
    this.durationCapMs = durationCapMs != null && durationCapMs > 0 ? durationCapMs : lastEventMs
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

  onEnded(fn: EndListener): () => void {
    this.endListeners.add(fn)
    return () => this.endListeners.delete(fn)
  }

  play(fromMs = 0): void {
    this.pause()
    const duration = this.getDurationMs()
    if (duration <= 0 || !this.events.length) {
      this.playheadMs = 0
      this.emitProgress()
      return
    }
    if (fromMs >= duration) {
      this.seek(duration)
      this.emitEnded()
      return
    }
    this.playheadMs = fromMs
    this.cursor = this.findCursorForMs(fromMs)
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
    const duration = this.getDurationMs()
    const clamped = Math.max(0, Math.min(targetMs, duration))
    this.playheadMs = clamped
    this.cursor = 0
    while (this.cursor < this.events.length && this.events[this.cursor].elapsedMs <= clamped) {
      this.dispatch(this.events[this.cursor])
      this.cursor++
    }
    this.emitProgress()
    if (wasPlaying) {
      if (clamped >= duration) {
        this.emitEnded()
      } else {
        this.play(clamped)
      }
    }
  }

  getDurationMs(): number {
    return this.durationCapMs
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
    this.endListeners.clear()
  }

  private tick = (): void => {
    if (!this.playing) return

    const duration = this.getDurationMs()
    const now = performance.now()
    if (this.lastFrameTs === 0) this.lastFrameTs = now
    const delta = (now - this.lastFrameTs) * this.speed
    this.lastFrameTs = now
    this.playheadMs = Math.min(this.playheadMs + delta, duration)

    this.dispatchUntilPlayhead()

    this.emitProgress()

    if (this.playheadMs >= duration || this.cursor >= this.events.length) {
      this.playheadMs = duration
      this.emitProgress()
      this.pause()
      this.emitEnded()
      return
    }

    this.rafId = requestAnimationFrame(this.tick)
  }

  private findCursorForMs(ms: number): number {
    let lo = 0
    let hi = this.events.length
    while (lo < hi) {
      const mid = (lo + hi) >> 1
      if (this.events[mid].elapsedMs < ms) lo = mid + 1
      else hi = mid
    }
    return lo
  }

  private dispatchUntilPlayhead(): void {
    while (this.cursor < this.events.length && this.events[this.cursor].elapsedMs <= this.playheadMs) {
      this.dispatch(this.events[this.cursor])
      this.cursor++
    }
  }

  private emitProgress(): void {
    const duration = this.getDurationMs()
    const ms = Math.min(this.playheadMs, duration)
    for (const fn of this.progressListeners) fn(ms, duration)
  }

  private emitEnded(): void {
    for (const fn of this.endListeners) fn()
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
