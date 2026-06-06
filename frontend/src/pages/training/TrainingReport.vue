<template>
  <div class="training-report">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">训练报告</span>
      </template>
    </el-page-header>

    <el-card class="mt-6">
      <!-- 报告头部 -->
      <div class="report-header">
        <div class="score-circle" :class="getScoreClass(record.totalScore)">
          <div class="score-value">{{ record.totalScore }}</div>
          <div class="score-label">总分</div>
        </div>
        <div class="report-info">
          <h2>{{ record.scenarioName }}</h2>
          <el-tag :type="getRatingType(record.rating)" size="large">
            {{ getRatingName(record.rating) }}
          </el-tag>
          <p class="mt-2 text-gray-500">完成时间: {{ record.completedAt }}</p>
        </div>
      </div>
    </el-card>

    <!-- 详细评分 -->
    <el-card class="mt-4" v-if="record.decisions && record.decisions.length > 0">
      <template #header>
        <span class="font-bold">决策详情</span>
      </template>
      <div
        v-for="(decision, index) in record.decisions"
        :key="index"
        class="decision-item"
      >
        <div class="decision-header">
          <span class="decision-index">决策 {{ Number(index) + 1 }}</span>
          <el-tag :type="decision.correct || decision.isCorrect ? 'success' : 'danger'">
            {{ decision.correct || decision.isCorrect ? '正确' : '错误' }}
          </el-tag>
        </div>
        <div class="decision-content">
          <div class="question">{{ decision.question || decision.decisionPointId }}</div>
          <div class="answer">
            <span class="label">您的选择:</span>
            <span :class="decision.correct || decision.isCorrect ? 'text-green-500' : 'text-red-500'">
              {{ decision.selectedAnswer || decision.optionId }}
            </span>
          </div>
          <div v-if="!(decision.correct || decision.isCorrect)" class="correct-answer">
            <span class="label">正确答案:</span>
            <span class="text-green-500">{{ decision.correctAnswer || '见解析' }}</span>
          </div>
          <div class="response-time">
            <span class="label">响应时间:</span>
            <span>{{ decision.responseTime || 0 }}秒</span>
          </div>
        </div>
        <div class="decision-score">
          得分: <span class="font-bold" :class="(decision.score || 0) > 0 ? 'text-green-500' : 'text-red-500'">
            +{{ decision.score || 0 }}
          </span>
        </div>
      </div>
    </el-card>

    <!-- 评价与建议 -->
    <el-card class="mt-4">
      <template #header>
        <span class="font-bold">评价与建议</span>
      </template>
      <div class="feedback-content">
        <p>{{ record.feedback }}</p>
      </div>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-buttons mt-6">
      <el-button type="primary" size="large" @click="$router.push('/training')">
        返回训练列表
      </el-button>
      <el-button size="large" @click="handleRetry">
        重新训练
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()

const record = ref<any>({
  id: '', scenarioId: '', scenarioName: '', completedAt: '', totalScore: 0, rating: 'good',
  feedback: '', decisions: [],
})

function getScoreClass(score: number) {
  if (score >= 90) return 'score-excellent'
  if (score >= 70) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-poor'
}

function getRatingType(rating: string) {
  const map: Record<string, string> = {
    excellent: 'success',
    good: '',
    average: 'warning',
    poor: 'danger',
  }
  return map[rating] || ''
}

function getRatingName(rating: string) {
  const map: Record<string, string> = {
    excellent: '优秀',
    good: '良好',
    average: '及格',
    poor: '不及格',
  }
  return map[rating] || rating
}

function handleRetry() {
  const scenarioId = record.value.scenarioId || (route.query.scenarioId as string) || ''
  if (scenarioId) {
    router.push(`/training/${scenarioId}`)
  } else {
    router.push('/training')
  }
}

onMounted(async () => {
  const id = route.params.id as string
  const res = await request.get(`/training/records/${id}`)
  record.value = {
    scenarioId: route.query.scenarioId || res.data.scenarioId,
    ...res.data,
  }
})
</script>

<style scoped>
.report-header {
  display: flex;
  align-items: center;
  gap: 32px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
}

.score-excellent {
  background: linear-gradient(135deg, #10b981, #059669);
}

.score-good {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.score-average {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.score-poor {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.score-value {
  font-size: 36px;
  font-weight: bold;
}

.score-label {
  font-size: 14px;
  opacity: 0.9;
}

.report-info h2 {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #1f2937;
}

.decision-item {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 16px;
}

.decision-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.decision-index {
  font-weight: 600;
  color: #374151;
}

.decision-content {
  margin-bottom: 12px;
}

.question {
  font-size: 16px;
  color: #1f2937;
  margin-bottom: 12px;
}

.answer, .correct-answer, .response-time {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.label {
  font-weight: 500;
  margin-right: 8px;
}

.decision-score {
  text-align: right;
  font-size: 14px;
  color: #6b7280;
}

.feedback-content {
  font-size: 15px;
  line-height: 1.8;
  color: #374151;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
}
</style>
