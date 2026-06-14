import { CLASSIC_SCENARIO_META } from '@/simulation/classicScenarioMeta'

const DEDICATED_PATHS: Record<string, string> = {
  '40000000-0000-0000-0000-000000000003': '/simulation/timeline/beijing_416',
  '40000000-0000-0000-0000-000000000005': '/simulation/test/guangzhou_614',
}

for (const [scenarioId, meta] of Object.entries(CLASSIC_SCENARIO_META)) {
  DEDICATED_PATHS[meta.caseId] = `/simulation/${scenarioId}`
}

/** 案例 ID → 推演路由（相对 /user 前缀的路径） */
export function getCaseSimulationPath(caseId: string): string | null {
  return DEDICATED_PATHS[caseId] ?? null
}

export function hasCaseSimulation(caseId: string): boolean {
  return Boolean(getCaseSimulationPath(caseId))
}

export function parseCaseMetaTags(description: string) {
  const battery = description.match(/【电池类型】([^【】]+)/)?.[1]?.trim() || ''
  const station = description.match(/【电站类型】([^【】]+)/)?.[1]?.trim() || ''
  return { battery, station }
}

export function stripMetaFromDescription(description: string) {
  return description
    .replace(/【电池类型】[^【】]+/g, '')
    .replace(/【电站类型】[^【】]+/g, '')
    .trim()
}
