<template>
  <div class="training-view" v-if="currentDecisionPoint">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">{{ scenario.name }}</span>
      </template>
    </el-page-header>

    <div class="training-content mt-6">
      <!-- 场景描述 -->
      <el-card class="scenario-card">
        <div class="scenario-header">
          <el-icon :size="48" color="#f59e0b"><Warning /></el-icon>
          <div>
            <h2>{{ currentDecisionPoint.triggerCondition }}</h2>
            <p class="text-gray-500">{{ scenario.description }}</p>
          </div>
        </div>
      </el-card>

      <!-- 决策区域 -->
      <el-card class="decision-card mt-4">
        <div class="decision-header">
          <h3>{{ currentDecisionPoint.question }}</h3>
          <div class="timer" :class="{ 'timer-warning': timeLeft < 10 }">
            <el-icon><Timer /></el-icon>
            <span>{{ timeLeft }}秒</span>
          </div>
        </div>

        <div class="options-grid">
          <div
            v-for="option in currentDecisionPoint.options"
            :key="option.id"
            class="option-item"
            :class="{
              'option-selected': selectedOption === option.id,
              'option-correct': showResult && option.correct,
              'option-wrong': showResult && selectedOption === option.id && !option.correct,
            }"
            @click="selectOption(option.id)"
          >
            <div class="option-text">{{ option.text }}</div>
          </div>
        </div>

        <div v-if="showResult" class="result-area">
          <el-alert
            :title="resultTitle"
            :type="resultType"
            show-icon
            :closable="false"
          />
          <el-button type="primary" class="mt-4" @click="nextDecision">
            {{ isLastDecision ? '查看训练报告' : '下一题' }}
          </el-button>
        </div>
      </el-card>

      <!-- 进度条 -->
      <div class="progress-bar mt-4">
        <div class="progress-text">
          决策进度: {{ currentStep }}/{{ scenario.decisionPoints.length }}
        </div>
        <el-progress
          :percentage="(currentStep / scenario.decisionPoints.length) * 100"
          :status="showResult ? (isCorrect ? 'success' : 'exception') : ''"
        />
      </div>
    </div>
  </div>
  <div v-else class="loading-container">
    <el-icon class="is-loading" :size="40"><Loading /></el-icon>
    <p>加载中...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Warning, Timer, Loading } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()

const scenario = ref<any>({
  id: '', name: '', description: '', difficulty: 'easy', timeLimit: 60, decisionPoints: [],
})

const recordId = ref('')
const currentStep = ref(1)
const selectedOption = ref('')
const showResult = ref(false)
const isCorrect = ref(false)
const totalScore = ref(0)
const decisions = ref<{ decisionPointId: string; optionId: string; responseTime: number; score: number }[]>([])

const timeLeft = ref(30)
let timerInterval: number | null = null

const currentDecisionPoint = computed(() => {
  return scenario.value.decisionPoints[currentStep.value - 1]
})

const isLastDecision = computed(() => {
  return currentStep.value === scenario.value.decisionPoints.length
})

const resultTitle = computed(() => {
  if (isCorrect.value) {
    return `回答正确！得分: +${getSelectedScore()}`
  }
  return `回答错误，正确答案是: ${getCorrectAnswer()}`
})

const resultType = computed(() => isCorrect.value ? 'success' : 'error')

function getSelectedScore() {
  const option = currentDecisionPoint.value.options.find((o: any) => o.id === selectedOption.value)
  return option?.score || 0
}

function getCorrectAnswer() {
  const option = currentDecisionPoint.value.options.find((o: any) => o.correct)
  return option?.text || ''
}

function selectOption(optionId: string) {
  if (showResult.value) return
  selectedOption.value = optionId
  showResult.value = true

  const option = currentDecisionPoint.value.options.find((o: any) => o.id === optionId)
  isCorrect.value = option?.correct || false

  if (isCorrect.value) {
    totalScore.value += option?.score || 0
  }

  decisions.value.push({
    decisionPointId: currentDecisionPoint.value.id,
    optionId,
    responseTime: currentDecisionPoint.value.timePressure - timeLeft.value,
    score: option?.score || 0,
  })

  stopTimer()
}

async function nextDecision() {
  if (isLastDecision.value) {
    // 提交所有决策并跳转到训练报告
    try {
      for (const d of decisions.value) {
        await request.post('/training/decision', {
          recordId: recordId.value,
          decisionPointId: d.decisionPointId,
          optionId: d.optionId,
          responseTime: d.responseTime,
        })
      }
      router.push({ path: `/training/records/${recordId.value}`, query: { scenarioId: scenario.value.id } })
    } catch {
      router.push({ path: `/training/records/${recordId.value}`, query: { scenarioId: scenario.value.id } })
    }
  } else {
    currentStep.value++
    selectedOption.value = ''
    showResult.value = false
    isCorrect.value = false
    startTimer()
  }
}

function startTimer() {
  timeLeft.value = currentDecisionPoint.value.timePressure
  timerInterval = window.setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      stopTimer()
      // 时间到，自动选择错误答案
      selectOption(currentDecisionPoint.value.options.find((o: any) => !o.correct)?.id || '')
    }
  }, 1000)
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

onMounted(async () => {
  const id = route.params.id as string
  const [scenarioRes, startRes] = await Promise.all([
    request.get(`/training/scenarios/${id}`),
    request.post('/training/start', { scenarioId: id }),
  ])
  scenario.value = scenarioRes.data
  recordId.value = startRes.data.recordId
  startTimer()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.training-content {
  max-width: 800px;
  margin: 0 auto;
}

.scenario-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.scenario-header h2 {
  font-size: 20px;
  font-weight: bold;
  color: #1f2937;
}

.decision-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.decision-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
}

.timer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
  color: #3b82f6;
  padding: 8px 16px;
  background: #eff6ff;
  border-radius: 8px;
}

.timer-warning {
  color: #ef4444;
  background: #fee2e2;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.options-grid {
  display: grid;
  gap: 16px;
}

.option-item {
  padding: 20px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.option-item:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.option-selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

.option-correct {
  border-color: #10b981;
  background: #ecfdf5;
}

.option-wrong {
  border-color: #ef4444;
  background: #fee2e2;
}

.option-text {
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
}

.option-consequence {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280;
}

.result-area {
  margin-top: 24px;
  text-align: center;
}

.progress-bar {
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.progress-text {
  margin-bottom: 8px;
  font-size: 14px;
  color: #6b7280;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #6b7280;
  gap: 16px;
}
</style>
