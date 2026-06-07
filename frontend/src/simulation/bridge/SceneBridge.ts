import type { Ref } from 'vue'
import type { DeductionSceneEvent } from '../types/deduction.types'
import type { BatteryCellState } from '@/composables/useSimulation'

export interface SceneBridgeTarget {
  cells: Ref<BatteryCellState[]>
  onAlert?: (level: string, message: string) => void
  onPhaseChange?: (phase: string) => void
}

/**
 * 将 DeductionSceneEvent 翻译为 Three.js / Vue 可消费的状态更新。
 * BatteryScene / ParticleSystem 只读 cells，不感知 XState。
 */
export class SceneBridge {
  private target: SceneBridgeTarget

  constructor(target: SceneBridgeTarget) {
    this.target = target
  }

  handle(event: DeductionSceneEvent): void {
    switch (event.type) {
      case 'cell:temperature': {
        const cellId = event.payload.cellId as number
        const temperature = event.payload.temperature as number
        const cell = this.target.cells.value.find(c => c.id === cellId)
        if (cell) cell.temperature = Math.round(temperature)
        break
      }
      case 'cell:status': {
        const cellId = event.payload.cellId as number
        const status = event.payload.status as BatteryCellState['status']
        const cell = this.target.cells.value.find(c => c.id === cellId)
        if (cell) cell.status = status
        break
      }
      case 'fx:smoke': {
        const cellId = event.payload.cellId as number
        const intensity = event.payload.intensity as number
        const cell = this.target.cells.value.find(c => c.id === cellId)
        if (cell) cell.smokeIntensity = intensity
        break
      }
      case 'fx:fire': {
        const cellId = event.payload.cellId as number
        const intensity = event.payload.intensity as number
        const cell = this.target.cells.value.find(c => c.id === cellId)
        if (cell) cell.fireIntensity = intensity
        break
      }
      case 'ui:alert':
        this.target.onAlert?.(
          event.payload.level as string,
          event.payload.message as string,
        )
        break
      case 'phase:change':
        this.target.onPhaseChange?.(event.payload.phase as string)
        break
      default:
        break
    }
  }

  bind(engine: { onSceneEvent: (fn: (e: DeductionSceneEvent) => void) => () => void }): () => void {
    return engine.onSceneEvent(event => this.handle(event))
  }
}
