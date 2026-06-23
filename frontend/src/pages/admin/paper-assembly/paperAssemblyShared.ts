export const PAPER_TYPE_RULES = [
  { type: 'single', label: '单选题', count: 10, scorePerQuestion: 2, difficultyRatio: { easy: 30, medium: 50, hard: 20 } },
  { type: 'multiple', label: '多选题', count: 5, scorePerQuestion: 3, difficultyRatio: { easy: 20, medium: 50, hard: 30 } },
  { type: 'truefalse', label: '判断题', count: 5, scorePerQuestion: 1, difficultyRatio: { easy: 30, medium: 50, hard: 20 } },
  { type: 'short', label: '简答题', count: 2, scorePerQuestion: 6, difficultyRatio: { easy: 20, medium: 60, hard: 20 } },
  { type: 'case', label: '案例分析题', count: 1, scorePerQuestion: 10, difficultyRatio: { easy: 10, medium: 60, hard: 30 } },
] as const

export const PAPER_CHART_COLORS = ['#3b82f6', '#f59e0b', '#22c55e', '#ec4899', '#8b5cf6']

export function formatDifficultyRatio(ratio: { easy: number; medium: number; hard: number }) {
  return `${ratio.easy}%:${ratio.medium}%:${ratio.hard}%`
}

export function calcRuleSubtotal(rule: { count: number; scorePerQuestion: number }) {
  return rule.count * rule.scorePerQuestion
}

export function calcTotals(rules: Array<{ count: number; scorePerQuestion: number }>) {
  const totalQuestions = rules.reduce((sum, r) => sum + (Number(r.count) || 0), 0)
  const totalScore = rules.reduce((sum, r) => sum + calcRuleSubtotal(r), 0)
  return { totalQuestions, totalScore }
}

export function typeLabel(type: string) {
  return PAPER_TYPE_RULES.find(r => r.type === type)?.label || type
}
