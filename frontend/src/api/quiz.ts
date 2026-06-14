import request from './request'
import type { ApiResponse } from '@/types'

export interface QuizQuestion {
  id: string
  type: 'single' | 'multiple' | 'truefalse'
  question: string
  options: Array<{
    id: string
    text: string
    correct?: boolean
  }>
  explanation?: string
}

export interface Quiz {
  id: string
  chapterId: string
  title: string
  passScore: number
  timeLimit: number | null
  questions: QuizQuestion[]
}

export interface QuizAttempt {
  id: string
  quizId: string
  score: number
  passed: boolean
  masteryLevel: number
  startedAt: string
  completedAt: string | null
}

export interface QuizSubmitResult {
  attemptId: string
  score: number
  passed: boolean
  passScore: number
  masteryLevel: number
  rating: string
  feedback: string
  results: Array<{
    questionId: string
    correct: boolean
    userAnswer: string
    correctAnswer: string
    explanation: string
  }>
  wrongQuestions: Array<{
    questionId: string
    question: string
    options: Array<{ id: string; text: string }>
    userAnswer: string
    correctAnswer: string
    explanation: string
    type: string
  }>
}

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

/** 获取章节测验 */
export function getQuizByChapter(chapterId: string): Promise<ApiResponse<Quiz>> {
  return request.get(`/quiz/chapter/${chapterId}`)
}

/** 章节测验与学习状态 */
export interface ChapterQuizStatus {
  exists: boolean
  chapterCompleted?: boolean
  quizPassed?: boolean
  bestScore?: number
  masteryLevel?: number
}

/** 检查章节是否有测验及完成状态 */
export function checkQuizExists(chapterId: string): Promise<ApiResponse<ChapterQuizStatus>> {
  return request.get(`/quiz/chapter/${chapterId}/exists`)
}

/** 开始测验 */
export function startQuiz(quizId: string): Promise<ApiResponse<{ attemptId: string; quiz: Quiz; startedAt: string }>> {
  return request.post(`/quiz/${quizId}/start`)
}

/** 提交测验答案 */
export function submitQuiz(attemptId: string, answers: Record<string, string>): Promise<ApiResponse<QuizSubmitResult>> {
  return request.post(`/quiz/attempts/${attemptId}/submit`, { answers })
}

/** 获取测验历史 */
export function getQuizHistory(chapterId: string): Promise<ApiResponse<QuizAttempt[]>> {
  return request.get('/quiz/history', { params: { chapterId } })
}

/** 获取错题本 */
export function getWrongQuestions(): Promise<ApiResponse<WrongQuestionsData>> {
  return request.get('/quiz/wrong-questions')
}

/** 获取测验详情（含答案） */
export function getQuizDetail(quizId: string): Promise<ApiResponse<Quiz>> {
  return request.get(`/quiz/${quizId}/detail`)
}
