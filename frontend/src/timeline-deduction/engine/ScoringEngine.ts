import type { DebriefReport, TimelineEngineState, TimelineScenario } from '../types'

function ratingFromScore(score: number): string {
  if (score >= 90) return '优秀'
  if (score >= 75) return '良好'
  if (score >= 60) return '合格'
  return '待提升'
}

export function buildDebriefReport(
  scenario: TimelineScenario,
  state: TimelineEngineState,
): DebriefReport {
  const decisions = state.decisions
  const correct = decisions.filter(d => d.isCorrect).length
  const total = decisions.length || 1

  const riskIdentification = Math.round(clamp(100 - state.riskIndex + correct * 8, 40, 100))
  const decisionMaking = Math.round((correct / total) * 85 + (decisions.every(d => d.responseMs < 45000) ? 15 : 5))
  const emergencyResponse = state.branch === 'optimal' ? 92 : state.branch === 'delayed' ? 72 : state.branch === 'escalated' ? 55 : 38
  const accidentAnalysis = Math.round(60 + correct * 12)

  const dimensions = [
    { key: 'risk', label: '风险识别', score: riskIdentification, max: 100, comment: riskIdentification >= 80 ? '能较早识别异常征象' : '对预警信号敏感度不足' },
    { key: 'decision', label: '决策判断', score: decisionMaking, max: 100, comment: decisionMaking >= 75 ? '关键节点处置较为得当' : '存在明显决策失误' },
    { key: 'response', label: '应急响应', score: emergencyResponse, max: 100, comment: emergencyResponse >= 75 ? '响应链条基本完整' : '应急动作滞后或不当' },
    { key: 'analysis', label: '事故分析', score: accidentAnalysis, max: 100, comment: '结合案例可深化根因认知' },
  ]

  const totalScore = Math.round(dimensions.reduce((s, d) => s + d.score, 0) / dimensions.length)

  const strengths: string[] = []
  const weaknesses: string[] = []
  if (correct >= 2) strengths.push('多数关键决策符合规程要求')
  else weaknesses.push('关键决策与标准处置流程偏差较大')
  if (state.branch === 'optimal') strengths.push('成功将事故遏制在初期阶段')
  if (state.branch === 'catastrophic') weaknesses.push('连续失误导致事故升级至爆炸')
  decisions.filter(d => !d.isCorrect).forEach(d => weaknesses.push(d.consequence))

  let instructorComment = ''
  if (state.outcome === 'contained') {
    instructorComment = '你在本次推演中展现了良好的预警意识和分级处置能力。继续保持「预警即行动」的原则，并加强跨岗位协同演练。'
  } else if (state.outcome === 'partial_loss') {
    instructorComment = '整体响应尚可，但个别环节存在犹豫或手段不当。建议重点复习热失控早期识别与气体消防联锁流程。'
  } else {
    instructorComment = '本次推演事故后果严重，暴露出风险识别与应急决策的薄弱环节。请结合推荐课程系统补强，并在下次推演中优先落实「断电-通风-撤离」三步法。'
  }

  const recommendations = scenario.courseLinks.slice(0, weaknesses.length > 1 ? 4 : 2)

  return {
    totalScore,
    rating: ratingFromScore(totalScore),
    dimensions,
    instructorComment,
    strengths: strengths.length ? strengths : ['已完成完整推演流程'],
    weaknesses: weaknesses.length ? weaknesses : ['暂无明显失误记录'],
    recommendations,
    fishbone: scenario.fishbone,
    fiveWhy: scenario.fiveWhy,
    branch: state.branch,
    outcome: state.outcome,
  }
}

function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n))
}
