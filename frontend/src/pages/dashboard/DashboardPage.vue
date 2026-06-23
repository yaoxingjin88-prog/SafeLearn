<template>
  <div class="admin-dashboard" v-loading="loading">
    <el-alert
      v-if="usingFallback"
      class="fallback-banner"
      type="warning"
      show-icon
      :closable="false"
      title="当前展示演示数据，请确认后端已启动并重新加载"
    />

    <template v-if="dashboard">
      <section class="stats-grid" aria-label="培训统计概览">
        <article v-for="item in statCards" :key="item.key" class="stat-card">
          <span class="stat-icon" :class="`tone-${item.tone}`">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <div class="stat-content">
            <span class="stat-label">{{ item.label }}</span>
            <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
            <span class="stat-note" :class="item.noteTone">{{ item.note }}</span>
          </div>
        </article>
      </section>

      <section id="analytics" class="dashboard-grid dashboard-grid--top">
        <article class="panel trend-panel">
          <header class="panel-header">
            <div>
              <h2>培训完成情况</h2>
              <p>完成人数与完成率趋势</p>
            </div>
            <div class="panel-actions">
              <el-select v-model="trendRange" size="small" class="panel-select">
                <el-option label="近7天" value="7d" />
                <el-option label="近30天" value="30d" />
              </el-select>
              <el-select v-model="trendGranularity" size="small" class="panel-select">
                <el-option label="按日" value="day" />
                <el-option label="按周" value="week" />
              </el-select>
            </div>
          </header>
          <VChart class="trend-chart" :option="trendOption" autoresize />
        </article>

        <article class="panel category-panel">
          <header class="panel-header">
            <div>
              <h2>课程分类完成率</h2>
              <p>按已发布课程分类统计</p>
            </div>
            <el-select
              v-model="selectedDepartment"
              size="small"
              class="dept-select"
              popper-class="dashboard-dept-popper"
            >
              <el-option label="全部部门" value="all" />
              <el-option
                v-for="dept in departmentOptions"
                :key="dept"
                :label="dept"
                :value="dept"
              />
            </el-select>
          </header>
          <div class="donut-wrap">
            <VChart class="category-chart" :option="categoryOption" autoresize />
            <div class="donut-total">
              <span>总体完成率</span>
              <strong>{{ categoryOverview.completionRate }}%</strong>
              <small>已完成 {{ categoryOverview.completedTraining }}</small>
            </div>
          </div>
        </article>

        <aside class="right-stack">
          <article class="panel quick-panel">
            <header class="panel-header panel-header--small"><h2>快捷入口</h2></header>
            <div class="quick-grid">
              <button v-for="item in quickActions" :key="item.label" type="button" @click="handleQuick(item.target)">
                <span :class="`quick-icon tone-${item.tone}`"><el-icon><component :is="item.icon" /></el-icon></span>
                {{ item.label }}
              </button>
            </div>
          </article>

          <article class="panel notice-panel">
            <header class="panel-header panel-header--small">
              <h2>公告通知</h2><span class="more-link">更多 <el-icon><ArrowRight /></el-icon></span>
            </header>
            <ul class="notice-list">
              <li v-for="notice in dashboard.announcements" :key="`${notice.title}-${notice.date}`">
                <span class="notice-title">{{ notice.title }}</span>
                <time>{{ notice.date.slice(5) }}</time>
                <em v-if="notice.pinned">置顶</em>
              </li>
            </ul>
          </article>
        </aside>
      </section>

      <section class="dashboard-grid dashboard-grid--bottom">
        <article id="plans" class="panel plans-panel">
          <header class="panel-header">
            <div><h2>培训计划执行情况</h2><p>已发布课程的人员覆盖与完成进度</p></div>
            <button class="plain-link" type="button" @click="$router.push('/admin/learning/courses')">全部计划</button>
          </header>
          <div class="plan-table-wrap">
            <table class="plan-table">
              <thead>
                <tr>
                  <th>计划名称</th>
                  <th>所属部门</th>
                  <th>学习进度</th>
                  <th>完成率</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="plan in dashboard.trainingPlans" :key="plan.id">
                  <td><strong>{{ plan.name }}</strong></td>
                  <td>{{ plan.department }}</td>
                  <td>
                    <div class="plan-progress">
                      <span><i :style="{ width: `${Math.min(100, plan.target ? plan.learned * 100 / plan.target : 0)}%` }" /></span>
                      <small>{{ plan.learned }}/{{ plan.target }}</small>
                    </div>
                  </td>
                  <td>{{ plan.rate }}%</td>
                  <td><span class="status-pill" :class="`status-${plan.status}`">{{ statusLabel(plan.status) }}</span></td>
                  <td><button type="button" class="table-action" @click="$router.push('/admin/learning/courses')">查看</button></td>
                </tr>
              </tbody>
            </table>
            <el-empty v-if="!dashboard.trainingPlans.length" description="暂无培训计划" :image-size="54" />
          </div>
        </article>

        <article id="alerts" class="panel alerts-panel">
          <header class="panel-header">
            <div><h2>最新安全预警</h2><p>基于考试、培训进度与证书状态</p></div>
            <span class="more-link">更多 <el-icon><ArrowRight /></el-icon></span>
          </header>
          <div class="alert-list">
            <div v-for="alert in dashboard.alerts" :key="`${alert.title}-${alert.time}`" class="alert-item">
              <span class="alert-icon" :class="`level-${alert.level}`"><el-icon><WarningFilled /></el-icon></span>
              <div class="alert-body">
                <strong>{{ alert.title }}</strong>
                <p>{{ alert.description }}</p>
                <time>{{ alert.time }}</time>
              </div>
              <em class="alert-tag" :class="`level-${alert.level}`">{{ alertLevel(alert.level) }}</em>
            </div>
          </div>
        </article>

        <article class="panel calendar-panel">
          <header class="panel-header">
            <div><h2>培训日历</h2><p>课程发布、学习与训练记录</p></div>
          </header>
          <div class="calendar-toolbar">
            <button type="button" title="上个月" @click="shiftMonth(-1)"><el-icon><ArrowLeft /></el-icon></button>
            <strong>{{ calendarTitle }}</strong>
            <button type="button" title="下个月" @click="shiftMonth(1)"><el-icon><ArrowRight /></el-icon></button>
          </div>
          <div class="calendar-week"><span v-for="day in weekDays" :key="day">{{ day }}</span></div>
          <div class="calendar-days">
            <span v-for="cell in calendarCells" :key="cell.date" :class="{ muted: !cell.current, today: cell.today }">
              {{ cell.day }}<i v-if="cell.event" :class="`event-${cell.event}`" />
            </span>
          </div>
          <div class="calendar-legend">
            <span><i class="event-planned" />有计划</span>
            <span><i class="event-in_progress" />进行中</span>
            <span><i class="event-completed" />已完成</span>
          </div>
        </article>
      </section>

      <footer class="dashboard-footer">
        © {{ new Date().getFullYear() }} 储能安全培训管理系统 v1.0.0 · 数据统计口径以实时业务记录为准
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import {
  ArrowLeft, ArrowRight, BellFilled, Calendar, CircleCheckFilled,
  DataAnalysis, Document, DocumentAdd, Medal, Reading, School, UploadFilled,
  WarningFilled,
} from '@element-plus/icons-vue'
import { adminApi, type AdminDashboardData } from '@/api/admin'
import { createFallbackDashboard } from './dashboardFallback'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent])

const router = useRouter()
const loading = ref(false)
const usingFallback = ref(false)
const dashboard = ref<AdminDashboardData | null>(null)
const calendarDate = ref(new Date())
const trendRange = ref('7d')
const trendGranularity = ref('day')
const selectedDepartment = ref('all')
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const departmentOptions = computed(() => dashboard.value?.departments ?? [])

const visibleCategoryCompletion = computed(() => {
  if (!dashboard.value) return []
  if (selectedDepartment.value === 'all') return dashboard.value.categoryCompletion
  return dashboard.value.categoryCompletionByDepartment?.[selectedDepartment.value]
    ?? dashboard.value.categoryCompletion
})

const categoryOverview = computed(() => {
  if (!dashboard.value) return { completionRate: 0, completedTraining: 0 }
  if (selectedDepartment.value === 'all') {
    return {
      completionRate: dashboard.value.stats.completionRate,
      completedTraining: dashboard.value.stats.completedTraining,
    }
  }
  const overview = dashboard.value.departmentOverview?.[selectedDepartment.value]
  return overview ?? {
    completionRate: dashboard.value.stats.completionRate,
    completedTraining: dashboard.value.stats.completedTraining,
  }
})

function formatDelta(value: number | undefined, unit = '', percent = false) {
  const v = value ?? 0
  if (v === 0) return '较昨日 —'
  const arrow = v > 0 ? '▲' : '▼'
  const abs = Math.abs(v)
  const suffix = percent ? '%' : unit
  return `较昨日 ${arrow} ${abs}${suffix}`
}

const statCards = computed(() => {
  if (!dashboard.value) return []
  const stats = dashboard.value.stats
  return [
    {
      key: 'trainee',
      label: '应训人数',
      value: stats.traineeCount.toLocaleString(),
      unit: '人',
      note: formatDelta(stats.traineeDelta, '人'),
      noteTone: (stats.traineeDelta ?? 0) >= 0 ? 'positive' : 'negative',
      tone: 'blue',
      icon: School,
    },
    {
      key: 'completed',
      label: '已完成培训',
      value: stats.completedTraining.toLocaleString(),
      unit: '人',
      note: `完成率 ${stats.completionRate}%`,
      noteTone: 'positive',
      tone: 'green',
      icon: CircleCheckFilled,
    },
    {
      key: 'incomplete',
      label: '未完成培训',
      value: stats.incompleteTraining.toLocaleString(),
      unit: '人',
      note: formatDelta(stats.incompleteDelta, '人'),
      noteTone: (stats.incompleteDelta ?? 0) <= 0 ? 'positive' : 'negative',
      tone: 'red',
      icon: Document,
    },
    {
      key: 'pass',
      label: '考试通过率',
      value: stats.examPassRate,
      unit: '%',
      note: formatDelta(stats.passRateDelta, '', true),
      noteTone: (stats.passRateDelta ?? 0) >= 0 ? 'positive' : 'negative',
      tone: 'purple',
      icon: Medal,
    },
    {
      key: 'alerts',
      label: '安全预警',
      value: stats.safetyAlerts,
      unit: '项',
      note: formatDelta(stats.alertsDelta, '项'),
      noteTone: (stats.alertsDelta ?? 0) <= 0 ? 'positive' : 'negative',
      tone: 'orange',
      icon: WarningFilled,
    },
  ]
})

const trendOption = computed<EChartsOption>(() => {
  const trend = dashboard.value?.completionTrend
  return {
    color: ['#5b96f7', '#34b75b'],
    tooltip: { trigger: 'axis' },
    legend: { top: 4, left: 8, itemWidth: 14, itemHeight: 4, textStyle: { color: '#6b7687', fontSize: 11 } },
    grid: { left: 48, right: 48, top: 42, bottom: 32 },
    xAxis: {
      type: 'category',
      data: trend?.labels ?? [],
      axisLine: { lineStyle: { color: '#e8edf4' } },
      axisTick: { show: false },
      axisLabel: { color: '#8791a1', fontSize: 11 },
    },
    yAxis: [
      { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#8791a1', fontSize: 10 } },
      { type: 'value', min: 0, max: 100, splitLine: { show: false }, axisLabel: { color: '#8791a1', fontSize: 10, formatter: '{value}%' } },
    ],
    series: [
      { name: '完成人数', type: 'bar', data: trend?.completedUsers ?? [], barWidth: 22, itemStyle: { borderRadius: [4, 4, 0, 0], color: '#5b96f7' } },
      { name: '完成率', type: 'line', yAxisIndex: 1, data: trend?.completionRates ?? [], smooth: true, symbolSize: 7, lineStyle: { width: 2, color: '#34b75b' }, itemStyle: { color: '#34b75b' } },
    ],
  }
})

const categoryOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
  legend: {
    orient: 'vertical',
    right: 8,
    top: 'center',
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 12,
    textStyle: { color: '#586578', fontSize: 11 },
    formatter: (name: string) => {
      const item = visibleCategoryCompletion.value.find(entry => entry.name === name)
      return `${name}  ${item?.rate ?? 0}%`
    },
  },
  series: [{
    type: 'pie',
    center: ['34%', '52%'],
    radius: ['48%', '68%'],
    label: { show: false },
    itemStyle: { borderColor: '#fff', borderWidth: 2 },
    data: visibleCategoryCompletion.value.map(item => ({
      name: item.name,
      value: item.rate,
      itemStyle: { color: item.color },
    })),
  }],
}))

const quickActions = [
  { label: '创建培训计划', icon: Calendar, target: '/admin/learning/courses', tone: 'blue' },
  { label: '添加课程', icon: Reading, target: '/admin/learning/courses', tone: 'green' },
  { label: '新建考试', icon: DocumentAdd, target: '/admin/learning/exams', tone: 'purple' },
  { label: '导入用户', icon: UploadFilled, target: '/admin/users', tone: 'orange' },
  { label: '学习报表', icon: DataAnalysis, target: '#plans', tone: 'cyan' },
  { label: '预警管理', icon: BellFilled, target: '#alerts', tone: 'red' },
]

const calendarTitle = computed(() => `${calendarDate.value.getFullYear()}年${calendarDate.value.getMonth() + 1}月`)
const eventMap = computed(() => new Map((dashboard.value?.calendarEvents ?? []).map(item => [item.date, item.type])))
const calendarCells = computed(() => {
  const year = calendarDate.value.getFullYear()
  const month = calendarDate.value.getMonth()
  const first = new Date(year, month, 1)
  const start = new Date(year, month, 1 - first.getDay())
  const today = new Date()
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    return {
      date: key,
      day: date.getDate(),
      current: date.getMonth() === month,
      today: date.toDateString() === today.toDateString(),
      event: eventMap.value.get(key),
    }
  })
})

async function loadDashboard() {
  loading.value = true
  usingFallback.value = false
  selectedDepartment.value = 'all'
  try {
    const response = await adminApi.getDashboard()
    dashboard.value = response.data
  } catch {
    dashboard.value = createFallbackDashboard()
    usingFallback.value = true
  } finally {
    loading.value = false
  }
}

function statusLabel(status: AdminDashboardData['trainingPlans'][number]['status']) {
  return { completed: '已完成', in_progress: '进行中', not_started: '未开始' }[status]
}

function alertLevel(level: AdminDashboardData['alerts'][number]['level']) {
  return { danger: '高危', warning: '中危', info: '提醒' }[level]
}

function handleQuick(target: string) {
  if (target.startsWith('#')) {
    document.querySelector(target)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  router.push(target)
}

function shiftMonth(offset: number) {
  calendarDate.value = new Date(calendarDate.value.getFullYear(), calendarDate.value.getMonth() + offset, 1)
}

onMounted(loadDashboard)
</script>

<style scoped>
.admin-dashboard {
  min-height: 100%;
  padding: 20px 24px 16px;
  box-sizing: border-box;
  color: #243044;
  background: #f5f7fc;
}

.fallback-banner {
  margin-bottom: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.stat-card {
  min-width: 0;
  height: 108px;
  padding: 18px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #e7ebf1;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(34, 54, 86, 0.04);
}

.stat-icon {
  width: 50px;
  height: 50px;
  flex: 0 0 50px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 24px;
}

.stat-icon.tone-blue { color: #3478ef; background: #edf3ff; }
.stat-icon.tone-green { color: #20ad82; background: #eaf8f3; }
.stat-icon.tone-red { color: #ec4c54; background: #fff0f1; }
.stat-icon.tone-purple { color: #8255ea; background: #f2edff; }
.stat-icon.tone-orange { color: #ef5b51; background: #fff0ed; }

.stat-content { min-width: 0; }
.stat-label { display: block; margin-bottom: 6px; color: #7c8695; font-size: 12px; }
.stat-content strong {
  display: block;
  color: #1c2738;
  font-size: 24px;
  line-height: 1.1;
  font-weight: 700;
  white-space: nowrap;
}
.stat-content strong small { margin-left: 3px; color: #7f8998; font-size: 12px; font-weight: 500; }
.stat-note {
  display: block;
  margin-top: 8px;
  color: #98a1ae;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-note.positive { color: #20ad82; }
.stat-note.negative { color: #ef5960; }

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

.dashboard-grid--top { margin-bottom: 14px; }

.panel {
  min-width: 0;
  background: #fff;
  border: 1px solid #e7ebf1;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(34, 54, 86, 0.04);
  overflow: hidden;
}

.trend-panel { grid-column: span 5; height: 340px; }
.category-panel { grid-column: span 4; height: 340px; position: relative; }
.category-panel .panel-header { position: relative; z-index: 2; }
.right-stack {
  grid-column: span 3;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  min-width: 0;
  height: 340px;
}

.right-stack .quick-panel {
  overflow: visible;
}

.right-stack .notice-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  min-height: 56px;
  padding: 14px 16px 10px;
  box-sizing: border-box;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-header h2 { margin: 0; color: #263246; font-size: 15px; font-weight: 700; }
.panel-header p { margin: 4px 0 0; color: #9ba4b0; font-size: 11px; }
.panel-header--small { min-height: 44px; padding-bottom: 6px; align-items: center; }
.panel-actions { display: flex; gap: 8px; }
.panel-select { width: 88px; }
.panel-filter,
.more-link {
  color: #8d97a6;
  font-size: 11px;
  white-space: nowrap;
}
.dept-select {
  width: 108px;
}
.dept-select :deep(.el-select__wrapper) {
  min-height: 28px;
  padding: 0 8px;
  font-size: 11px;
  box-shadow: none;
  background: #f8fafc;
}
.panel-filter,
.more-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.more-link { cursor: pointer; }
.plain-link {
  padding: 0;
  border: 0;
  background: none;
  color: #3b7ff0;
  font-size: 12px;
  cursor: pointer;
}

.trend-chart { height: 278px; }
.donut-wrap { height: 278px; position: relative; }
.category-chart { height: 100%; }
.donut-total {
  position: absolute;
  left: 34%;
  top: 52%;
  width: 96px;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}
.donut-total span,
.donut-total small { display: block; color: #8994a3; font-size: 11px; }
.donut-total strong { display: block; margin: 4px 0; color: #263247; font-size: 22px; font-weight: 700; }

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 6px 12px 12px;
  gap: 10px 4px;
}

.quick-grid button {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #606c7d;
  font-size: 11px;
  cursor: pointer;
}

.quick-icon {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 16px;
}

.quick-icon.tone-blue { background: #eef3ff; color: #4b7ff2; }
.quick-icon.tone-green { background: #eaf8f3; color: #20ad82; }
.quick-icon.tone-purple { background: #f2edff; color: #8255ea; }
.quick-icon.tone-orange { background: #fff5ea; color: #f38b3b; }
.quick-icon.tone-cyan { background: #e8f8ff; color: #06b6d4; }
.quick-icon.tone-red { background: #fff0f1; color: #ef4c55; }
.quick-grid button:hover .quick-icon { filter: brightness(0.95); transform: scale(1.05); transition: transform 0.15s; }

.notice-panel { min-height: 0; }
.notice-list {
  margin: 0;
  padding: 0 14px 10px;
  list-style: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.notice-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  min-height: 24px;
  font-size: 11px;
  padding: 4px 0;
}
.notice-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #485568;
}
.notice-list time { color: #a1a9b4; white-space: nowrap; }
.notice-list em {
  padding: 1px 6px;
  border-radius: 3px;
  background: #ffe9e9;
  color: #ef5d62;
  font-style: normal;
  font-size: 10px;
}

.plans-panel { grid-column: span 5; min-height: 340px; }
.alerts-panel { grid-column: span 4; min-height: 340px; }
.calendar-panel { grid-column: span 3; min-height: 340px; }

.plan-table-wrap { overflow-x: auto; }
.plan-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.plan-table th {
  padding: 10px 14px;
  background: #fafbfc;
  color: #8b95a4;
  text-align: left;
  font-weight: 500;
  white-space: nowrap;
}
.plan-table td {
  padding: 12px 14px;
  border-top: 1px solid #edf0f4;
  color: #667183;
  white-space: nowrap;
}
.plan-table td strong {
  display: block;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #3b4658;
  font-weight: 600;
}

.plan-progress { display: flex; align-items: center; gap: 8px; }
.plan-progress > span {
  width: 72px;
  height: 6px;
  border-radius: 99px;
  overflow: hidden;
  background: #edf1f6;
}
.plan-progress i { display: block; height: 100%; border-radius: inherit; background: #4b87f2; }
.plan-progress small { color: #738096; font-size: 11px; }

.status-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}
.status-in_progress { color: #19ad7d; background: #eaf8f3; }
.status-completed { color: #377ce8; background: #edf3ff; }
.status-not_started { color: #a0a7b2; background: #f3f4f6; }
.table-action { border: 0; padding: 0; color: #3478ef; background: none; font-size: 12px; cursor: pointer; }

.alert-list { padding: 0 14px 12px; }
.alert-item {
  min-height: 64px;
  padding: 10px 0;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
  box-sizing: border-box;
  border-top: 1px solid #edf0f4;
}
.alert-item:first-child { border-top: 0; }
.alert-icon {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 16px;
}
.level-danger { color: #ef4c55; background: #fff0f1; }
.level-warning { color: #f38b3b; background: #fff5ea; }
.level-info { color: #3d80ed; background: #eef4ff; }
.alert-body strong { display: block; color: #364153; font-size: 12px; font-weight: 600; }
.alert-body p { margin: 3px 0; color: #778293; font-size: 11px; line-height: 1.4; }
.alert-body time { color: #a1a9b4; font-size: 10px; }
.alert-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-style: normal;
  font-size: 10px;
  white-space: nowrap;
}
.alert-tag.level-danger { color: #ef4c55; background: #fff0f1; }
.alert-tag.level-warning { color: #f38b3b; background: #fff5ea; }
.alert-tag.level-info { color: #3d80ed; background: #eef4ff; }

.calendar-toolbar {
  padding: 0 16px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.calendar-toolbar strong { color: #5b6677; font-size: 12px; }
.calendar-toolbar button {
  width: 24px;
  height: 24px;
  border: 0;
  display: grid;
  place-items: center;
  color: #8994a3;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
}
.calendar-toolbar button:hover { background: #f3f4f6; }

.calendar-week,
.calendar-days {
  padding: 0 14px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
}
.calendar-week span { padding-bottom: 8px; color: #737e8e; font-size: 11px; }
.calendar-days span {
  position: relative;
  height: 30px;
  display: grid;
  place-items: center;
  color: #394557;
  font-size: 11px;
}
.calendar-days span.muted { color: #c0c6cf; }
.calendar-days span.today {
  width: 26px;
  height: 26px;
  margin: 2px auto;
  border-radius: 50%;
  color: #fff;
  background: #3478ef;
}
.calendar-days i {
  position: absolute;
  bottom: 2px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
}
.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 14px;
  padding: 8px 0 4px;
  color: #7f8997;
  font-size: 10px;
}
.calendar-legend span { display: flex; align-items: center; gap: 5px; }
.calendar-legend i { width: 6px; height: 6px; border-radius: 50%; }
.event-planned { background: #3478ef; }
.event-in_progress { background: #20ad82; }
.event-completed { background: #aeb6c2; }

.dashboard-footer {
  padding: 16px 0 4px;
  text-align: center;
  color: #a0a8b4;
  font-size: 11px;
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .trend-panel, .category-panel { grid-column: span 6; }
  .right-stack { grid-column: span 12; grid-template-columns: 1fr 1fr; grid-template-rows: none; height: auto; }
  .plans-panel { grid-column: span 7; }
  .alerts-panel { grid-column: span 5; }
  .calendar-panel { grid-column: span 12; min-height: 300px; }
}

@media (max-width: 820px) {
  .admin-dashboard { padding: 14px; }
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .trend-panel, .category-panel, .plans-panel, .alerts-panel, .calendar-panel { grid-column: span 12; }
  .right-stack { grid-template-columns: 1fr; }
  .plan-table { min-width: 620px; }
}

@media (max-width: 520px) {
  .stats-grid { grid-template-columns: 1fr; }
  .stat-card { height: 92px; }
  .trend-panel, .category-panel { height: 300px; }
  .right-stack { height: auto; }
}
</style>
