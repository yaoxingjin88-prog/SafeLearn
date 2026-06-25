<template>
  <div class="quiz-result-container">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p>加载结果中...</p>
    </div>

    <template v-else-if="result">
      <!-- 结果卡片 -->
      <div class="result-card" :class="{ passed: result.passed, failed: !result.passed }">
        <div class="result-header">
          <div class="score-circle" :class="{ passed: result.passed }">
            <span class="score-number">{{ result.score }}</span>
            <span class="score-unit">分</span>
          </div>
          <div class="result-info">
            <h2>{{ result.passed ? '恭喜通过！' : '未达到及格标准' }}</h2>
            <p class="result-rating">
              评级：<el-tag :type="getRatingTag(result.rating)" size="large">{{ getRatingLabel(result.rating) }}</el-tag>
            </p>
            <p class="result-threshold">及格分数：{{ result.passScore }} 分</p>
          </div>
        </div>

        <!-- 掌握度提升 -->
        <div class="mastery-update">
          <el-icon><TrendCharts /></el-icon>
          <span>掌握度已更新为：<strong>{{ result.masteryLevel }}%</strong></span>
        </div>

        <!-- 反馈 -->
        <div class="feedback">
          <el-icon><ChatLineSquare /></el-icon>
          <span>{{ result.feedback }}</span>
        </div>
      </div>

      <!-- 答题详情 -->
      <div class="detail-section">
        <h3>
          <el-icon><Document /></el-icon>
          答题详情
        </h3>

        <div class="questions-list">
          <div
            v-for="(item, index) in result.results"
            :key="item.questionId"
            class="question-item"
            :class="{ correct: item.correct, wrong: !item.correct }"
          >
            <div class="question-header">
              <span class="question-index">第 {{ index + 1 }} 题</span>
              <el-tag v-if="item.sourceChapterTitle" type="info" size="small" effect="plain">
                {{ item.sourceChapterTitle }}
              </el-tag>
              <el-tag :type="item.correct ? 'success' : 'danger'" size="small">
                {{ item.correct ? '正确' : '错误' }}
              </el-tag>
            </div>
            <div class="question-text">{{ getQuestionText(item.questionId) }}</div>
            <div class="answer-info">
              <div class="your-answer">
                <span class="label">你的答案：</span>
                <span :class="{ 'text-success': item.correct, 'text-danger': !item.correct }">
                  {{ item.userAnswer || '未作答' }}
                </span>
              </div>
              <div v-if="!item.correct" class="correct-answer">
                <span class="label">正确答案：</span>
                <span class="text-success">{{ item.correctAnswer }}</span>
              </div>
            </div>
            <div v-if="item.explanation" class="explanation">
              <el-icon><InfoFilled /></el-icon>
              <span>{{ item.explanation }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="result-actions">
        <el-button @click="goBack" size="large">
          <el-icon><ArrowLeft /></el-icon>
          {{ examType === 'comprehensive' ? '返回课程' : '返回章节' }}
        </el-button>
        <el-button v-if="!result.passed" @click="retryQuiz" type="primary" size="large">
          <el-icon><RefreshRight /></el-icon>
          {{ examType === 'comprehensive' ? '重新考试' : '重新测验' }}
        </el-button>
        <el-button @click="goToWrongQuestions" type="warning" size="large" plain>
          <el-icon><Notebook /></el-icon>
          错题本
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Loading, TrendCharts, ChatLineSquare, Document, InfoFilled,
  ArrowLeft, RefreshRight, Notebook
} from '@element-plus/icons-vue'
import { getQuizByChapter, startQuiz, submitQuiz } from '@/api/quiz'
import type { QuizSubmitResult } from '@/api/quiz'
import { useAppBase } from '@/composables/useAppBase'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const result = ref<QuizSubmitResult | null>(null)
const chapterId = ref('')
const courseId = ref('')
const examType = ref<'chapter' | 'comprehensive' | 'paper'>('chapter')
const paperId = ref('')
const questionMap = ref<Record<string, string>>({})

onMounted(async () => {
  try {
    chapterId.value = route.query.chapterId as string || ''
    courseId.value = route.query.courseId as string || ''
    examType.value = route.query.examType === 'comprehensive'
      ? 'comprehensive'
      : route.query.examType === 'paper'
        ? 'paper'
        : 'chapter'

    paperId.value = route.query.paperId as string || ''

    const storedResult = sessionStorage.getItem('quizResult')
    if (storedResult) {
      result.value = JSON.parse(storedResult)
      sessionStorage.removeItem('quizResult')
      buildQuestionMap()
    } else {
      ElMessage.warning('测验结果已过期')
      goBack()
    }
  } catch (error) {
    ElMessage.error('加载结果失败')
  } finally {
    loading.value = false
  }
})

function buildQuestionMap() {
  questionMap.value = {}
  if (!result.value) return
  for (const q of result.value.wrongQuestions || []) {
    questionMap.value[q.questionId] = q.question
  }
  for (const q of result.value.questions || []) {
    questionMap.value[q.id] = q.question
  }
}

function getQuestionText(questionId: string): string {
  if (!result.value) return ''
  const wrongQ = result.value.wrongQuestions?.find(q => q.questionId === questionId)
  if (wrongQ) return wrongQ.question
  if (questionMap.value[questionId]) return questionMap.value[questionId]
  return `题目 ${questionId}`
}

function getRatingTag(rating: string) {
  switch (rating) {
    case 'excellent': return 'success'
    case 'good': return 'primary'
    case 'average': return 'warning'
    default: return 'danger'
  }
}

function getRatingLabel(rating: string) {
  switch (rating) {
    case 'excellent': return '优秀'
    case 'good': return '良好'
    case 'average': return '合格'
    default: return '不合格'
  }
}

function goBack() {
  if (examType.value === 'paper') {
    router.replace(p('/courses/exams'))
  } else if (examType.value === 'comprehensive' && courseId.value) {
    router.replace(p(`/courses/${courseId.value}`))
  } else if (courseId.value && chapterId.value) {
    router.replace(p(`/courses/${courseId.value}/chapters/${chapterId.value}`))
  } else {
    router.replace(p('/courses/list'))
  }
}

function retryQuiz() {
  if (examType.value === 'paper' && paperId.value) {
    router.replace(p(`/exams/${paperId.value}`))
  } else if (examType.value === 'comprehensive' && courseId.value) {
    router.replace(p(`/courses/${courseId.value}/comprehensive-exam`))
  } else if (chapterId.value) {
    router.replace(p(`/courses/${courseId.value}/chapters/${chapterId.value}/quiz`))
  }
}

function goToWrongQuestions() {
  router.push(p('/courses/wrong-questions'))
}
</script>

<style scoped>
.quiz-result-container {
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

.result-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.result-card.passed {
  border-top: 4px solid #67c23a;
}

.result-card.failed {
  border-top: 4px solid #f56c6c;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 24px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%);
  flex-shrink: 0;
}

.score-circle.passed {
  background: linear-gradient(135deg, #67c23a 0%, #409eff 100%);
}

.score-number {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.score-unit {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 4px;
}

.result-info h2 {
  margin: 0 0 12px 0;
  font-size: 24px;
  color: #303133;
}

.result-rating {
  margin: 8px 0;
  font-size: 16px;
  color: #606266;
}

.result-threshold {
  margin: 4px 0;
  font-size: 14px;
  color: #909399;
}

.mastery-update {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #ecf5ff;
  border-radius: 8px;
  color: #409eff;
  font-size: 14px;
  margin-bottom: 16px;
}

.mastery-update strong {
  font-size: 18px;
}

.feedback {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  color: #e6a23c;
  font-size: 14px;
  line-height: 1.6;
}

.feedback .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.detail-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.detail-section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #303133;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #ebeef5;
}

.question-item.correct {
  background: #f0f9eb;
  border-left-color: #67c23a;
}

.question-item.wrong {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.question-index {
  font-size: 13px;
  color: #909399;
}

.question-text {
  font-size: 15px;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.5;
}

.answer-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
}

.your-answer,
.correct-answer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  color: #909399;
}

.text-success {
  color: #67c23a;
  font-weight: 500;
}

.text-danger {
  color: #f56c6c;
  font-weight: 500;
}

.explanation {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 6px;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.explanation .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}
</style>
