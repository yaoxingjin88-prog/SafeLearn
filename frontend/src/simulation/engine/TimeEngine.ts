export type TimeEngineListener = (deltaMs: number, elapsedMs: number) => void

export class TimeEngine {
  private elapsedMs = 0
  private speed = 1
  private playing = false
  private rafId: number | null = null
  private lastTs = 0
  private listeners = new Set<TimeEngineListener>()

  subscribe(fn: TimeEngineListener): () => void {
    this.listeners.add(fn)
    return () => this.listeners.delete(fn)
  }

  start(fromMs = 0): void {
    this.elapsedMs = fromMs
    this.playing = true
    this.lastTs = performance.now()
    this.tick()
  }

  pause(): void {
    this.playing = false
    if (this.rafId !== null) {
      cancelAnimationFrame(this.rafId)
      this.rafId = null
    }
  }

  resume(): void {
    if (this.playing) return
    this.playing = true
    this.lastTs = performance.now()
    this.tick()
  }

  setSpeed(speed: number): void {
    this.speed = Math.max(0.25, Math.min(8, speed))
  }

  seek(ms: number): void {
    this.elapsedMs = Math.max(0, ms)
    this.emit(0)
  }

  getElapsedMs(): number {
    return this.elapsedMs
  }

  isPlaying(): boolean {
    return this.playing
  }

  reset(): void {
    this.pause()
    this.elapsedMs = 0
    this.speed = 1
  }

  destroy(): void {
    this.pause()
    this.listeners.clear()
  }

  private tick = (): void => {
    if (!this.playing) return
    const now = performance.now()
    const delta = (now - this.lastTs) * this.speed
    this.lastTs = now
    this.elapsedMs += delta
    this.emit(delta)
    this.rafId = requestAnimationFrame(this.tick)
  }

  private emit(deltaMs: number): void {
    for (const fn of this.listeners) fn(deltaMs, this.elapsedMs)
  }
}
