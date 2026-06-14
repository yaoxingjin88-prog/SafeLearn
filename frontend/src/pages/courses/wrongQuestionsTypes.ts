export interface WrongQuestionItem {
  questionId: string
  question: string
  options: Array<{ id: string; text: string }>
  userAnswer: string
  correctAnswer: string
  explanation: string
  type: string
  chapterId: string
  chapterTitle: string
  courseId?: string
  attemptId: string
  attemptDate: string
}

export interface WrongQuestionsData {
  chapters: Record<string, WrongQuestionItem[]>
  totalWrong: number
}

export interface ChapterSummary {
  chapterId: string
  chapterTitle: string
  wrongCount: number
  totalCount: number
  lastPracticeDate: string
  courseId?: string
}

export type ChapterSortMode = 'count' | 'name'
export type DetailViewMode = 'time' | 'type'
