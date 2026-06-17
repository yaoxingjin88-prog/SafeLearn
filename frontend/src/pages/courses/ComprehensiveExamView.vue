<template>
  <div class="quiz-container">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p>加载综合考试中...</p>
    </div>

    <div v-else-if="quiz && !submitted" class="quiz-header">
      <div class="header-left">
        <el-button @click="goBack" text>
          <el-icon><ArrowLeft /></el-icon>
          返回课程
        </el-button>
        <div>
          <h2>{{ quiz.title }}</h2>
          <p class="exam-subtitle">跨章节抽题 · 共 {{ quiz.questions.length }} 题</p>
        </div>
      </div>
      <div class="header-right">
        <div v-if="quiz.timeLimit" class="timer">
          <el-icon><Timer /></el-icon>
          <span :class="{ 'time-warning': remainingTime < 60 }">{{ formatTime(remainingTime) }}</span>
        </div>
        <span class="progress-text">{{ currentIndex + 1 }} / {{ quiz.questions.length }}</span>
      </div>
    </div>

    <div v-if="quiz && !submitted" class="quiz-content">
      <el-progress
        :percentage="((currentIndex + 1) / quiz.questions.length) * 100"
        :stroke-width="8"
        :show-text="false"
        class="quiz-progress"
      />

      <div class="question-card">
        <div class="question-header">
          <el-tag :type="getQuestionTypeTag(currentQuestion.type)" size="small">
            {{ getQuestionTypeLabel(currentQuestion.type) }}
          </el-tag>
          <el-tag v-if="currentQuestion.sourceChapterTitle" type="info" size="small" effect="plain">
            {{ currentQuestion.sourceChapterTitle }}
          </el-tag>
          <span class="question-index">第 {{ currentIndex + 1 }} 题</span>
        </div>

        <h3 class="question-text">{{ currentQuestion.question }}</h3>

        <div class="options-list">
          <div
            v-for="option in currentQuestion.options"
            :key="option.id"
            class="option-item"
            :class="{
              selected: answers[currentQuestion.id] === option.id,
              correct: showAnswer && option.correct,
              wrong: showAnswer && answers[currentQuestion.id] === option.id && !option.correct,
              disabled: showAnswer,
            }"
            @click="!showAnswer && selectOption(option.id)"
          >
            <div class="option-marker">{{ option.id.toUpperCase() }}</div>
            <div class="option-text">{{ option.text }}</div>
            <div v-if="showAnswer && option.correct" class="option-icon correct-icon">
              <el-icon><Check /></el-icon>
            </div>
            <div v-if="showAnswer && answers[currentQuestion.id] === option.id && !option.correct" class="option-icon wrong-icon">
              <el-icon><Close /></el-icon>
            </div>
          </div>
        </div>

        <div v-if="showAnswer && currentQuestion.explanation" class="explanation">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ currentQuestion.explanation }}</span>
        </div>
      </div>

      <div class="quiz-actions">
        <el-button @click="prevQuestion" :disabled="currentIndex === 0">
          <el-icon><ArrowLeft /></el-icon>
          上一题
        </el-button>

        <el-button v-if="!showAnswer && answers[currentQuestion.id]" type="primary" plain @click="checkCurrentAnswer">
          检查答案
        </el-button>

        <el-button v-if="showAnswer && currentIndex < quiz.questions.length - 1" type="primary" @click="nextQuestion">
          下一题
          <el-icon><ArrowRight /></el-icon>
        </el-button>

        <el-button
          v-if="showAnswer && currentIndex === quiz.questions.length - 1"
          type="success"
          @click="handleSubmitExam"
        >
          提交考试
          <el-icon><Check /></el-icon>
        </el-button>

        <el-button
          v-if="!showAnswer && currentIndex === quiz.questions.length - 1"
          type="warning"
          @click="confirmSubmit"
        >
          提交答案
          <el-icon><Promotion /></el-icon>
        </el-button>
      </div>
    </div>

    <div v-if="submitting" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p>正在评分...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Loading, ArrowLeft, ArrowRight, Timer, Check, Close,
  InfoFilled, Promotion,
} from '@element-plus/icons-vue'
import {
  getComprehensiveExamStatus,
  startComprehensiveExam,
  submitComprehensiveExam,
} from '@/api/quiz'
import type { Quiz, QuizQuestion } from '@/api/quiz'
import { useAppBase } from '@/composables/useAppBase'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const submitting = ref(false)
const submitted = ref(false)
const quiz = ref<Quiz | null>(null)
const attemptId = ref('')
const currentIndex = ref(0)
const answers = ref<Record<string, string>>({})
const showAnswer = ref(false)
const remainingTime = ref(0)
let timerInterval: ReturnType<typeof setInterval> | null = null

const courseId = computed(() => route.params.courseId as string)

const currentQuestion = computed(() => {
  if (!quiz.value) return {} as QuizQuestion
  return quiz.value.questions[currentIndex.value]
})

onMounted(async () => {
  try {
    const statusRes = await getComprehensiveExamStatus(courseId.value)
    const status = statusRes.data
    if (!status?.available) {
      ElMessage.warning(status?.eligibilityHint || '该课程暂不支持综合考试')
      goBack()
      return
    }
    if (!status.eligible) {
      ElMessage.warning(status.eligibilityHint || '暂不符合参加条件')
      goBack()
      return
    }
    if (status.examPassed) {
      ElMessage.info('综合考试已通过，无需重复参加')
      goBack()
      return
    }

    const startRes = await startComprehensiveExam(courseId.value)
    if (startRes.code !== 200 || !startRes.data) {
      ElMessage.error('开始综合考试失败')
      goBack()
      return
    }

    quiz.value = startRes.data.quiz
    attemptId.value = startRes.data.attemptId

    if (quiz.value.timeLimit) {
      remainingTime.value = quiz.value.timeLimit * 60
      startTimer()
    }
  } catch {
    ElMessage.error('加载综合考试失败')
    goBack()
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  stopTimer()
})

function startTimer() {
  timerInterval = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
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

function selectOption(optionId: string) {
  if (!currentQuestion.value) return
  answers.value[currentQuestion.value.id] = optionId
}

function checkCurrentAnswer() {
  showAnswer.value = true
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    showAnswer.value = false
  }
}

function nextQuestion() {
  if (quiz.value && currentIndex.value < quiz.value.questions.length - 1) {
    currentIndex.value++
    showAnswer.value = false
  }
}

async function confirmSubmit() {
  const unanswered = quiz.value!.questions.length - Object.keys(answers.value).length
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
    const res = await submitComprehensiveExam(attemptId.value, answers.value)
    if (res.code === 200 && res.data) {
      sessionStorage.setItem('quizResult', JSON.stringify(res.data))
      router.replace({
        path: p(`/courses/quiz/result/${res.data.attemptId}`),
        query: { courseId: courseId.value, examType: 'comprehensive' },
      })
    } else {
      submitted.value = false
      ElMessage.error(res.message || '提交失败')
    }
  } catch {
    submitted.value = false
    ElMessage.error('提交考试失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.replace(p(`/courses/${courseId.value}`))
}

function getQuestionTypeTag(type: string) {
  switch (type) {
    case 'single': return 'primary'
    case 'multiple': return 'success'
    case 'truefalse': return 'warning'
    default: return 'info'
  }
}

function getQuestionTypeLabel(type: string) {
  switch (type) {
    case 'single': return '单选题'
    case 'multiple': return '多选题'
    case 'truefalse': return '判断题'
    default: return '未知'
  }
}
</script>

<style scoped>
.quiz-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #909399;
}

.loading-container p {
  margin-top: 16px;
  font-size: 14px;
}

.quiz-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.exam-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.timer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.timer .time-warning {
  color: #e6a23c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.progress-text {
  font-size: 14px;
  color: #909399;
}

.quiz-progress {
  margin-bottom: 24px;
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.question-index {
  font-size: 14px;
  color: #909399;
}

.question-text {
  margin: 0 0 24px;
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover:not(.disabled) {
  border-color: #c0c4cc;
  background: #f5f7fa;
}

.option-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.option-item.correct {
  border-color: #67c23a;
  background: #f0f9eb;
}

.option-item.wrong {
  border-color: #f56c6c;
  background: #fef0f0;
}

.option-item.disabled {
  cursor: default;
}

.option-marker {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f0f2f5;
  font-weight: 600;
  color: #606266;
  flex-shrink: 0;
}

.option-item.selected .option-marker {
  background: #409eff;
  color: #fff;
}

.option-item.correct .option-marker {
  background: #67c23a;
  color: #fff;
}

.option-item.wrong .option-marker {
  background: #f56c6c;
  color: #fff;
}

.option-text {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.option-icon {
  flex-shrink: 0;
}

.correct-icon {
  color: #67c23a;
  font-size: 18px;
}

.wrong-icon {
  color: #f56c6c;
  font-size: 18px;
}

.explanation {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  color: #e6a23c;
  font-size: 14px;
  line-height: 1.6;
}

.explanation .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.quiz-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}
</style>
