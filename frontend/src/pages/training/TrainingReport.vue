<template>
  <div class="sl-page training-report">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">训练复盘报告</span>
      </template>
    </el-page-header>

    <el-card class="mt-6">
      <div class="report-header">
        <div class="score-circle" :class="getScoreClass(record.totalScore)">
          <div class="score-value">{{ record.totalScore }}</div>
          <div class="score-label">总分</div>
          <div v-if="record.maxScore" class="score-max">/ {{ record.maxScore }}</div>
        </div>
        <div class="report-info">
          <h2>{{ record.scenarioName }}</h2>
          <el-tag :type="getRatingType(record.rating)" size="large">
            {{ getRatingName(record.rating) }}
          </el-tag>
          <p class="summary">{{ record.instructorSummary || record.feedback }}</p>
          <p class="meta">完成时间：{{ formatDateTime(record.completedAt || record.endTime) }}</p>
        </div>
      </div>
    </el-card>

    <el-card v-if="debriefItems.length" class="mt-4">
      <template #header><span class="font-bold">处置复盘</span></template>
      <div v-for="item in debriefItems" :key="item.step" class="debrief-item">
        <div class="debrief-head">
          <span class="step">第 {{ item.step }} 步</span>
          <span v-if="item.timelinePhase" class="phase">{{ item.timelinePhase }}</span>
          <el-tag :type="item.correct ? 'success' : 'danger'" size="small">
            {{ item.correct ? '正确' : '错误' }}
          </el-tag>
        </div>
        <p class="question">{{ item.question }}</p>
        <div class="answer-row">
          <span class="label">您的选择</span>
          <span :class="item.correct ? 'ok' : 'bad'">{{ item.selectedAnswer || '—' }}</span>
        </div>
        <div v-if="!item.correct" class="answer-row">
          <span class="label">正确处置</span>
          <span class="ok">{{ item.correctAnswer || '—' }}</span>
        </div>
        <p v-if="item.regulationRef" class="reg">依据：{{ item.regulationRef }}</p>
        <p v-if="item.explanation" class="explain">{{ item.explanation }}</p>
      </div>
    </el-card>

    <el-row v-if="highlights.length || improvements.length" :gutter="16" class="mt-4">
      <el-col v-if="highlights.length" :xs="24" :md="12">
        <el-card>
          <template #header><span class="font-bold">处置亮点</span></template>
          <ul class="bullet-list success">
            <li v-for="(h, i) in highlights" :key="i">{{ h }}</li>
          </ul>
        </el-card>
      </el-col>
      <el-col v-if="improvements.length" :xs="24" :md="12">
        <el-card>
          <template #header><span class="font-bold">改进建议</span></template>
          <ul class="bullet-list warn">
            <li v-for="(h, i) in improvements" :key="i">{{ h }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="keyNodeReview.length" class="mt-4">
      <template #header><span class="font-bold">关键节点处置要点</span></template>
      <ol class="numbered-list">
        <li v-for="(item, i) in keyNodeReview" :key="i">{{ item }}</li>
      </ol>
    </el-card>

    <el-row v-if="typicalMistakes.length || correctFlow.length || wrongFlow.length" :gutter="16" class="mt-4">
      <el-col v-if="typicalMistakes.length" :xs="24" :md="12">
        <el-card>
          <template #header><span class="font-bold">典型处置误区</span></template>
          <ul class="bullet-list warn">
            <li v-for="(m, i) in typicalMistakes" :key="i">{{ m }}</li>
          </ul>
        </el-card>
      </el-col>
      <el-col v-if="correctFlow.length || wrongFlow.length" :xs="24" :md="12">
        <el-card>
          <template #header><span class="font-bold">处置流程对比</span></template>
          <div v-if="correctFlow.length" class="flow-block">
            <h4 class="flow-title ok">正确流程</h4>
            <ol class="numbered-list compact">
              <li v-for="(step, i) in correctFlow" :key="'c' + i">{{ step }}</li>
            </ol>
          </div>
          <div v-if="wrongFlow.length" class="flow-block">
            <h4 class="flow-title bad">错误流程</h4>
            <ol class="numbered-list compact">
              <li v-for="(step, i) in wrongFlow" :key="'w' + i">{{ step }}</li>
            </ol>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="knowledgePoints.length" class="mt-4">
      <template #header><span class="font-bold">知识点强化</span></template>
      <div v-for="(kp, i) in knowledgePoints" :key="i" class="knowledge-item">
        <h4 class="kp-title">{{ kp.title }}</h4>
        <p class="kp-content">{{ kp.content }}</p>
        <p v-if="kp.applicableScene" class="kp-meta"><span class="kp-label">适用场景</span>{{ kp.applicableScene }}</p>
        <p v-if="kp.operationPoints" class="kp-meta"><span class="kp-label">操作要点</span>{{ kp.operationPoints }}</p>
      </div>
    </el-card>

    <el-card v-else-if="record.feedback && !debriefItems.length" class="mt-4">
      <template #header><span class="font-bold">评价与建议</span></template>
      <p class="plain-feedback">{{ record.feedback }}</p>
    </el-card>

    <div class="action-buttons mt-6">
      <el-button type="primary" size="large" @click="router.push(p('/training/scenarios'))">
        返回训练列表
      </el-button>
      <el-button size="large" @click="handleRetry">重新训练</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

const record = ref<any>({
  id: '', scenarioId: '', scenarioName: '', completedAt: '', totalScore: 0, maxScore: 0,
  rating: 'good', feedback: '', instructorSummary: '', decisions: [],
  debrief: [], highlights: [], improvements: [],
  keyNodeReview: [], typicalMistakes: [], correctFlow: [], wrongFlow: [], knowledgePoints: [],
})

const debriefItems = computed(() => {
  if (Array.isArray(record.value.debrief) && record.value.debrief.length) {
    return record.value.debrief
  }
  return (record.value.decisions || []).map((d: any, i: number) => ({
    step: i + 1,
    timelinePhase: d.timelinePhase,
    question: d.question || d.decisionPointId,
    correct: d.correct || d.isCorrect,
    selectedAnswer: d.selectedAnswer || d.optionId,
    correctAnswer: d.correctAnswer,
    explanation: d.explanation,
    regulationRef: d.regulationRef,
  }))
})

const highlights = computed(() => normalizeList(record.value.highlights))
const improvements = computed(() => normalizeList(record.value.improvements))
const keyNodeReview = computed(() => normalizeList(record.value.keyNodeReview))
const typicalMistakes = computed(() => normalizeList(record.value.typicalMistakes))
const correctFlow = computed(() => normalizeList(record.value.correctFlow))
const wrongFlow = computed(() => normalizeList(record.value.wrongFlow))
const knowledgePoints = computed(() => {
  const v = record.value.knowledgePoints
  return Array.isArray(v) ? v : []
})

function normalizeList(v: unknown): string[] {
  if (Array.isArray(v)) return v.map(String)
  return []
}

function formatDateTime(t?: string) {
  if (!t) return '—'
  const normalized = t.replace('T', ' ').replace(/\.\d+$/, '')
  return normalized.length >= 19 ? normalized.slice(0, 19) : normalized
}

function getScoreClass(score: number) {
  if (score >= 90) return 'score-excellent'
  if (score >= 70) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-poor'
}

function getRatingType(rating: string) {
  const map: Record<string, string> = { excellent: 'success', good: '', average: 'warning', poor: 'danger' }
  return map[rating] || ''
}

function getRatingName(rating: string) {
  const map: Record<string, string> = { excellent: '优秀', good: '良好', average: '及格', poor: '不及格' }
  return map[rating] || rating
}

function handleRetry() {
  const scenarioId = record.value.scenarioId || (route.query.scenarioId as string) || ''
  if (scenarioId) router.push(p(`/training/${scenarioId}`))
  else router.push(p('/training/scenarios'))
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
  flex-shrink: 0;
}

.score-excellent { background: linear-gradient(135deg, #10b981, #059669); }
.score-good { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.score-average { background: linear-gradient(135deg, #f59e0b, #d97706); }
.score-poor { background: linear-gradient(135deg, #ef4444, #dc2626); }

.score-value { font-size: 36px; font-weight: bold; line-height: 1; }
.score-label { font-size: 14px; opacity: 0.9; margin-top: 4px; }
.score-max { font-size: 12px; opacity: 0.85; }

.report-info h2 {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #1f2937;
}

.summary {
  margin: 12px 0 8px;
  font-size: 15px;
  line-height: 1.7;
  color: #475569;
}

.meta { margin: 0; font-size: 13px; color: #94a3b8; }

.debrief-item {
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
}
.debrief-item:last-child { border-bottom: none; padding-bottom: 0; }

.debrief-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.step { font-weight: 700; color: #1e40af; }
.phase { font-size: 12px; color: #64748b; font-family: Consolas, monospace; }

.question {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.5;
}

.answer-row {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 14px;
  line-height: 1.5;
}
.label { flex-shrink: 0; color: #64748b; min-width: 72px; }
.ok { color: #059669; }
.bad { color: #dc2626; }

.reg { margin: 8px 0 0; font-size: 13px; color: #7c3aed; }
.explain { margin: 8px 0 0; font-size: 13px; line-height: 1.7; color: #64748b; }

.bullet-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
  font-size: 14px;
  color: #475569;
}
.bullet-list.success li::marker { color: #10b981; }
.bullet-list.warn li::marker { color: #f59e0b; }

.numbered-list {
  margin: 0;
  padding-left: 22px;
  line-height: 1.85;
  font-size: 14px;
  color: #475569;
}
.numbered-list.compact { line-height: 1.65; }

.flow-block + .flow-block { margin-top: 16px; padding-top: 16px; border-top: 1px dashed #e5e7eb; }
.flow-title { margin: 0 0 8px; font-size: 14px; font-weight: 700; }
.flow-title.ok { color: #059669; }
.flow-title.bad { color: #dc2626; }

.knowledge-item {
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
}
.knowledge-item:last-child { border-bottom: none; padding-bottom: 0; }
.kp-title { margin: 0 0 8px; font-size: 16px; font-weight: 700; color: #1e40af; }
.kp-content { margin: 0 0 10px; font-size: 14px; line-height: 1.75; color: #475569; }
.kp-meta { margin: 6px 0 0; font-size: 13px; line-height: 1.65; color: #64748b; }
.kp-label {
  display: inline-block;
  min-width: 64px;
  margin-right: 8px;
  font-weight: 600;
  color: #334155;
}

.plain-feedback {
  margin: 0;
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
