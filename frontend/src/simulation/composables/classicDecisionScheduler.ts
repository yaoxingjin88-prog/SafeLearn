import type { DeductionContext, DecisionPointDef, DeductionScenarioDef } from '../types/deduction.types'

function hotspotTemp(ctx: DeductionContext, hotspotId: number): number {
  return ctx.cells[hotspotId]?.temperature ?? 0
}

function maxTemp(ctx: DeductionContext): number {
  return ctx.cells.length ? Math.max(...ctx.cells.map(c => c.temperature)) : 0
}

function isDecided(ctx: DeductionContext, dpId: string): boolean {
  return ctx.decisions.some(d => d.decisionPointId === dpId)
}

function isTriggered(ctx: DeductionContext, dp: DecisionPointDef, hotspotId: number): boolean {
  const elapsed = ctx.elapsedMs / 1000
  const timeHit = dp.triggerTimeSec != null && elapsed >= dp.triggerTimeSec
  const tempHit = dp.triggerTemp != null
    && Math.max(hotspotTemp(ctx, hotspotId), maxTemp(ctx)) >= dp.triggerTemp

  if (dp.phase === 'isolation') {
    return ctx.branch === 'isolation' || ctx.phase === 'isolation'
  }

  return timeHit || tempHit
}

/** 按 order 依次返回当前应展示的决策点 */
export function resolveNextDecision(
  scenario: DeductionScenarioDef,
  ctx: DeductionContext | null,
): DecisionPointDef | null {
  if (!ctx || ctx.phase === 'completed' || ctx.phase === 'idle') return null

  const hotspotId = scenario.thermalModel.hotspotCellId
    ?? Math.floor(scenario.initialConditions.cellCount / 2)

  const ordered = [...scenario.decisionPoints].sort((a, b) => a.order - b.order)

  for (const dp of ordered) {
    if (isDecided(ctx, dp.id)) continue

    // L1 隔离确认仅在选择「电气隔离」分支后出现
    if (dp.phase === 'isolation' && ctx.branch !== 'isolation') continue

    if (isTriggered(ctx, dp, hotspotId)) return dp
    return null
  }

  return null
}
