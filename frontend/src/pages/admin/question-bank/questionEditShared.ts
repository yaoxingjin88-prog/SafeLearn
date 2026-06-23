export const OPTION_LABELS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

export const QUESTION_TYPE_EDIT_OPTIONS = [
  { label: '单选题', value: 'single' },
  { label: '多选题', value: 'multiple' },
  { label: '判断题', value: 'truefalse' },
  { label: '简答题', value: 'short' },
  { label: '案例分析题', value: 'case' },
] as const

export const DIFFICULTY_EDIT_OPTIONS = [
  { label: '易', value: 'easy' },
  { label: '中', value: 'medium' },
  { label: '难', value: 'hard' },
] as const

export interface QuestionOptionItem {
  id: string
  text: string
  correct: boolean
}

export interface QuestionAttachmentFile {
  name: string
  size?: number
  url?: string
  type?: string
}

export interface QuestionAttachments {
  files: QuestionAttachmentFile[]
  images: QuestionAttachmentFile[]
  norms: QuestionAttachmentFile[]
}

export interface QuestionSettings {
  allowComments: boolean
  allowReport: boolean
  showAnalysisInExam: boolean
}

export function defaultAttachments(): QuestionAttachments {
  return { files: [], images: [], norms: [] }
}

export function defaultSettings(): QuestionSettings {
  return { allowComments: false, allowReport: true, showAnalysisInExam: true }
}

export function createDefaultOptions(type: string): QuestionOptionItem[] {
  if (type === 'truefalse') {
    return [
      { id: 'a', text: '正确', correct: true },
      { id: 'b', text: '错误', correct: false },
    ]
  }
  if (type === 'multiple' || type === 'single') {
    return [
      { id: 'a', text: '', correct: false },
      { id: 'b', text: '', correct: true },
      { id: 'c', text: '', correct: false },
      { id: 'd', text: '', correct: false },
    ]
  }
  return []
}

export function needsOptions(type: string) {
  return ['single', 'multiple', 'truefalse'].includes(type)
}

export function optionLabel(index: number) {
  return OPTION_LABELS[index] || String(index + 1)
}

export function parseOptions(raw: unknown): QuestionOptionItem[] {
  if (!raw) return []
  if (typeof raw === 'string') {
    try {
      return parseOptions(JSON.parse(raw))
    } catch {
      return []
    }
  }
  if (!Array.isArray(raw)) return []
  return raw.map((item, index) => ({
    id: String(item.id || OPTION_LABELS[index]?.toLowerCase() || `opt${index}`),
    text: String(item.text || ''),
    correct: Boolean(item.correct),
  }))
}

export function parseAttachments(raw: unknown): QuestionAttachments {
  const base = defaultAttachments()
  if (!raw || typeof raw !== 'object') return base
  const data = raw as Partial<QuestionAttachments>
  return {
    files: Array.isArray(data.files) ? data.files : [],
    images: Array.isArray(data.images) ? data.images : [],
    norms: Array.isArray(data.norms) ? data.norms : [],
  }
}

export function parseSettings(raw: unknown): QuestionSettings {
  const base = defaultSettings()
  if (!raw || typeof raw !== 'object') return base
  const data = raw as Partial<QuestionSettings>
  return {
    allowComments: data.allowComments ?? base.allowComments,
    allowReport: data.allowReport ?? base.allowReport,
    showAnalysisInExam: data.showAnalysisInExam ?? base.showAnalysisInExam,
  }
}

export function syncSingleCorrect(options: QuestionOptionItem[], selectedId: string) {
  options.forEach(opt => {
    opt.correct = opt.id === selectedId
  })
}

export function getSingleCorrectId(options: QuestionOptionItem[]) {
  return options.find(opt => opt.correct)?.id || options[0]?.id || ''
}

export function difficultyTagType(difficulty: string): '' | 'success' | 'warning' | 'danger' {
  if (difficulty === 'easy') return 'success'
  if (difficulty === 'hard') return 'danger'
  return 'warning'
}
