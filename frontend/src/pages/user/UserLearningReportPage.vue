<template>
  <div class="sl-page learning-report" v-loading="loading">
    <div class="sl-page-head">
      <h2 class="sl-page-title">个人学习报告</h2>
      <p class="sl-page-desc">汇总课程、推演、训练与能力分析数据</p>
    </div>

    <section class="stats-row">
      <el-card v-for="item in statCards" :key="item.label" class="stat-card" shadow="never">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}<span class="stat-unit">{{ item.unit }}</span></div>
      </el-card>
    </section>

    <section class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <span>近 7 天学习趋势</span>
            <span class="muted">累计 {{ studyTrend.totalHours }} 小时</span>
          </div>
        </template>
        <VChart class="chart" :option="trendOption" autoresize />
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <span>能力雷达</span>
            <span class="muted">综合 {{ abilityRadar.overallScore }} 分</span>
          </div>
        </template>
        <VChart class="chart" :option="radarOption" autoresize />
      </el-card>
    </section>

    <section class="bottom-row">
      <el-card shadow="never" class="recent-card">
        <template #header>
          <span>最近学习课程</span>
        </template>
        <el-table :data="recentCourses" empty-text="暂无记录">
          <el-table-column prop="title" label="课程" min-width="200" />
          <el-table-column label="进度" width="160">
            <template #default="{ row }">
              <el-progress :percentage="row.progress || 0" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="primary" link @click="router.push(p(`/courses/${row.id}`))">继续</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" class="alert-card">
        <template #header>
          <div class="card-head">
            <span>培训预警订阅</span>
            <el-tag v-if="alerts.length" type="danger" size="small">{{ alerts.length }} 条</el-tag>
          </div>
        </template>
        <div v-if="alerts.length" class="alert-list">
          <div v-for="alert in alerts" :key="alert.id" class="alert-item">
            <div class="alert-title">
              <el-tag :type="alertLevelType(alert.level)" size="small">{{ alert.level }}</el-tag>
              {{ alert.title }}
            </div>
            <p v-if="alert.description" class="alert-desc">{{ alert.description }}</p>
            <el-button
              v-if="alert.actionPath"
              type="primary"
              link
              @click="router.push(normalizeActionPath(alert.actionPath))"
            >
              查看详情
            </el-button>
          </div>
        </div>
        <el-empty v-else description="暂无与您相关的培训预警" :image-size="72" />
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, RadarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, RadarComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { dashboardApi, type StudyTrend, type AbilityRadar } from '@/api/dashboard'
import { useAppBase } from '@/composables/useAppBase'

use([BarChart, RadarChart, GridComponent, TooltipComponent, RadarComponent, CanvasRenderer])

const router = useRouter()
const { p } = useAppBase()
const loading = ref(true)

const stats = ref({
  courseCount: 0,
  completedCount: 0,
  simulationCount: 0,
  trainingCount: 0,
  avgScore: 0,
})
const studyTrend = ref<StudyTrend>({ days: 7, labels: [], hours: [], totalHours: 0 })
const abilityRadar = ref<AbilityRadar>({ dimensions: [], hasDeductionData: false, overallScore: 0 })
const recentCourses = ref<Array<{ id: string; title: string; progress: number }>>([])
const alerts = ref<Array<{
  id: string
  level: string
  title: string
  description?: string
  actionPath?: string
}>>([])

const statCards = computed(() => [
  { label: '已发布课程', value: stats.value.courseCount, unit: '门' },
  { label: '完成章节', value: stats.value.completedCount, unit: '节' },
  { label: '推演次数', value: stats.value.simulationCount, unit: '次' },
  { label: '训练次数', value: stats.value.trainingCount, unit: '次' },
  { label: '平均得分', value: stats.value.avgScore, unit: '分' },
])

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 24, bottom: 28 },
  xAxis: { type: 'category', data: studyTrend.value.labels },
  yAxis: { type: 'value', name: '小时' },
  series: [{ type: 'bar', data: studyTrend.value.hours, itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] } }],
}))

const radarOption = computed(() => ({
  tooltip: {},
  radar: {
    indicator: abilityRadar.value.dimensions.map(d => ({ name: d.name, max: 100 })),
    radius: '62%',
  },
  series: [{
    type: 'radar',
    data: [{ value: abilityRadar.value.dimensions.map(d => d.value), name: '能力值' }],
    areaStyle: { opacity: 0.15 },
    lineStyle: { color: '#2563eb' },
    itemStyle: { color: '#2563eb' },
  }],
}))

function alertLevelType(level: string) {
  if (level === '高' || level === 'high') return 'danger'
  if (level === '中' || level === 'medium') return 'warning'
  return 'info'
}

function normalizeActionPath(path: string) {
  if (path.startsWith('/user')) return path
  if (path.startsWith('/')) return `/user${path}`
  return p(`/${path}`)
}

onMounted(async () => {
  try {
    const [statsRes, trendRes, radarRes, recentRes, alertsRes] = await Promise.all([
      dashboardApi.getStats(),
      dashboardApi.getStudyTrend(7),
      dashboardApi.getAbilityRadar(),
      dashboardApi.getRecentCourses(),
      dashboardApi.getTrainingAlerts(),
    ])
    stats.value = statsRes.data
    studyTrend.value = trendRes.data
    abilityRadar.value = radarRes.data
    recentCourses.value = recentRes.data || []
    alerts.value = alertsRes.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card :deep(.el-card__body) {
  padding: 16px;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  margin-left: 4px;
  color: #6b7280;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.chart {
  height: 280px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.muted {
  font-size: 13px;
  color: #6b7280;
}

.bottom-row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  padding: 12px;
  border-radius: 8px;
  background: #f9fafb;
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 6px;
}

.alert-desc {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .charts-row,
  .bottom-row {
    grid-template-columns: 1fr;
  }
}
</style>
