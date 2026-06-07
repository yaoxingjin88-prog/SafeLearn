import { createActor, type Subscription } from 'xstate'
import { thermalRunawayMachine } from '../machine/thermalRunawayMachine'
import { TimeEngine } from './TimeEngine'
import { ReplayEngine } from './ReplayEngine'
import type {
  DeductionContext,
  DeductionEvent,
  DeductionEventLogEntry,
  DeductionSceneEvent,
  DeductionSceneEventType,
} from '../types/deduction.types'

export type SceneEventHandler = (event: DeductionSceneEvent) => void

export class SimulationEngine {
  private actor = createActor(thermalRunawayMachine)
  private timeEngine = new TimeEngine()
  private replayEngine = new ReplayEngine()
  private eventLog: DeductionEventLogEntry[] = []
  private sceneHandlers = new Set<SceneEventHandler>()
  private sub: Subscription | null = null
  private mode: 'live' | 'replay' = 'live'
  private prevContext: DeductionContext | null = null

  constructor() {
    this.actor.start()
    this.sub = this.actor.subscribe(snapshot => {
      this.onStateChange(snapshot.context)
    })
    this.timeEngine.subscribe((deltaMs) => {
      if (this.mode === 'live' && this.timeEngine.isPlaying()) {
        this.send({ type: 'TICK', deltaMs })
      }
    })
    this.replayEngine.subscribe(event => {
      for (const h of this.sceneHandlers) h(event)
    })
  }

  onSceneEvent(handler: SceneEventHandler): () => void {
    this.sceneHandlers.add(handler)
    return () => this.sceneHandlers.delete(handler)
  }

  start(sessionId: string, scenarioId: string): DeductionContext {
    this.mode = 'live'
    this.eventLog = []
    this.prevContext = null
    this.timeEngine.reset()
    this.send({ type: 'START', sessionId, scenarioId })
    this.timeEngine.start(0)
    return this.getContext()
  }

  pause(): void {
    this.timeEngine.pause()
    this.send({ type: 'PAUSE' })
  }

  resume(): void {
    this.send({ type: 'RESUME' })
    this.timeEngine.resume()
  }

  togglePlay(): void {
    if (this.timeEngine.isPlaying()) this.pause()
    else this.resume()
  }

  setSpeed(speed: number): void {
    this.timeEngine.setSpeed(speed)
  }

  submitDecision(decisionPointId: string, optionId: string): DeductionContext {
    this.send({ type: 'DECISION', decisionPointId, optionId })
    return this.getContext()
  }

  submitTimeout(): void {
    this.send({ type: 'TIMEOUT' })
  }

  finish(): DeductionContext {
    this.timeEngine.pause()
    this.send({ type: 'FINISH' })
    return this.getContext()
  }

  startReplay(events: DeductionEventLogEntry[]): void {
    this.mode = 'replay'
    this.timeEngine.pause()
    this.replayEngine.load(events)
    this.send({ type: 'REPLAY' })
    this.replayEngine.play(0)
  }

  getContext(): DeductionContext {
    return this.actor.getSnapshot().context
  }

  getEventLog(): DeductionEventLogEntry[] {
    return [...this.eventLog]
  }

  getElapsedSec(): number {
    return Math.floor(this.getContext().elapsedMs / 1000)
  }

  destroy(): void {
    this.sub?.unsubscribe()
    this.timeEngine.destroy()
    this.replayEngine.destroy()
    this.sceneHandlers.clear()
    this.actor.stop()
  }

  private send(event: DeductionEvent): void {
    this.actor.send(event)
  }

  private onStateChange(ctx: DeductionContext): void {
    const prev = this.prevContext
    this.prevContext = { ...ctx, cells: ctx.cells.map(c => ({ ...c })), environment: { ...ctx.environment } }

    if (!prev || prev.phase !== ctx.phase) {
      this.emitSceneEvent('phase:change', ctx, { phase: ctx.phase })
    }

    if (prev) {
      ctx.cells.forEach((cell, i) => {
        const old = prev.cells[i]
        if (!old) return
        if (old.temperature !== cell.temperature) {
          this.emitSceneEvent('cell:temperature', ctx, { cellId: cell.id, temperature: cell.temperature })
        }
        if (old.status !== cell.status) {
          this.emitSceneEvent('cell:status', ctx, { cellId: cell.id, status: cell.status })
        }
        if (old.smokeIntensity !== cell.smokeIntensity && cell.smokeIntensity > 0) {
          this.emitSceneEvent('fx:smoke', ctx, { cellId: cell.id, intensity: cell.smokeIntensity })
        }
        if (old.fireIntensity !== cell.fireIntensity && cell.fireIntensity > 0) {
          this.emitSceneEvent('fx:fire', ctx, { cellId: cell.id, intensity: cell.fireIntensity })
        }
      })

      if (prev.environment.gasLevel !== ctx.environment.gasLevel) {
        this.emitSceneEvent('env:gasLevel', ctx, { gasLevel: ctx.environment.gasLevel })
      }
      if (!prev.environment.ventilationOn && ctx.environment.ventilationOn) {
        this.emitSceneEvent('fx:ventilation', ctx, { active: true })
      }
      if (!prev.environment.isolationComplete && ctx.environment.isolationComplete) {
        this.emitSceneEvent('fx:isolation', ctx, { complete: true })
      }
    }

    if (ctx.pendingDecision && (!prev?.pendingDecision || prev.pendingDecision.id !== ctx.pendingDecision.id)) {
      this.emitSceneEvent('ui:alert', ctx, {
        level: 'warning',
        message: ctx.pendingDecision.question,
        decisionPointId: ctx.pendingDecision.id,
      })
    }

    if (ctx.phase === 'completed' && prev?.phase !== 'completed') {
      this.emitSceneEvent('ui:alert', ctx, {
        level: ctx.outcome === 'success' ? 'info' : 'danger',
        message: ctx.outcome === 'success' ? '推演成功：事态已受控' : '推演失败：事故扩大',
      })
    }
  }

  private emitSceneEvent(
    type: DeductionSceneEventType,
    ctx: DeductionContext,
    payload: Record<string, unknown>,
  ): void {
    const entry: DeductionEventLogEntry = {
      seq: ctx.eventSeq,
      sessionId: ctx.sessionId,
      elapsedMs: ctx.elapsedMs,
      eventType: type,
      machineState: ctx.phase,
      payload,
    }
    this.eventLog.push(entry)

    const sceneEvent: DeductionSceneEvent = {
      seq: entry.seq,
      elapsedMs: entry.elapsedMs,
      type,
      payload,
    }
    for (const h of this.sceneHandlers) h(sceneEvent)
  }
}
