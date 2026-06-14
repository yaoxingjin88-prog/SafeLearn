/** 经典推演场景与真实事故案例的关联元数据 */
export interface ClassicScenarioMeta {
  caseId: string
  caseTitle: string
  location: string
  accidentDate: string
  tier: 'basic' | 'intermediate' | 'advanced'
  tierLabel: string
  subtitle: string
  phaseSteps: string[]
}

export const CLASSIC_SCENARIO_META: Record<string, ClassicScenarioMeta> = {
  '30000000-0000-0000-0000-000000000001': {
    caseId: '40000000-0000-0000-0000-000000000001',
    caseTitle: '深圳储能电站锂电池起火事故',
    location: '广东省深圳市',
    accidentDate: '2025-03-15',
    tier: 'basic',
    tierLabel: '入门 · L1',
    subtitle: '基于真实单模组热失控案例抽象，训练早期识别、断电与冷却决策',
    phaseSteps: ['正常监控', '温度异常', '冒烟', '起火', '灭火处置'],
  },
  '30000000-0000-0000-0000-000000000002': {
    caseId: '40000000-0000-0000-0000-000000000002',
    caseTitle: '南京储能电站热失控扩散事故',
    location: '江苏省南京市',
    accidentDate: '2025-06-20',
    tier: 'intermediate',
    tierLabel: '进阶 · L2',
    subtitle: '模拟模组内热扩散链式反应，训练区域隔离与消防联动决策',
    phaseSteps: ['单体异常', '温度升高', '热扩散', '模组起火', '全站响应'],
  },
  '30000000-0000-0000-0000-000000000003': {
    caseId: '40000000-0000-0000-0000-00000000000a',
    caseTitle: '美国加州 Gateway 250MW 储能电站火灾',
    location: '美国加州 Otay Mesa',
    accidentDate: '2024-05-15',
    tier: 'advanced',
    tierLabel: '高级 · L3',
    subtitle: '大规模储能舱火灾指挥推演，训练全舱消防、撤离与长期监护决策',
    phaseSteps: ['局部异常', '烟雾产生', '舱内起火', '火势蔓延', '指挥处置'],
  },
}

export function getClassicMeta(scenarioId: string): ClassicScenarioMeta | null {
  return CLASSIC_SCENARIO_META[scenarioId] ?? null
}
