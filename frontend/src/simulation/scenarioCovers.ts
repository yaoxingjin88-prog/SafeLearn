/** 推演场景封面（public 目录静态资源） */
export const SCENARIO_COVERS = {
  beijing416: '/北京.png',
  guangzhou614: '/广州.png',
  classicL1: '/单电池.png',
  classicL2: '/电池组.png',
  classicL3: '/储能.png',
} as const

const CLASSIC_COVER_BY_ID: Record<string, string> = {
  '30000000-0000-0000-0000-000000000001': SCENARIO_COVERS.classicL1,
  '30000000-0000-0000-0000-000000000002': SCENARIO_COVERS.classicL2,
  '30000000-0000-0000-0000-000000000003': SCENARIO_COVERS.classicL3,
}

export function getClassicCover(scenarioId: string): string | undefined {
  return CLASSIC_COVER_BY_ID[scenarioId]
}
