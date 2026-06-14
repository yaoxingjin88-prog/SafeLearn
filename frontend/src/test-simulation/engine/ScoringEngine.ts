import type { SimEngineState, SimScoreReport, TestSimScenario } from '../types'

function rating(score: number): string {
  if (score >= 90) return '优秀'
  if (score >= 75) return '良好'
  if (score >= 60) return '合格'
  return '待提升'
}

export function buildSimReport(scenario: TestSimScenario, state: SimEngineState): SimScoreReport {
  const correct = state.decisions.filter(d => d.isCorrect).length
  const total = state.decisions.length || 1
  const avgResponse = state.decisions.reduce((s, d) => s + d.responseMs, 0) / total

  const riskId = Math.round(100 - state.riskIndex * 0.5 + correct * 10)
  const responseSpeed = Math.round(avgResponse < 30000 ? 88 : avgResponse < 50000 ? 72 : 55)
  const decisionAcc = Math.round((correct / total) * 100)
  const emergency = state.outcome === 'controlled' ? 92 : state.outcome === 'partial' ? 68 : 42

  const dimensions = [
    { key: 'risk', label: '风险识别', score: clamp(riskId, 40, 100), comment: riskId >= 80 ? '能识别测试异常征象' : '对温升告警敏感度不足' },
    { key: 'speed', label: '响应速度', score: responseSpeed, comment: responseSpeed >= 75 ? '决策响应及时' : '存在犹豫延误' },
    { key: 'accuracy', label: '决策正确率', score: decisionAcc, comment: `${correct}/${total} 项关键决策正确` },
    { key: 'emergency', label: '应急处置', score: emergency, comment: emergency >= 75 ? '处置符合生命第一原则' : '应急处置存在重大偏差' },
  ]

  const totalScore = Math.round(dimensions.reduce((s, d) => s + d.score, 0) / 4)
  const strengths: string[] = []
  const weaknesses: string[] = []
  if (correct >= 2) strengths.push('关键决策多数符合测试安全规范')
  if (state.outcome === 'controlled') strengths.push('成功在异常阶段遏制事故升级')
  if (state.outcome === 'catastrophic') weaknesses.push('事故后果严重，测试安全意识薄弱')
  state.decisions.filter(d => !d.isCorrect).forEach(d => weaknesses.push(d.consequence))

  let summary = ''
  if (state.outcome === 'controlled') {
    summary = '你在本次推演中展现了良好的测试安全意识和异常响应能力。请继续保持「预警即停机」原则。'
  } else if (state.outcome === 'partial') {
    summary = '部分环节处置尚可，但异常温升阶段的决策仍有改进空间。建议复习 BMS 告警响应流程。'
  } else {
    summary = '本次推演发生闪爆事故，暴露测试前确认与异常处置的严重缺陷。请系统学习推荐课程后重新推演。'
  }

  return {
    totalScore,
    rating: rating(totalScore),
    dimensions,
    strengths: strengths.length ? strengths : ['已完成完整推演流程'],
    weaknesses: weaknesses.length ? weaknesses : [],
    instructorSummary: summary,
    reportTitle: `《${scenario.title.replace('推演', '')}学习报告》`,
    courseLinks: scenario.courseLinks,
  }
}

function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n))
}
