<template>
  <div class="training-list">
    <h2 class="text-2xl font-bold mb-6">应急决策训练</h2>

    <el-row :gutter="20">
      <el-col :span="8" v-for="scenario in scenarios" :key="scenario.id">
        <el-card class="training-card" :class="{ locked: scenario.unlocked === false }" shadow="hover">
          <div class="card-header">
            <el-icon :size="40" :color="scenario.unlocked === false ? '#d1d5db' : getDifficultyColor(scenario.difficulty)">
              <Lock v-if="scenario.unlocked === false" />
              <Cpu v-else />
            </el-icon>
            <el-tag :type="getDifficultyType(scenario.difficulty)">
              {{ getDifficultyName(scenario.difficulty) }}
            </el-tag>
          </div>
          <h3 class="card-title">{{ scenario.name }}</h3>
          <p class="card-desc">{{ scenario.description }}</p>
          <div v-if="scenario.unlocked === false" class="lock-notice">
            <el-icon><Lock /></el-icon> 需先完成前置章节学习
          </div>
          <div class="card-meta">
            <span><el-icon><Timer /></el-icon> {{ scenario.timeLimit }}秒</span>
            <span><el-icon><List /></el-icon> {{ scenario.decisionPoints?.length || 0 }}个决策点</span>
          </div>
          <el-button
            :type="scenario.unlocked === false ? 'info' : 'primary'"
            class="w-full mt-4"
            :disabled="scenario.unlocked === false"
            @click="handleStartScenario(scenario)"
          >
            {{ scenario.unlocked === false ? '未解锁' : '开始训练' }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 训练记录 -->
    <el-card class="mt-8">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-bold">最近训练记录</span>
          <el-button text>查看全部</el-button>
        </div>
      </template>
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="scenarioName" label="训练场景" />
        <el-table-column prop="totalScore" label="得分" width="100">
          <template #default="{ row }">
            <span :class="getScoreClass(row.totalScore)">{{ row.totalScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRatingType(row.rating)">{{ getRatingName(row.rating) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="完成时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/training/records/${row.id}`)">
              查看报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu, Timer, List, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import type { TrainingScenario } from '@/types'

const router = useRouter()

const scenarios = ref<TrainingScenario[]>([])
const records = ref([])

onMounted(async () => {
  const [scenariosRes, recordsRes] = await Promise.all([
    request.get('/training/scenarios'),
    request.get('/training/records'),
  ])
  scenarios.value = scenariosRes.data
  records.value = recordsRes.data
})

function getDifficultyColor(difficulty: string) {
  const map: Record<string, string> = {
    easy: '#10b981',
    medium: '#f59e0b',
    hard: '#ef4444',
  }
  return map[difficulty] || '#6b7280'
}

function getDifficultyType(difficulty: string) {
  const map: Record<string, string> = {
    easy: 'success',
    medium: 'warning',
    hard: 'danger',
  }
  return map[difficulty] || ''
}

function getDifficultyName(difficulty: string) {
  const map: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难',
  }
  return map[difficulty] || difficulty
}

function getScoreClass(score: number) {
  if (score >= 90) return 'text-green-500 font-bold'
  if (score >= 70) return 'text-blue-500 font-bold'
  if (score >= 60) return 'text-orange-500 font-bold'
  return 'text-red-500 font-bold'
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

function handleStartScenario(scenario: TrainingScenario) {
  if (scenario.unlocked === false) {
    ElMessage.warning('请先完成前置章节学习')
    return
  }
  router.push(`/training/${scenario.id}`)
}

</script>

<style scoped>
.training-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.training-card:hover {
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #1f2937;
}

.card-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 16px;
  line-height: 1.5;
}

.card-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #9ca3af;
}

.card-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.training-card.locked {
  opacity: 0.55;
}

.lock-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #f59e0b;
  background: #fffbeb;
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 12px;
}
</style>
