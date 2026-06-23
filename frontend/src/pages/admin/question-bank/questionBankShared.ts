export const QUESTION_TYPE_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '单选题', value: 'single' },
  { label: '多选题', value: 'multiple' },
  { label: '判断题', value: 'truefalse' },
  { label: '简答题', value: 'short' },
  { label: '案例分析题', value: 'case' },
] as const

export const QUESTION_DIFFICULTY_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '易', value: 'easy' },
  { label: '中', value: 'medium' },
  { label: '难', value: 'hard' },
] as const

export const QUESTION_STATUS_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '已发布', value: 'published' },
  { label: '草稿', value: 'draft' },
  { label: '已停用', value: 'disabled' },
] as const

export const QUESTION_TYPE_TABS = [
  { label: '全部', value: 'all', key: 'all' },
  { label: '单选题', value: 'single', key: 'single' },
  { label: '多选题', value: 'multiple', key: 'multiple' },
  { label: '判断题', value: 'truefalse', key: 'truefalse' },
  { label: '简答题', value: 'short', key: 'short' },
  { label: '案例分析题', value: 'case', key: 'case' },
] as const

export function questionTypeLabel(type: string) {
  return QUESTION_TYPE_OPTIONS.find(i => i.value === type)?.label?.replace('全部', type) || type
}

export function questionTypeText(type: string) {
  const map: Record<string, string> = {
    single: '单选题',
    multiple: '多选题',
    truefalse: '判断题',
    short: '简答题',
    case: '案例分析题',
  }
  return map[type] || type
}

export function difficultyLabel(difficulty: string) {
  const map: Record<string, string> = { easy: '易', medium: '中', hard: '难' }
  return map[difficulty] || difficulty
}

export function difficultyClass(difficulty: string) {
  return `difficulty-${difficulty}`
}

export function statusLabel(status: string) {
  const map: Record<string, string> = {
    published: '已发布',
    draft: '草稿',
    disabled: '已停用',
  }
  return map[status] || status
}

export const TAG_COLORS = ['blue', 'green', 'orange', 'purple', 'cyan'] as const

export function tagColorClass(index: number) {
  return `tag-${TAG_COLORS[index % TAG_COLORS.length]}`
}
