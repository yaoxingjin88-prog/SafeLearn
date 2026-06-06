<template>
  <div class="scenario-list">
    <h2 class="text-2xl font-bold mb-6">事故推演场景</h2>

    <el-row :gutter="20">
      <el-col :span="8" v-for="scenario in scenarios" :key="scenario.id">
        <el-card class="scenario-card" shadow="hover">
          <div class="scenario-header">
            <div class="scenario-icon" :class="getDifficultyClass(scenario.difficulty)">
              <el-icon :size="32"><Warning /></el-icon>
            </div>
            <el-tag :type="getDifficultyType(scenario.difficulty)">
              {{ getDifficultyName(scenario.difficulty) }}
            </el-tag>
          </div>
          <h3 class="scenario-title">{{ scenario.name }}</h3>
          <p class="scenario-desc">{{ scenario.description }}</p>
          <div class="scenario-meta">
            <span><el-icon><Timer /></el-icon> {{ scenario.duration }}秒</span>
            <span><el-icon><Cpu /></el-icon> {{ scenario.initialConditions.batteryCount }}电池</span>
          </div>
          <el-button type="primary" class="w-full mt-4" @click="router.push(p(`/simulation/${scenario.id}`))">
            开始推演
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Warning, Timer, Cpu } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import type { AccidentScenario } from '@/types'

const router = useRouter()
const { p } = useAppBase()

const scenarios = ref<AccidentScenario[]>([])

onMounted(async () => {
  const res = await request.get('/simulation/scenarios')
  scenarios.value = res.data
})

function getDifficultyClass(difficulty: string) {
  return `difficulty-${difficulty}`
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
</script>

<style scoped>
.scenario-list {
  width: 100%;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.scenario-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.scenario-card:hover {
  transform: translateY(-4px);
}

.scenario-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.scenario-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.difficulty-easy {
  background: linear-gradient(135deg, #10b981, #059669);
}

.difficulty-medium {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.difficulty-hard {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.scenario-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #1f2937;
}

.scenario-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 16px;
  line-height: 1.5;
}

.scenario-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #9ca3af;
}

.scenario-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
