<template>
  <div class="sl-page training-list">
    <div class="sl-page-head">
      <h2 class="sl-page-title">应急决策训练</h2>
      <p class="sl-page-desc">基于真实事故案例的分级决策训练 · 按时间线推进 · 即时评分与专业解析</p>
    </div>

    <section v-if="emergencyScenarios.length" class="featured-section">
      <div class="section-label">
        <el-tag type="danger" effect="dark" size="small">案例驱动</el-tag>
        <span>特别重大事故 · 专项应急决策训练</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="12" v-for="scenario in emergencyScenarios" :key="scenario.id">
          <el-card
            class="training-card case-card"
            :class="sceneClass(scenario)"
            shadow="hover"
            @click="handleStartScenario(scenario)"
          >
            <div class="card-accent" :class="sceneClass(scenario)" />
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="36" :color="getDifficultyColor(scenario.difficulty)"><Cpu /></el-icon>
                <div>
                  <el-tag v-if="getSceneLabel(scenario)" size="small" :type="getSceneTagType(scenario)" effect="dark">
                    {{ getSceneLabel(scenario) }}
                  </el-tag>
                  <el-tag type="danger" size="small" class="ml-1">特别严重</el-tag>
                </div>
              </div>
              <el-tag :type="getDifficultyType(scenario.difficulty)">
                {{ getDifficultyLabel(scenario) }}
              </el-tag>
            </div>
            <h3 class="card-title">{{ scenario.name }}</h3>
            <p class="card-desc">{{ scenario.description }}</p>
            <div class="card-meta">
              <span><el-icon><Timer /></el-icon> {{ scenario.timeLimit }}秒</span>
              <span><el-icon><List /></el-icon> {{ scenario.decisionPoints?.length || 0 }} 个决策点</span>
              <span v-if="getAccidentDate(scenario)"><el-icon><Calendar /></el-icon> {{ getAccidentDate(scenario) }}</span>
            </div>
            <el-button type="primary" class="w-full mt-4" @click.stop="handleStartScenario(scenario)">
              开始训练
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <section v-if="gradedScenarios.length" class="featured-section">
      <div class="section-label">
        <el-tag type="primary" effect="dark" size="small">分级训练</el-tag>
        <span>真实事故案例 · L1→L3 递进 · 时间线决策 · 复盘强化</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="8" v-for="scenario in gradedScenarios" :key="scenario.id">
          <el-card
            class="training-card case-card"
            :class="tierClass(scenario)"
            shadow="hover"
            @click="handleStartScenario(scenario)"
          >
            <div class="card-accent" :class="tierClass(scenario)" />
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="36" :color="getDifficultyColor(scenario.difficulty)"><Cpu /></el-icon>
                <div>
                  <el-tag v-if="getTierLabel(scenario)" size="small" type="info" effect="dark">
                    {{ getTierLabel(scenario) }}
                  </el-tag>
                </div>
              </div>
              <el-tag :type="getDifficultyType(scenario.difficulty)">
                {{ getDifficultyLabel(scenario) }}
              </el-tag>
            </div>
            <h3 class="card-title">{{ scenario.name }}</h3>
            <p class="card-desc">{{ scenario.description }}</p>
            <div class="card-meta">
              <span><el-icon><Timer /></el-icon> {{ scenario.timeLimit }}秒</span>
              <span><el-icon><List /></el-icon> {{ scenario.decisionPoints?.length || 0 }} 个决策点</span>
              <span v-if="getAccidentDate(scenario)"><el-icon><Calendar /></el-icon> {{ getAccidentDate(scenario) }}</span>
            </div>
            <el-button
              type="primary"
              class="w-full mt-4"
              :disabled="scenario.unlocked === false || !scenario.decisionPoints?.length"
              @click.stop="handleStartScenario(scenario)"
            >
              {{ scenario.unlocked === false ? '未解锁' : '开始训练' }}
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <el-empty v-if="!loading && !gradedScenarios.length && !emergencyScenarios.length" description="暂无训练场景" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu, Timer, List, Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import type { TrainingScenario } from '@/types'

const router = useRouter()
const { p } = useAppBase()

const scenarios = ref<TrainingScenario[]>([])
const loading = ref(true)

function isTestScenario(s: TrainingScenario) {
  const name = (s.name || '').trim().toLowerCase()
  return name === 'test' || name.startsWith('test_') || name.startsWith('test ')
}

const emergencyScenarios = computed(() =>
  scenarios.value.filter(s =>
    !isTestScenario(s) && (s as any).initialConditions?.trainingKind === 'emergency_case',
  ),
)

const gradedScenarios = computed(() =>
  scenarios.value
    .filter(s => !isTestScenario(s) && (s as any).initialConditions?.trainingKind === 'graded_case')
    .sort((a, b) => (a.difficulty ?? 0) - (b.difficulty ?? 0)),
)

function getSceneLabel(s: TrainingScenario) {
  return (s as any).initialConditions?.sceneLabel || ''
}

function sceneClass(s: TrainingScenario) {
  const t = (s as any).initialConditions?.sceneType
  if (t === 'test_operation') return 'scene-test'
  if (t === 'station_operation') return 'scene-station'
  return 'scene-station'
}

function getSceneTagType(s: TrainingScenario): 'warning' | 'primary' | 'info' {
  const t = (s as any).initialConditions?.sceneType
  if (t === 'test_operation') return 'warning'
  return 'primary'
}

function getTierLabel(s: TrainingScenario) {
  return (s as any).initialConditions?.tierLabel || ''
}

function tierClass(s: TrainingScenario) {
  const tier = (s as any).initialConditions?.tier
  if (tier === 'L1') return 'tier-l1'
  if (tier === 'L2') return 'tier-l2'
  if (tier === 'L3') return 'tier-l3'
  return ''
}

onMounted(async () => {
  loading.value = true
  try {
    const scenariosRes = await request.get('/training/scenarios')
    scenarios.value = scenariosRes.data
  } finally {
    loading.value = false
  }
})

function getAccidentDate(s: TrainingScenario) {
  return (s as any).initialConditions?.accidentDate || ''
}

function getDifficultyColor(difficulty: number) {
  const map: Record<number, string> = { 1: '#10b981', 2: '#f59e0b', 3: '#ef4444' }
  return map[difficulty] || '#6b7280'
}

function getDifficultyType(difficulty: number): '' | 'success' | 'warning' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'danger'> = {
    1: 'success', 2: 'warning', 3: 'danger',
  }
  return map[difficulty] || ''
}

function getDifficultyLabel(s: TrainingScenario) {
  const label = (s as any).difficultyLabel
  if (label && label !== '未知') return label
  return getDifficultyName(s.difficulty ?? 0)
}

function getDifficultyName(difficulty: number) {
  const map: Record<number, string> = { 0: '基础', 1: '基础', 2: '中级', 3: '高级' }
  return map[difficulty] || '高级'
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
.sl-page-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: #64748b;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

.featured-section {
  margin-bottom: 32px;
}

.training-card {
  margin-bottom: 20px;
  transition: transform 0.28s ease, box-shadow 0.28s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.training-card:hover {
  transform: translateY(-4px);
}

.case-card.scene-station { border-color: rgba(37, 99, 235, 0.25); }
.case-card.scene-test { border-color: rgba(234, 88, 12, 0.25); }

.card-accent.scene-station { background: linear-gradient(90deg, #1d4ed8, #3b82f6); }
.card-accent.scene-test { background: linear-gradient(90deg, #c2410c, #f97316); }

.case-card.tier-l1 { border-color: rgba(16, 185, 129, 0.35); }
.case-card.tier-l2 { border-color: rgba(245, 158, 11, 0.35); }
.case-card.tier-l3 { border-color: rgba(239, 68, 68, 0.35); }

.card-accent.tier-l1 { background: linear-gradient(90deg, #059669, #10b981); }
.card-accent.tier-l2 { background: linear-gradient(90deg, #d97706, #f59e0b); }
.card-accent.tier-l3 { background: linear-gradient(90deg, #dc2626, #ef4444); }

.card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
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
  flex-wrap: wrap;
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
