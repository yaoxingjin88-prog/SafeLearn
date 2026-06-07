<template>
  <div class="sl-page training-list">
    <div class="sl-page-head">
      <h2 class="sl-page-title">应急决策训练</h2>
    </div>

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
            :type="scenario.unlocked === false || !scenario.decisionPoints?.length ? 'info' : 'primary'"
            class="w-full mt-4"
            :disabled="scenario.unlocked === false || !scenario.decisionPoints?.length"
            @click="handleStartScenario(scenario)"
          >
            {{
              scenario.unlocked === false
                ? '未解锁'
                : !scenario.decisionPoints?.length
                  ? '暂无题目'
                  : '开始训练'
            }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu, Timer, List, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import type { TrainingScenario } from '@/types'

const router = useRouter()
const { p } = useAppBase()

const scenarios = ref<TrainingScenario[]>([])

onMounted(async () => {
  const scenariosRes = await request.get('/training/scenarios')
  scenarios.value = scenariosRes.data
})

function getDifficultyColor(difficulty: number) {
  const map: Record<number, string> = {
    1: '#10b981',
    2: '#f59e0b',
    3: '#ef4444',
  }
  return map[difficulty] || '#6b7280'
}

function getDifficultyType(difficulty: number): '' | 'success' | 'warning' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'danger'> = {
    1: 'success',
    2: 'warning',
    3: 'danger',
  }
  return map[difficulty] || ''
}

function getDifficultyName(difficulty: number) {
  const map: Record<number, string> = {
    1: '基础',
    2: '中级',
    3: '高级',
  }
  return map[difficulty] || '未知'
}

function handleStartScenario(scenario: TrainingScenario) {
  if (scenario.unlocked === false) {
    ElMessage.warning('请先完成前置章节学习')
    return
  }
  if (!scenario.decisionPoints?.length) {
    ElMessage.warning('该训练场景暂无决策题目')
    return
  }
  router.push(p(`/training/${scenario.id}`))
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
