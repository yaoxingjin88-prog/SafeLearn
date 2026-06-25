<template>
  <div class="paper-exam-page">
  <div v-if="loading" class="exam-loading">
    <el-icon class="is-loading" :size="40"><Loading /></el-icon>
    <p>加载考试中...</p>
  </div>

  <div v-else-if="quiz && !submitted" class="exam-shell">
    <div class="exam-bg" aria-hidden="true" />

    <div class="exam-main">
      <header class="exam-header">
        <button type="button" class="back-btn" @click="confirmLeave">
          <el-icon><ArrowLeft /></el-icon>
          返回考试中心
        </button>

        <div class="header-center">
          <h1 class="exam-title">{{ quiz.title }}</h1>
          <p class="exam-meta">
            独立试卷，共 {{ totalQuestions }} 题，及格 {{ quiz.passScore ?? 60 }} 分
          </p>
        </div>

        <div class="header-actions">
          <div class="header-stat">
            <div class="stat-value" :class="{ 'time-warning': remainingTime < 60 }">
              <el-icon><Timer /></el-icon>
              {{ formatTime(remainingTime) }}
            </div>
            <span class="stat-label">考试剩余时间</span>
          </div>
          <div class="header-stat">
            <div class="stat-value stat-progress">{{ currentIndex + 1 }} / {{ totalQuestions }}</div>
            <span class="stat-label">当前进度</span>
          </div>
          <button
            type="button"
            class="icon-action"
            :class="{ active: isMarked(currentQuestion.id) }"
            @click="toggleMark"
          >
            <el-icon><CollectionTag /></el-icon>
            标记本题
          </button>
          <button type="button" class="icon-action" @click="toggleFullscreen">
            <el-icon><FullScreen /></el-icon>
            全屏
          </button>
          <button type="button" class="submit-btn" :disabled="submitting" @click="confirmSubmit">
            <el-icon><Promotion /></el-icon>
            提交试卷
          </button>
        </div>
      </header>

      <div class="progress-track">
        <div class="progress-fill" :style="{ width: `${answerProgress}%` }" />
        <span class="progress-label">{{ answerProgress }}%</span>
      </div>

      <div class="exam-body">
        <section class="question-panel">
          <div class="question-card">
            <div class="question-head">
              <span class="type-tag">{{ getQuestionTypeLabel(currentQuestion.type) }}</span>
              <span class="q-index">第 {{ currentIndex + 1 }} 题</span>
            </div>
            <h2 class="question-text">{{ currentQuestion.question }}</h2>

            <div v-if="currentQuestion.options?.length" class="options-list">
              <button
                v-for="(option, idx) in currentQuestion.options"
                :key="option.id"
                type="button"
                class="option-btn"
                :class="{ selected: isOptionSelected(currentQuestion, option.id) }"
                @click="selectOption(currentQuestion, option.id)"
              >
                <span class="option-letter">{{ optionLetter(idx) }}</span>
                <span class="option-text">{{ option.text }}</span>
              </button>
            </div>
            <el-input
              v-else
              v-model="answers[currentQuestion.id]"
              type="textarea"
              :rows="4"
              placeholder="请输入你的答案"
              class="short-answer"
            />
          </div>

          <div class="nav-row">
            <button type="button" class="nav-btn ghost" :disabled="currentIndex === 0" @click="prevQuestion">
              <el-icon><ArrowLeft /></el-icon>
              上一题
            </button>
            <button
              v-if="currentIndex < totalQuestions - 1"
              type="button"
              class="nav-btn primary"
              @click="nextQuestion"
            >
              下一题
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button v-else type="button" class="nav-btn warn" @click="confirmSubmit">
              提交试卷
              <el-icon><Promotion /></el-icon>
            </button>
          </div>
        </section>

        <aside class="answer-sheet">
          <h3 class="sheet-title">答题卡</h3>
          <div class="sheet-legend">
            <span><i class="dot answered" />已作答</span>
            <span><i class="dot unanswered" />未作答</span>
            <span><i class="dot marked" />待检查</span>
          </div>
          <div class="sheet-grid">
            <button
              v-for="(q, idx) in quiz.questions"
              :key="q.id"
              type="button"
              class="sheet-cell"
              :class="cellClass(q.id, idx)"
              @click="goToQuestion(idx)"
            >
              {{ idx + 1 }}
            </button>
          </div>
          <div class="sheet-stats">
            <div class="stat-item">
              已作答 <span class="c-answered">{{ answeredCount }}</span> 题
            </div>
            <div class="stat-item">
              未作答 <span class="c-unanswered">{{ unansweredCount }}</span> 题
            </div>
            <div class="stat-item">
              待检查 <span class="c-marked">{{ markedCount }}</span> 题
            </div>
          </div>
          <p class="sheet-tip">
            <el-icon><InfoFilled /></el-icon>
            小贴士：考试过程中请勿刷新或关闭页面，避免答题记录丢失。
          </p>
        </aside>
      </div>
    </div>
  </div>

  <div v-if="submitting" class="exam-loading overlay">
    <el-icon class="is-loading" :size="40"><Loading /></el-icon>
    <p>正在评分...</p>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Loading, ArrowLeft, ArrowRight, Timer, Promotion,
  CollectionTag, FullScreen, InfoFilled,
} from '@element-plus/icons-vue'
import { examApi } from '@/api/exam'
import { useAppBase } from '@/composables/useAppBase'

interface PaperQuestion {
  id: string
  type: string
  question: string
  options?: Array<{ id: string; text: string }>
}

interface PaperQuiz {
  id: string
  title: string
  passScore?: number
  timeLimit?: number
  questions: PaperQuestion[]
}

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const submitting = ref(false)
const submitted = ref(false)
const quiz = ref<PaperQuiz | null>(null)
const attemptId = ref('')
const currentIndex = ref(0)
const answers = ref<Record<string, string>>({})
const markedIds = ref<Set<string>>(new Set())
const remainingTime = ref(0)
let timerInterval: ReturnType<typeof setInterval> | null = null

const paperId = computed(() => route.params.id as string)
const storageKey = computed(() => `safelearn:paper-exam:${paperId.value}:${attemptId.value}`)

const totalQuestions = computed(() => quiz.value?.questions.length || 0)

const currentQuestion = computed(() => {
  if (!quiz.value?.questions.length) return {} as PaperQuestion
  return quiz.value.questions[currentIndex.value]
})

const answeredCount = computed(() => {
  if (!quiz.value) return 0
  return quiz.value.questions.filter(q => isAnswered(q.id)).length
})

const unansweredCount = computed(() => totalQuestions.value - answeredCount.value)
const markedCount = computed(() => markedIds.value.size)
const answerProgress = computed(() => {
  if (!totalQuestions.value) return 0
  return Math.round((answeredCount.value / totalQuestions.value) * 100)
})

function isAnswered(questionId: string) {
  const val = answers.value[questionId]
  return val != null && val !== ''
}

function isMarked(questionId?: string) {
  return questionId ? markedIds.value.has(questionId) : false
}

function isOptionSelected(question: PaperQuestion, optionId: string) {
  const val = answers.value[question.id]
  if (!val) return false
  if (question.type === 'multiple') {
    return val.split(',').includes(optionId)
  }
  return val === optionId
}

function optionLetter(index: number) {
  return String.fromCharCode(65 + index)
}

function cellClass(questionId: string, idx: number) {
  return {
    current: idx === currentIndex.value,
    answered: isAnswered(questionId) && !isMarked(questionId),
    marked: isMarked(questionId),
    unanswered: !isAnswered(questionId) && !isMarked(questionId),
  }
}

function persistLocal() {
  if (!attemptId.value) return
  try {
    sessionStorage.setItem(storageKey.value, JSON.stringify({
      answers: answers.value,
      marked: [...markedIds.value],
      currentIndex: currentIndex.value,
      remainingTime: remainingTime.value,
    }))
  } catch {
    // ignore
  }
}

function restoreLocal() {
  if (!attemptId.value) return
  try {
    const raw = sessionStorage.getItem(storageKey.value)
    if (!raw) return
    const data = JSON.parse(raw) as {
      answers?: Record<string, string>
      marked?: string[]
      currentIndex?: number
      remainingTime?: number
    }
    if (data.answers) answers.value = data.answers
    if (data.marked) markedIds.value = new Set(data.marked)
    if (typeof data.currentIndex === 'number') currentIndex.value = data.currentIndex
    if (typeof data.remainingTime === 'number' && data.remainingTime > 0) {
      remainingTime.value = data.remainingTime
    }
  } catch {
    // ignore
  }
}

watch([answers, markedIds, currentIndex], persistLocal, { deep: true })

function onBeforeUnload(e: BeforeUnloadEvent) {
  if (!submitted.value && quiz.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(async () => {
  window.addEventListener('beforeunload', onBeforeUnload)
  try {
    const statusRes = await examApi.getPaperStatus(paperId.value)
    const status = statusRes.data
    if (status.examPassed) {
      ElMessage.info('该考试已通过，无需重复参加')
      goBack()
      return
    }
    if (!status.questionCount) {
      ElMessage.warning('试卷暂无题目')
      goBack()
      return
    }

    const startRes = await examApi.startPaperExam(paperId.value)
    quiz.value = startRes.data.quiz
    attemptId.value = startRes.data.attemptId

    if (quiz.value?.timeLimit) {
      remainingTime.value = quiz.value.timeLimit * 60
    }
    restoreLocal()
    startTimer()
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(msg || '加载考试失败')
    goBack()
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  stopTimer()
  window.removeEventListener('beforeunload', onBeforeUnload)
})

function startTimer() {
  if (timerInterval) return
  timerInterval = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
      persistLocal()
    } else {
      stopTimer()
      ElMessage.warning('时间到，自动提交考试')
      handleSubmitExam()
    }
  }, 1000)
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function selectOption(question: PaperQuestion, optionId: string) {
  if (!question.id) return
  if (question.type === 'multiple') {
    const current = answers.value[question.id]
      ? answers.value[question.id].split(',').filter(Boolean)
      : []
    const idx = current.indexOf(optionId)
    if (idx >= 0) current.splice(idx, 1)
    else current.push(optionId)
    current.sort()
    answers.value[question.id] = current.join(',')
  } else {
    answers.value[question.id] = optionId
  }
}

function toggleMark() {
  const id = currentQuestion.value?.id
  if (!id) return
  const next = new Set(markedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  markedIds.value = next
}

function goToQuestion(idx: number) {
  if (idx >= 0 && idx < totalQuestions.value) {
    currentIndex.value = idx
  }
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (currentIndex.value < totalQuestions.value - 1) currentIndex.value++
}

async function confirmLeave() {
  try {
    await ElMessageBox.confirm('离开后将无法继续本次考试进度，确定返回吗？', '确认离开', {
      type: 'warning',
      confirmButtonText: '继续考试',
      cancelButtonText: '确认离开',
      distinguishCancelAndClose: true,
    })
  } catch (action) {
    if (action === 'cancel') goBack()
  }
}

async function confirmSubmit() {
  const unanswered = unansweredCount.value
  if (unanswered > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unanswered} 道题未作答，确定要提交吗？`,
        '确认提交',
        {
          confirmButtonText: '继续答题',
          cancelButtonText: '确认提交',
          distinguishCancelAndClose: true,
          type: 'warning',
        },
      )
      return
    } catch (action) {
      if (action !== 'cancel') return
    }
  }
  await handleSubmitExam()
}

async function handleSubmitExam() {
  if (!quiz.value || !attemptId.value || submitting.value) return

  submitting.value = true
  submitted.value = true
  stopTimer()

  try {
    const res = await examApi.submitPaperExam(attemptId.value, answers.value)
    sessionStorage.removeItem(storageKey.value)
    sessionStorage.setItem('quizResult', JSON.stringify(res.data))
    router.replace({
      path: p(`/courses/quiz/result/${attemptId.value}`),
      query: { examType: 'paper', paperId: paperId.value },
    })
  } catch {
    submitted.value = false
    startTimer()
    ElMessage.error('提交考试失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.replace(p('/courses/exams'))
}

function toggleFullscreen() {
  const el = document.documentElement
  if (!document.fullscreenElement) {
    el.requestFullscreen?.().catch(() => ElMessage.info('当前浏览器不支持全屏'))
  } else {
    document.exitFullscreen?.()
  }
}

function getQuestionTypeLabel(type: string) {
  const map: Record<string, string> = {
    single: '单选题',
    multiple: '多选题',
    truefalse: '判断题',
    short: '简答题',
    case: '案例题',
  }
  return map[type] || '题目'
}
</script>

<style scoped>
.paper-exam-page {
  height: 100%;
  min-height: 100%;
}

.exam-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 420px;
  color: #94a3b8;
}

.exam-loading.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(3, 7, 18, 0.72);
}

.exam-shell {
  position: relative;
  height: 100%;
  min-height: 100%;
  background: #0a192f;
  color: #e2e8f0;
  overflow: hidden;
}

.exam-bg {
  position: absolute;
  inset: 0;
  background: url('/考试背景.png') no-repeat left center / auto 100%;
  pointer-events: none;
  user-select: none;
}

.exam-shell::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent 28%, rgba(10, 25, 47, 0.55) 48%, rgba(10, 25, 47, 0.92) 72%);
  pointer-events: none;
}

.exam-main {
  position: relative;
  z-index: 1;
  height: 100%;
  max-width: 1560px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  padding: 22px 40px 28px;
  box-sizing: border-box;
}

.exam-header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  font-size: 15px;
  white-space: nowrap;
  transition: color 0.2s;
}

.back-btn:hover {
  color: #e2e8f0;
}

.header-center {
  text-align: center;
  min-width: 0;
}

.exam-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #f8fafc;
  line-height: 1.3;
}

.exam-meta {
  margin: 6px 0 0;
  font-size: 14px;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.header-stat {
  text-align: center;
  min-width: 88px;
}

.stat-value {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 24px;
  font-weight: 700;
  color: #60a5fa;
  line-height: 1.2;
}

.stat-value.stat-progress {
  color: #f1f5f9;
}

.stat-value.time-warning {
  color: #fbbf24;
  animation: pulse 1s infinite;
}

.stat-label {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: #94a3b8;
}

.icon-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #cbd5e1;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.2s, background 0.2s;
}

.icon-action:hover,
.icon-action.active {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
}

.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.35);
  transition: transform 0.15s, box-shadow 0.15s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(37, 99, 235, 0.45);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.progress-track {
  position: relative;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  margin-bottom: 26px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
  transition: width 0.25s ease;
}

.progress-label {
  position: absolute;
  right: 0;
  top: -20px;
  font-size: 11px;
  color: #64748b;
}

.exam-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: start;
}

.question-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  max-width: 100%;
}

.question-card {
  flex: 0 0 auto;
  background: #fff;
  border-radius: 16px;
  padding: 32px 36px 36px;
  color: #1e293b;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}

.question-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.type-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
}

.q-index {
  font-size: 14px;
  color: #64748b;
}

.question-text {
  margin: 0 0 26px;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.6;
  color: #0f172a;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.option-btn:hover {
  border-color: #93c5fd;
  background: #f8fafc;
}

.option-btn.selected {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.15);
}

.option-letter {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eff6ff;
  color: #3b82f6;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.option-btn.selected .option-letter {
  background: #2563eb;
  color: #fff;
}

.option-text {
  flex: 1;
  font-size: 15px;
  line-height: 1.55;
  color: #334155;
}

.short-answer {
  margin-top: 4px;
}

.nav-row {
  display: flex;
  justify-content: space-between;
  margin-top: 22px;
  gap: 12px;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.nav-btn.ghost {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.25);
  color: #e2e8f0;
}

.nav-btn.ghost:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.06);
}

.nav-btn.ghost:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.nav-btn.primary {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  border: none;
}

.nav-btn.warn {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: #fff;
  border: none;
}

.answer-sheet {
  display: flex;
  flex-direction: column;
  padding: 24px 22px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.12);
  backdrop-filter: blur(10px);
  align-self: start;
  width: 360px;
}

.sheet-title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 700;
  color: #f8fafc;
}

.sheet-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  margin-bottom: 18px;
  font-size: 13px;
  color: #94a3b8;
}

.sheet-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.dot.answered { background: #3b82f6; }
.dot.unanswered { background: transparent; border: 1px solid #64748b; }
.dot.marked { background: #fbbf24; }

.sheet-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 20px;
  max-height: none;
  overflow: visible;
}

.sheet-cell {
  aspect-ratio: 1;
  min-height: 0;
  height: 44px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(15, 23, 42, 0.5);
  color: #e2e8f0;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.sheet-cell.answered {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.sheet-cell.marked {
  background: #f59e0b;
  border-color: #f59e0b;
  color: #1e293b;
}

.sheet-cell.current {
  border-color: #fff;
  box-shadow: 0 0 0 2px #fff;
  color: #fff;
}

.sheet-cell.current.answered {
  box-shadow: 0 0 0 2px #fff, 0 0 0 4px rgba(59, 130, 246, 0.4);
}

.sheet-cell:hover:not(.current) {
  border-color: rgba(148, 163, 184, 0.6);
}

.sheet-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 16px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}

.stat-item {
  line-height: 1.5;
}

.stat-item .c-answered,
.stat-item .c-unanswered,
.stat-item .c-marked {
  font-size: 22px;
  font-weight: 700;
}

.c-answered { color: #60a5fa; }
.c-unanswered { color: #e2e8f0; }
.c-marked { color: #fbbf24; }

.sheet-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  padding-top: 14px;
  font-size: 12px;
  line-height: 1.55;
  color: #64748b;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

@media (max-width: 1100px) {
  .exam-main {
    max-width: none;
    padding: 16px;
  }

  .exam-header {
    grid-template-columns: 1fr;
    justify-items: stretch;
  }

  .header-center {
    text-align: left;
  }

  .header-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .exam-body {
    grid-template-columns: 1fr;
  }

  .answer-sheet {
    order: -1;
    width: 100%;
  }

  .sheet-grid {
    max-height: 180px;
    overflow-y: auto;
  }
}
</style>
