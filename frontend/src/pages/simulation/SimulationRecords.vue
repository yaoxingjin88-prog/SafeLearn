<template>
  <div class="sl-page simulation-records">
    <div class="sl-page-head">
      <h2 class="sl-page-title">推演记录</h2>
      <p class="sl-page-desc">经典推演、时间轴推演与测试作业推演的历史记录</p>
    </div>
    <el-card v-loading="loading">
      <el-table :data="records" style="width: 100%" row-key="sessionId">
        <el-table-column label="推演场景" min-width="220" header-align="left">
          <template #default="{ row }">
            <div class="scenario-cell">
              <span>{{ row.scenarioName }}</span>
              <el-tag v-if="row.type === 'classic'" size="small" type="success" class="type-tag">经典推演</el-tag>
              <el-tag v-if="row.type === 'timeline'" size="small" type="warning" class="type-tag">时间轴</el-tag>
              <el-tag v-if="row.type === 'test'" size="small" type="info" class="type-tag">测试推演</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="结果" width="100" header-align="center" align="center">
          <template #default="{ row }">
            <el-tag :type="outcomeTagType(row)" size="small">{{ outcomeLabel(row) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="得分" width="72" header-align="center" align="center">
          <template #default="{ row }">
            <span class="score-cell">{{ row.totalScore ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="评级" width="88" header-align="center" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.rating" :type="ratingTagType(row.rating)" size="small">
              {{ ratingLabel(row.rating) }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column label="完成时间" width="168" header-align="center" align="center">
          <template #default="{ row }">
            {{ formatTime(row.finishedAt || row.startedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right" header-align="center" align="center">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button type="primary" link @click="openDebrief(row)">查看总评</el-button>
              <el-button type="primary" link @click="retry(row)">再次推演</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无推演记录，完成推演后将显示在此" />
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      :title="activeRecord?.scenarioName || '推演总评'"
      size="640px"
      direction="rtl"
      class="debrief-drawer"
      header-class="debrief-drawer-header"
      body-class="debrief-drawer-body"
      footer-class="debrief-drawer-footer"
    >
      <SimulationDebriefPanel
        v-if="activeDebrief"
        :debrief="activeDebrief"
        :scenario-name="activeRecord?.scenarioName"
      />
      <div v-else class="drawer-loading">
        <el-icon class="is-loading drawer-loading-icon" :size="32"><Loading /></el-icon>
      </div>
      <template #footer>
        <el-button class="debrief-btn-close" @click="drawerVisible = false">关闭</el-button>
        <el-button v-if="activeRecord" type="primary" @click="retry(activeRecord); drawerVisible = false">
          再次推演
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import request from '@/api/request'
import * as timelineApi from '@/api/timelineDeduction'
import * as testApi from '@/api/testSimulation'
import { deductionApi } from '@/api/deduction'
import { useAppBase } from '@/composables/useAppBase'
import SimulationDebriefPanel, { type SessionDebrief } from '@/components/simulation/SimulationDebriefPanel.vue'

interface SimulationRecord {
  sessionId: string
  type: 'classic' | 'timeline' | 'test'
  scenarioName: string
  scenarioId?: string
  scenarioCode?: string
  outcome?: string
  branch?: string
  totalScore?: number
  rating?: string
  instructorComment?: string
  dimensions?: SessionDebrief['dimensions']
  strengths?: string[]
  weaknesses?: string[]
  fiveWhy?: string[]
  recommendations?: SessionDebrief['recommendations']
  status?: string
  startedAt?: string
  finishedAt?: string
}

const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const records = ref<SimulationRecord[]>([])
const drawerVisible = ref(false)
const activeRecord = ref<SimulationRecord | null>(null)
const activeDebrief = ref<SessionDebrief | null>(null)

function ratingLabel(r: string) {
  const map: Record<string, string> = {
    excellent: '优秀', good: '良好', average: '及格', poor: '不及格',
  }
  return map[r] || r || '-'
}

function ratingTagType(r: string) {
  if (r === 'excellent' || r === 'good') return 'success'
  if (r === 'average') return 'warning'
  return 'danger'
}

function outcomeLabel(row: SimulationRecord) {
  if (row.type === 'timeline') {
    const map: Record<string, string> = {
      contained: '遏制成功', partial_loss: '部分损失', major_accident: '重大事故',
    }
    return map[row.outcome ?? ''] || row.outcome || '—'
  }
  if (row.type === 'test') {
    const map: Record<string, string> = {
      controlled: '事故受控', partial: '部分损失', catastrophic: '闪爆事故',
    }
    return map[row.outcome ?? ''] || row.outcome || '—'
  }
  return row.outcome === 'success' ? '受控' : '扩大'
}

function outcomeTagType(row: SimulationRecord) {
  if (row.type === 'timeline') {
    if (row.outcome === 'contained') return 'success'
    if (row.outcome === 'partial_loss') return 'warning'
    return 'danger'
  }
  if (row.type === 'test') {
    if (row.outcome === 'controlled') return 'success'
    if (row.outcome === 'partial') return 'warning'
    return 'danger'
  }
  return row.outcome === 'success' ? 'success' : 'danger'
}

function formatTime(t?: string) {
  if (!t) return '—'
  return t.replace('T', ' ').slice(0, 19)
}

function recordTime(r: SimulationRecord) {
  const t = r.finishedAt || r.startedAt
  return t ? new Date(t).getTime() : 0
}

function rowToDebrief(row: SimulationRecord): SessionDebrief {
  return {
    totalScore: row.totalScore,
    rating: row.rating,
    instructorComment: row.instructorComment,
    branch: row.branch,
    outcome: row.outcome,
    dimensions: row.dimensions,
    strengths: row.strengths,
    weaknesses: row.weaknesses,
    fiveWhy: row.fiveWhy,
    recommendations: row.recommendations,
  }
}

function normalizeDimensions(raw: unknown): SessionDebrief['dimensions'] {
  if (Array.isArray(raw)) {
    return raw.map((d: any) => ({
      key: d.key || '',
      label: d.label || d.name || '',
      score: d.score ?? 0,
      max: d.max ?? 100,
    }))
  }
  if (raw && typeof raw === 'object') {
    const labels: Record<string, string> = {
      timeliness: '响应时效', procedure: '处置顺序', safety: '人员安全', effectiveness: '处置效果',
    }
    return Object.entries(raw as Record<string, unknown>).map(([key, val]) => {
      const item = val as Record<string, unknown>
      return {
        key,
        label: labels[key] || key,
        score: typeof item?.score === 'number' ? item.score : Number(item?.score) || 0,
        max: 25,
      }
    })
  }
  return []
}

function mapApiDebrief(data: Record<string, unknown>): SessionDebrief {
  return {
    totalScore: data.totalScore as number,
    rating: (data.ratingCode as string) || (data.rating as string),
    ratingLabel: data.rating as string,
    instructorComment: (data.instructorComment as string) || (data.instructorSummary as string),
    branch: data.branch as string,
    outcome: data.outcome as string,
    dimensions: normalizeDimensions(data.dimensions),
    strengths: (data.strengths as string[]) || (data.highlights as string[]) || [],
    weaknesses: (data.weaknesses as string[]) || (data.improvements as string[]) || [],
    fiveWhy: data.fiveWhy as string[],
    recommendations: data.recommendations as SessionDebrief['recommendations'],
  }
}

async function fetchFullDebrief(row: SimulationRecord): Promise<SessionDebrief> {
  if (row.instructorComment && row.dimensions?.length && row.fiveWhy?.length) {
    return rowToDebrief(row)
  }
  try {
    if (row.type === 'timeline') {
      const res = await timelineApi.getSessionScore(row.sessionId)
      return mapApiDebrief(res.data as Record<string, unknown>)
    }
    if (row.type === 'test') {
      const res = await testApi.getSessionScore(row.sessionId)
      return mapApiDebrief(res.data as Record<string, unknown>)
    }
    const res = await deductionApi.getScore(row.sessionId)
    return mapApiDebrief(res.data as unknown as Record<string, unknown>)
  } catch {
    return rowToDebrief(row)
  }
}

async function openDebrief(row: SimulationRecord) {
  activeRecord.value = row
  activeDebrief.value = null
  drawerVisible.value = true
  activeDebrief.value = await fetchFullDebrief(row)
}

function retry(row: SimulationRecord) {
  if (row.type === 'timeline') {
    router.push(p(`/simulation/timeline/${row.scenarioCode || 'beijing_416'}`))
  } else if (row.type === 'test') {
    router.push(p(`/simulation/test/${row.scenarioCode || 'guangzhou_614'}`))
  } else if (row.scenarioId) {
    router.push(p(`/simulation/${row.scenarioId}`))
  }
}

onMounted(async () => {
  try {
    const [classicRes, timelineRes, testRes] = await Promise.all([
      request.get('/deduction/sessions'),
      timelineApi.listSessions().catch(() => ({ data: [] as SimulationRecord[] })),
      testApi.listSessions().catch(() => ({ data: [] as SimulationRecord[] })),
    ])

    const classic: SimulationRecord[] = (classicRes.data || [])
      .filter((r: SimulationRecord) => r.status === 'completed' && r.totalScore != null)
      .map((r: SimulationRecord) => ({
        ...r,
        type: 'classic' as const,
        dimensions: normalizeDimensions(r.dimensions),
      }))

    const timeline: SimulationRecord[] = ((timelineRes.data || []) as SimulationRecord[])
      .filter(r => r.totalScore != null)
      .map(r => ({
        ...r,
        type: 'timeline' as const,
        scenarioName: r.scenarioName || '北京丰台 4·16 储能电站事故推演',
        dimensions: normalizeDimensions(r.dimensions),
      }))

    const test: SimulationRecord[] = ((testRes.data || []) as SimulationRecord[])
      .filter(r => r.totalScore != null)
      .map(r => ({
        ...r,
        type: 'test' as const,
        scenarioName: r.scenarioName || '广州智光储能科技「6·14」锂电池包闪爆事故推演',
        dimensions: normalizeDimensions(r.dimensions),
      }))

    records.value = [...classic, ...timeline, ...test].sort((a, b) => recordTime(b) - recordTime(a))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.sl-page-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: #64748b;
}

.scenario-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.type-tag { flex-shrink: 0; }

.score-cell {
  font-weight: 700;
  font-size: 16px;
  color: #1e3a5f;
  font-family: Consolas, monospace;
}

.action-cell {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 2px 4px;
}

.drawer-loading {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>

<style>
.debrief-drawer.el-drawer {
  --el-drawer-bg-color: #030712;
  --el-drawer-padding-primary: 20px;
  background: #030712 !important;
}

.debrief-drawer-header {
  margin-bottom: 0 !important;
  padding: 18px 20px 14px !important;
  background: #030712 !important;
  border-bottom: 1px solid #1e293b !important;
}

.debrief-drawer-header .el-drawer__title {
  color: #f8fafc !important;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.45;
  padding-right: 28px;
}

.debrief-drawer-header .el-drawer__close-btn {
  color: #94a3b8 !important;
}

.debrief-drawer-header .el-drawer__close-btn:hover {
  color: #f1f5f9 !important;
}

.debrief-drawer-body {
  background: #030712 !important;
  padding: 16px 20px 20px !important;
}

.debrief-drawer-footer {
  background: #030712 !important;
  border-top: 1px solid #1e293b !important;
  padding: 14px 20px 18px !important;
}

.debrief-drawer-footer .debrief-btn-close {
  background: transparent;
  border-color: #334155;
  color: #e2e8f0;
}

.debrief-drawer-footer .debrief-btn-close:hover {
  background: #1e293b;
  border-color: #475569;
  color: #f8fafc;
}

.debrief-drawer-body .drawer-loading-icon {
  color: #64748b;
}
</style>
