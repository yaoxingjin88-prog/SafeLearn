<template>
  <div class="wrong-questions-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" text>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>
          <el-icon><Notebook /></el-icon>
          错题本
        </h2>
      </div>
      <div class="header-right" v-if="data">
        <el-tag type="danger" size="large">共 {{ data.totalWrong }} 道错题</el-tag>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p>加载错题本...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!data || data.totalWrong === 0" class="empty-container">
      <el-empty description="暂无错题记录">
        <template #image>
          <div class="empty-icon">
            <el-icon :size="80"><CircleCheck /></el-icon>
          </div>
        </template>
        <el-button type="primary" @click="goToCourses">去学习课程</el-button>
      </el-empty>
    </div>

    <!-- 错题列表 -->
    <template v-else>
      <!-- 按章节分组展示 -->
      <div v-for="(questions, chapterId) in data.chapters" :key="chapterId" class="chapter-section">
        <div class="chapter-header">
          <h3>
            <el-icon><Document /></el-icon>
            {{ getChapterTitle(questions) }}
          </h3>
          <el-tag size="small">{{ questions.length }} 道错题</el-tag>
        </div>

        <div class="questions-list">
          <div
            v-for="(question, index) in questions"
            :key="question.questionId"
            class="question-card"
          >
            <div class="question-top">
              <el-tag :type="getQuestionTypeTag(question.type)" size="small">
                {{ getQuestionTypeLabel(question.type) }}
              </el-tag>
              <span class="question-date">{{ formatDate(question.attemptDate) }}</span>
            </div>

            <div class="question-text">{{ question.question }}</div>

            <div class="options-list">
              <div
                v-for="option in question.options"
                :key="option.id"
                class="option-item"
                :class="{
                  'correct-answer': option.id === question.correctAnswer,
                  'your-wrong-answer': option.id === question.userAnswer && option.id !== question.correctAnswer
                }"
              >
                <div class="option-marker">{{ option.id.toUpperCase() }}</div>
                <div class="option-text">{{ option.text }}</div>
                <div v-if="option.id === question.correctAnswer" class="option-badge correct">
                  <el-icon><Check /></el-icon>
                  正确答案
                </div>
                <div v-if="option.id === question.userAnswer && option.id !== question.correctAnswer" class="option-badge wrong">
                  <el-icon><Close /></el-icon>
                  你的答案
                </div>
              </div>
            </div>

            <div v-if="question.explanation" class="explanation">
              <el-icon><InfoFilled /></el-icon>
              <span>{{ question.explanation }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="bottom-actions">
        <el-button @click="goToCourses" type="primary" size="large">
          <el-icon><Reading /></el-icon>
          继续学习
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Notebook, Loading, CircleCheck, Document,
  Check, Close, InfoFilled, Reading
} from '@element-plus/icons-vue'
import { getWrongQuestions } from '@/api/quiz'
import type { WrongQuestionsData } from '@/api/quiz'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const data = ref<WrongQuestionsData | null>(null)

onMounted(async () => {
  try {
    const res = await getWrongQuestions()
    if (res.code === 200 && res.data) {
      data.value = res.data
    } else {
      ElMessage.error(res.message || '加载错题本失败')
    }
  } catch (error) {
    ElMessage.error('加载错题本失败')
  } finally {
    loading.value = false
  }
})

function getChapterTitle(questions: Array<{ chapterTitle?: string }>): string {
  if (questions.length > 0 && questions[0].chapterTitle) {
    return questions[0].chapterTitle
  }
  return '未知章节'
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

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function goBack() {
  router.back()
}

function goToCourses() {
  router.push(p('/courses/list'))
}
</script>

<style scoped>
.wrong-questions-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
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
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 20px;
  color: #303133;
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

.empty-container {
  padding: 80px 0;
}

.empty-icon {
  color: #67c23a;
}

.chapter-section {
  margin-bottom: 32px;
}

.chapter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.chapter-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border-left: 4px solid #f56c6c;
}

.question-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.question-date {
  font-size: 12px;
  color: #909399;
}

.question-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 16px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.option-item.correct-answer {
  background: #f0f9eb;
  border-color: #67c23a;
}

.option-item.your-wrong-answer {
  background: #fef0f0;
  border-color: #f56c6c;
}

.option-marker {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #e4e7ed;
  font-weight: 600;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}

.option-item.correct-answer .option-marker {
  background: #67c23a;
  color: #fff;
}

.option-item.your-wrong-answer .option-marker {
  background: #f56c6c;
  color: #fff;
}

.option-text {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.option-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.option-badge.correct {
  color: #67c23a;
  background: #f0f9eb;
}

.option-badge.wrong {
  color: #f56c6c;
  background: #fef0f0;
}

.explanation {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 8px;
  color: #e6a23c;
  font-size: 13px;
  line-height: 1.5;
}

.explanation .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.bottom-actions {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
}
</style>
