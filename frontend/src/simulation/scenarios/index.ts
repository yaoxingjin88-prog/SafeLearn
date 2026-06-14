import type { DeductionScenarioDef } from '../types/deduction.types'
import { singleBatteryThermalRunaway } from './singleBatteryThermalRunaway'
import { batteryGroupThermalDiffusion } from './batteryGroupThermalDiffusion'
import { energyStorageCabinFire } from './energyStorageCabinFire'

export const CLASSIC_DEDUCTION_SCENARIO_IDS = [
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003',
] as const

const SCENARIO_MAP: Record<string, DeductionScenarioDef> = {
  [singleBatteryThermalRunaway.id]: singleBatteryThermalRunaway,
  [batteryGroupThermalDiffusion.id]: batteryGroupThermalDiffusion,
  [energyStorageCabinFire.id]: energyStorageCabinFire,
}

export function getDeductionScenario(scenarioId: string): DeductionScenarioDef | null {
  return SCENARIO_MAP[scenarioId] ?? null
}

export function isClassicDeductionScenario(scenarioId: string): boolean {
  return CLASSIC_DEDUCTION_SCENARIO_IDS.includes(
    scenarioId as (typeof CLASSIC_DEDUCTION_SCENARIO_IDS)[number],
  )
}

export {
  singleBatteryThermalRunaway,
  batteryGroupThermalDiffusion,
  energyStorageCabinFire,
}
