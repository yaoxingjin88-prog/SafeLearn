export type HighlightKind = 'normal' | 'temp' | 'gas' | 'std' | 'risk'
export type RiskLevel = 'low' | 'medium' | 'high' | 'critical'

export interface TextToken {
  text: string
  kind: HighlightKind
}

export interface SceneTelemetry {
  maxTemp: number
  maxGas: number
  risk: RiskLevel
}

export function tokenizeHighlight(text: string): TextToken[] {
  const re = /(\d+\s*℃|\d+\s*ppm|H₂|VOC|GB\/T\s*[\d.\-]+(?:-\d+)?|DL\/T\s*[\d.\-]+(?:-\d+)?|GB\s*50016|《[^》]+》)/g
  const tokens: TextToken[] = []
  let last = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) {
      tokens.push({ text: text.slice(last, m.index), kind: 'normal' })
    }
    const raw = m[0]
    let kind: HighlightKind = 'risk'
    if (raw.includes('℃')) kind = 'temp'
    else if (raw.includes('ppm') || raw === 'H₂' || raw === 'VOC') kind = 'gas'
    else if (raw.startsWith('GB') || raw.startsWith('DL') || raw.startsWith('《')) kind = 'std'
    tokens.push({ text: raw, kind })
    last = m.index + raw.length
  }
  if (last < text.length) tokens.push({ text: text.slice(last), kind: 'normal' })
  return tokens.length ? tokens : [{ text, kind: 'normal' }]
}

export function extractTelemetry(text: string): SceneTelemetry {
  const temps = [...text.matchAll(/(\d+)\s*℃/g)].map(m => parseInt(m[1], 10))
  const gases = [...text.matchAll(/(\d+)\s*ppm/g)].map(m => parseInt(m[1], 10))
  const maxTemp = temps.length ? Math.max(...temps) : 28
  const maxGas = gases.length ? Math.max(...gases) : 0
  let risk: RiskLevel = 'low'
  if (maxTemp >= 120 || maxGas >= 500) risk = 'critical'
  else if (maxTemp >= 60 || maxGas >= 50) risk = 'high'
  else if (maxTemp >= 42 || maxGas >= 15) risk = 'medium'
  return { maxTemp, maxGas, risk }
}

export function getOptionConsequence(
  optionId: string,
  optionText: string,
  isCorrect: boolean,
  optionConsequence?: string,
): string {
  if (isCorrect) return optionConsequence || ''
  if (optionConsequence) return optionConsequence
  const fallback = LEGACY_CONSEQUENCE_FALLBACK[optionId]
  if (fallback) return fallback
  return `选择「${optionText.slice(0, 24)}${optionText.length > 24 ? '…' : ''}」可能导致险情升级或延误处置时机。`
}

const LEGACY_CONSEQUENCE_FALLBACK: Record<string, string> = {
  bj_dp1_b: '延误 10–15 分钟处置，热失控可能进入不可控阶段，与 4·16 事故中预警响应迟缓的教训一致。',
  bj_dp1_c: '响应链断裂，错过远程切断与现场确认的黄金窗口，事故链极易扩大。',
  bj_dp1_d: '调整告警阈值无法消除真实内短路风险，可能将一级预警误判为误报而延误处置。',
  bj_dp2_b: '氧气涌入加剧热失控，人员暴露在有毒气体与闪燃风险中，属储能火灾常见致命错误。',
  bj_dp2_c: '现场人员滞留核心区，温升与产气阶段人员安全风险急剧上升。',
  bj_dp2_d: '向舱内强制送风可能引入氧气，加剧热失控反应，外围监测无法替代隔离与消防联锁。',
  bj_dp3_b: '高压直流环境近距离水枪作业存在触电风险，且无法阻断舱内能量输入。',
  bj_dp3_c: '消极等待导致火势蔓延、可燃气体积聚，爆燃概率大幅上升。',
  bj_dp3_d: '未全面断电隔离时相邻舱仍可能有能量输入，局部通风难以阻断热扩散。',
  bj_dp4_b: '非专业人员进入核心区极易引发二次伤亡，4·16 事故已证明爆炸后核心区极度危险。',
  bj_dp4_c: '未评估结构安全即恢复并网，可能导致故障蔓延与次生事故。',
  bj_dp4_d: '爆炸后结构稳定性未评估前进入核心区风险极高，应由专业消防主导核心区处置。',
  gz_dp1_b: '遗漏人员资质与消防确认，异常发生时缺乏中止条件，与 6·14 管理缺陷一致。',
  gz_dp1_c: '口头确认无法追溯，测试环节风险管控形同虚设。',
  gz_dp1_d: '简化检查无法发现绝缘劣化、气体监测失效等隐患，与 6·14 测试管理失控教训一致。',
  gz_dp2_b: '从异常到闪爆仅约 6 分钟，继续测试将直接导致能量输入延续。',
  gz_dp2_c: '请示等待延误急停与隔离，错失遏制热失控链的第一道关卡。',
  gz_dp2_d: '降倍率仍持续能量输入，无法切断热失控链，温升可能继续加速。',
  gz_dp3_b: '开门查看引入氧气，闪爆临界区人员暴露于致命风险。',
  gz_dp3_c: '水与锂电池热失控反应不匹配，可能加剧反应并触发二次爆炸。',
  gz_dp3_d: '未切断主回路时监测数据无助于降低闪爆风险，人员仍暴露在危险区域。',
  gz_dp4_b: '延误救援黄金时间，先取证后救人不符「生命第一」原则。',
  gz_dp4_c: '闪爆后尝试重启 BMS 无意义且可能危及施救人员安全。',
  gz_dp4_d: '自行处置延误专业救援，闪爆现场可能存在二次爆炸与有毒气体风险。',
}

/** 后端已同步完整题目，前端直接透传 */
export function enrichDecisionPoints(points: unknown[], _scenarioId?: string): unknown[] {
  return points
}
