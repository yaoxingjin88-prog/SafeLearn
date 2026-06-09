export interface TimelineEvent {
  id: string
  time: string
  title: string
  description: string
  type: string
}

export interface ReviewOption {
  id: string
  text: string
  correct: boolean
}

export type GuideStep =
  | { key: string; kind: 'intro' }
  | { key: string; kind: 'timeline'; event: TimelineEvent; index: number; question: string; options: ReviewOption[] }
  | { key: string; kind: 'cause'; question: string; options: ReviewOption[] }
  | { key: string; kind: 'lessons'; question: string; options: ReviewOption[] }
  | { key: string; kind: 'summary' }

export const SEED_META_KEY = '__seed'

const REFLECTION_BY_TYPE: Record<string, { question: string; options: ReviewOption[] }> = {
  warning: {
    question: '发现上述征兆后，作为现场值班人员，第一步应怎么做？',
    options: [
      { id: 'a', text: '加强监测、记录异常并按规定上报', correct: true },
      { id: 'b', text: '继续观察，等待参数自行恢复', correct: false },
      { id: 'c', text: '立即用水降温', correct: false },
      { id: 'd', text: '重启系统而不做记录', correct: false },
    ],
  },
  danger: {
    question: '事故进入扩大阶段，最优先的处置措施是什么？',
    options: [
      { id: 'a', text: '切断电源、启动消防并疏散人员', correct: true },
      { id: 'b', text: '派人近距离开箱查看', correct: false },
      { id: 'c', text: '仅加强通风排烟', correct: false },
      { id: 'd', text: '等待上级指示再行动', correct: false },
    ],
  },
  action: {
    question: '以下哪项最符合储能事故应急处置规范？',
    options: [
      { id: 'a', text: '按应急预案执行并全程记录', correct: true },
      { id: 'b', text: '擅自扩大处置范围造成二次风险', correct: false },
      { id: 'c', text: '未通知相关部门自行处置', correct: false },
      { id: 'd', text: '处置结束后不做复盘记录', correct: false },
    ],
  },
  info: {
    question: '从本阶段可以提炼的关键经验是什么？',
    options: [
      { id: 'a', text: '及时控制事态并总结经验教训', correct: true },
      { id: 'b', text: '事故结束后无需再分析', correct: false },
      { id: 'c', text: '仅关注设备损失忽略管理改进', correct: false },
      { id: 'd', text: '减少培训以节约成本', correct: false },
    ],
  },
}

const WRONG_HINTS = [
  '该选项存在安全风险，请结合储能事故处置规范再选。',
  '处置优先级不正确，人员安全与断电应放在首位。',
  '此做法可能扩大事故后果，建议重新思考。',
  '不符合应急预案要求，请再试一次。',
]

function hashSeed(seed: string): number {
  let h = 2166136261
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

/** 确定性打乱选项顺序，避免正确答案总在第一项 */
export function shuffleOptions(options: ReviewOption[], seed: string): ReviewOption[] {
  const arr = options.map(o => ({ ...o }))
  let state = hashSeed(seed) || 1
  const rnd = () => {
    state = (Math.imul(state, 1664525) + 1013904223) >>> 0
    return state / 0x100000000
  }
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(rnd() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr.map((o, i) => ({ ...o, id: String.fromCharCode(97 + i) }))
}

export function createSessionSeed(caseId: string): string {
  return `${caseId}-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

export function pickWrongHint(optionText: string): string {
  const idx = hashSeed(optionText) % WRONG_HINTS.length
  return WRONG_HINTS[idx]
}

function cloneOptions(options: ReviewOption[], seed: string): ReviewOption[] {
  return shuffleOptions(options.map(o => ({ ...o })), seed)
}

export function buildGuideSteps(
  caseData: {
    timeline?: TimelineEvent[]
    directCause?: string
    lessons?: string[]
  },
  shuffleSeed: string,
): GuideStep[] {
  const steps: GuideStep[] = [{ key: 'intro', kind: 'intro' }]

  for (const [index, event] of (caseData.timeline || []).entries()) {
    const reflection = REFLECTION_BY_TYPE[event.type] || REFLECTION_BY_TYPE.warning
    const key = `timeline-${event.id || index}`
    steps.push({
      key,
      kind: 'timeline',
      event,
      index,
      question: reflection.question,
      options: cloneOptions(reflection.options, `${shuffleSeed}-${key}`),
    })
  }

  steps.push({
    key: 'cause',
    kind: 'cause',
    question: '结合原因分析，以下哪项最可能是本起事故的根本症结？',
    options: cloneOptions([
      { id: 'a', text: '技术故障与安全管理缺陷叠加', correct: true },
      { id: 'b', text: '纯属不可预见的偶然事件', correct: false },
      { id: 'c', text: '仅由操作人员个人失误导致', correct: false },
      { id: 'd', text: '与巡检和应急预案无关', correct: false },
    ], `${shuffleSeed}-cause`),
  })

  const lessonCorrect = (caseData.lessons && caseData.lessons[0]) || '完善巡检与应急预案并加强联动'
  steps.push({
    key: 'lessons',
    kind: 'lessons',
    question: '复盘后，你认为最应优先落实的改进措施是？',
    options: cloneOptions([
      { id: 'a', text: lessonCorrect, correct: true },
      { id: 'b', text: '降低安全投入以压缩成本', correct: false },
      { id: 'c', text: '减少人员培训频次', correct: false },
      { id: 'd', text: '忽视BMS与消防系统维护', correct: false },
    ], `${shuffleSeed}-lessons`),
  })

  steps.push({ key: 'summary', kind: 'summary' })
  return steps
}

export function phaseLabel(type: string): string {
  const map: Record<string, string> = {
    warning: '预警', danger: '扩大', action: '处置', info: '控制', success: '控制',
  }
  return map[type] || '节点'
}

export function phaseColor(type: string): string {
  const map: Record<string, string> = {
    warning: '#f59e0b', danger: '#ef4444', action: '#3b82f6', info: '#10b981', success: '#10b981',
  }
  return map[type] || '#6b7280'
}

export function stripReflectionMeta(reflections: Record<string, string>): Record<string, string> {
  const { [SEED_META_KEY]: _, ...rest } = reflections
  return rest
}
