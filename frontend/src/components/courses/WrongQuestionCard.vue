<script setup lang="ts">
import { Check, Close, InfoFilled } from '@element-plus/icons-vue'
import { isAnswerSelected } from '@/composables/useWrongQuestionMastered'
import type { WrongQuestionItem } from '@/pages/courses/wrongQuestionsTypes'

defineProps<{
  question: WrongQuestionItem
  showMasterAction?: boolean
}>()

const emit = defineEmits<{ mastered: [] }>()

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
</script>

<template>
  <div class="question-card">
    <div class="question-top">
      <div class="question-top-left">
        <el-tag :type="getQuestionTypeTag(question.type)" size="small">
          {{ getQuestionTypeLabel(question.type) }}
        </el-tag>
        <span class="question-date">{{ formatDate(question.attemptDate) }}</span>
      </div>
      <el-button
        v-if="showMasterAction"
        type="success"
        link
        size="small"
        @click="emit('mastered')"
      >
        标记已掌握
      </el-button>
    </div>

    <div class="question-text">{{ question.question }}</div>

    <div class="options-list">
      <div
        v-for="option in question.options"
        :key="option.id"
        class="option-item"
        :class="{
          'correct-answer': isAnswerSelected(option.id, question.correctAnswer),
          'your-wrong-answer': isAnswerSelected(option.id, question.userAnswer)
            && !isAnswerSelected(option.id, question.correctAnswer),
        }"
      >
        <div class="option-marker">{{ option.id.toUpperCase() }}</div>
        <div class="option-text">{{ option.text }}</div>
        <div v-if="isAnswerSelected(option.id, question.correctAnswer)" class="option-badge correct">
          <el-icon><Check /></el-icon>
          正确答案
        </div>
        <div
          v-if="isAnswerSelected(option.id, question.userAnswer)
            && !isAnswerSelected(option.id, question.correctAnswer)"
          class="option-badge wrong"
        >
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
</template>

<style scoped>
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
  gap: 12px;
  margin-bottom: 12px;
}

.question-top-left {
  display: flex;
  align-items: center;
  gap: 10px;
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
</style>
