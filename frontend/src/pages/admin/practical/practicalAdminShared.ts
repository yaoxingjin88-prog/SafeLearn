export const CASE_TYPE_OPTIONS = [
  { label: '全部类型', value: 'all' },
  { label: '热失控', value: 'thermal_runaway' },
  { label: '火灾爆炸', value: 'fire_explosion' },
  { label: '气体泄漏', value: 'gas_leak' },
  { label: '电气故障', value: 'electrical' },
]

export const SEVERITY_OPTIONS = [
  { label: '全部等级', value: 'all' },
  { label: '特别重大', value: 'critical' },
  { label: '重大', value: 'major' },
  { label: '较大', value: 'moderate' },
  { label: '一般', value: 'minor' },
]

export const SCENARIO_KIND_OPTIONS = [
  { label: '全部类型', value: 'all' },
  { label: '三维推演', value: 'simulation' },
  { label: '应急训练', value: 'training' },
  { label: '时间轴/测试', value: 'emergency' },
]

export function severityLabel(value?: string) {
  return SEVERITY_OPTIONS.find(o => o.value === value)?.label || value || '-'
}

export function scenarioKindLabel(value?: string) {
  return SCENARIO_KIND_OPTIONS.find(o => o.value === value)?.label || value || '-'
}

export function scenarioKindTag(value?: string) {
  if (value === 'simulation') return 'danger'
  if (value === 'emergency') return 'warning'
  if (value === 'training') return 'success'
  return 'info'
}
