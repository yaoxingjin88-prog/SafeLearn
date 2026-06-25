<template>
  <div class="learning-report-page" v-loading="loading">
    <nav class="page-breadcrumb" aria-label="面包屑">
      <button type="button" @click="router.push('/dashboard')">首页</button>
      <span>/</span>
      <button type="button" @click="router.push('/admin/learning/courses')">培训课程</button>
      <span>/</span>
      <strong>学习报表</strong>
    </nav>

    <header class="page-header">
      <div>
        <h1>学习报表</h1>
        <p>查看学员学习进度、课程完成情况、学习时长与考试达标情况</p>
      </div>
    </header>

    <section v-if="data" class="stats-grid">
      <article v-for="card in summaryCards" :key="card.key" class="stat-card">
        <span class="stat-icon" :class="`tone-${card.tone}`">
          <el-icon><component :is="card.icon" /></el-icon>
        </span>
        <div class="stat-content">
          <span class="stat-label">{{ card.label }}</span>
          <strong>{{ card.value }}<small v-if="card.unit">{{ card.unit }}</small></strong>
          <em :class="card.subTone">{{ card.subText }}</em>
        </div>
      </article>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <div class="filter-field filter-field--keyword">
          <span class="filter-label">关键词</span>
          <el-input v-model="filters.keyword" placeholder="搜索学员/课程/部门" clearable @keyup.enter="search" />
        </div>
        <div class="filter-field">
          <span class="filter-label">所属部门</span>
          <el-select v-model="filters.department" class="w-full">
            <el-option label="全部部门" value="all" />
            <el-option v-for="dept in data?.departments || []" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">课程分类</span>
          <el-select v-model="filters.category" class="w-full">
            <el-option
              v-for="item in data?.categories || []"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">学习状态</span>
          <el-select v-model="filters.learningStatus" class="w-full">
            <el-option v-for="item in LEARNING_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field filter-field--date">
          <span class="filter-label">统计时间</span>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="w-full"
          />
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button @click="exportReport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </div>
      </div>
    </section>

    <section v-if="data" class="charts-grid">
      <article class="panel chart-panel">
        <header class="panel-header">
          <div>
            <h2>学习趋势分析</h2>
            <p>学习人数与完成课程数趋势</p>
          </div>
          <el-select v-model="trendDays" size="small" class="trend-select" @change="search">
            <el-option label="近7天" :value="7" />
            <el-option label="近14天" :value="14" />
            <el-option label="近30天" :value="30" />
          </el-select>
        </header>
        <div class="chart-wrap">
          <VChart class="chart-box" :option="trendOption" autoresize />
          <div v-if="!hasTrendData" class="chart-empty">暂无趋势数据</div>
        </div>
      </article>

      <article class="panel chart-panel">
        <header class="panel-header">
          <div>
            <h2>课程完成率分布</h2>
            <p>按已发布课程统计</p>
          </div>
        </header>
        <div class="donut-wrap">
          <VChart class="chart-box" :option="courseDonutOption" autoresize />
          <div class="donut-center">
            <span>总体完成率</span>
            <strong>{{ data.charts.courseCompletion.overallRate }}%</strong>
          </div>
        </div>
      </article>

      <article class="panel chart-panel chart-panel--compact">
        <header class="panel-header">
          <div>
            <h2>部门学习完成率</h2>
            <p>各部门学员完成情况</p>
          </div>
        </header>
        <div class="chart-wrap">
          <VChart class="chart-box chart-box--compact" :option="departmentBarOption" autoresize />
          <div v-if="!hasDepartmentData" class="chart-empty">暂无部门数据</div>
        </div>
      </article>

      <article class="panel chart-panel chart-panel--compact">
        <header class="panel-header">
          <div>
            <h2>考试成绩分布</h2>
            <p>按分数区间统计</p>
          </div>
        </header>
        <div class="chart-wrap">
          <VChart class="chart-box chart-box--compact" :option="examBarOption" autoresize />
          <div v-if="!hasExamData" class="chart-empty">暂无考试数据</div>
        </div>
      </article>
    </section>

    <article v-if="data" class="panel table-panel">
      <header class="table-header">
        <div>
          <h2>待跟进学员</h2>
          <p>共 {{ data.followUp.total }} 人需关注</p>
        </div>
        <button type="button" class="plain-link" @click="exportFollowUp">导出明细</button>
      </header>

      <div class="table-wrap">
        <table class="report-table">
          <thead>
            <tr>
              <th>学员姓名</th>
              <th>所属部门</th>
              <th>必修课程</th>
              <th>完成进度</th>
              <th>学习时长</th>
              <th>最近考试</th>
              <th>预警状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in data.followUp.items" :key="`${row.userId}-${row.courseId}`">
              <td><strong>{{ row.username }}</strong></td>
              <td>{{ row.department }}</td>
              <td class="course-cell">{{ row.courseTitle }}</td>
              <td>
                <div class="progress-cell">
                  <span class="progress-track">
                    <i :class="{ done: row.progress >= 100 }" :style="{ width: `${Math.max(row.progress, row.progress > 0 ? 4 : 0)}%` }" />
                  </span>
                  <small>{{ row.progress }}%</small>
                </div>
              </td>
              <td>{{ row.studyDuration }}</td>
              <td>{{ row.examLabel }}</td>
              <td>
                <span class="warn-tag" :class="`warn-${row.warningStatus}`">
                  {{ warningLabel(row.warningStatus) }}
                </span>
              </td>
              <td class="action-cell">
                <button type="button" @click="goMonitoring(row)">查看</button>
                <button
                  type="button"
                  :disabled="remindingKey === `${row.userId}-${row.courseId}`"
                  @click="remindLearner(row)"
                >
                  {{ remindingKey === `${row.userId}-${row.courseId}` ? '发送中…' : '提醒' }}
                </button>
                <button type="button" @click="goUserDetail(row)">详情</button>
              </td>
            </tr>
          </tbody>
        </table>
        <el-empty v-if="!loading && !data.followUp.items.length" description="暂无待跟进学员" :image-size="64" />
      </div>

      <div v-if="data.followUp.total > 0" class="pagination-bar">
        <span>共 {{ data.followUp.total }} 条</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="data.followUp.total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          @current-change="loadData"
          @size-change="onPageSizeChange"
        />
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import type { EChartsOption } from 'echarts'
import {
  CircleCheckFilled,
  Clock,
  Download,
  Reading,
  UserFilled,
} from '@element-plus/icons-vue'
import {
  adminApi,
  type AdminLearningReportData,
  type AdminLearningReportFollowUpItem,
} from '@/api/admin'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent])

const LEARNING_STATUS_OPTIONS = [
  { label: '全部状态', value: 'all' },
  { label: '未开始', value: 'not_started' },
  { label: '学习中', value: 'in_progress' },
  { label: '已完成', value: 'completed' },
] as const

const WARNING_LABELS: Record<string, string> = {
  exam_fail: '考试未达标',
  incomplete: '课程未完成',
  low_progress: '进度滞后',
  retake: '补考中',
  not_started: '待复训',
  none: '正常',
}

const router = useRouter()
const loading = ref(false)
const remindingKey = ref<string | null>(null)
const data = ref<AdminLearningReportData | null>(null)
const page = ref(1)
const pageSize = ref(10)
const trendDays = ref(7)

function defaultDateRange(): [string, string] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  const format = (date: Date) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  return [format(start), format(end)]
}

const filters = reactive({
  keyword: '',
  department: 'all',
  category: 'all',
  learningStatus: 'all' as typeof LEARNING_STATUS_OPTIONS[number]['value'],
  dateRange: defaultDateRange() as [string, string],
})

const summaryCards = computed(() => {
  const summary = data.value?.summary
  if (!summary) return []
  const delta = summary.traineeDelta
  return [
    {
      key: 'trainee',
      label: '学员总数',
      value: summary.traineeCount.toLocaleString(),
      unit: '',
      subText: `较上月 ${delta >= 0 ? '+' : ''}${delta}`,
      subTone: delta >= 0 ? 'positive' : 'negative',
      tone: 'blue',
      icon: UserFilled,
    },
    {
      key: 'completion',
      label: '课程完成率',
      value: summary.completionRate,
      unit: '%',
      subText: `已完成 ${summary.completedCount} 人`,
      subTone: 'muted',
      tone: 'green',
      icon: Reading,
    },
    {
      key: 'hours',
      label: '平均学习时长',
      value: summary.avgStudyHours,
      unit: 'h',
      subText: '人均本月',
      subTone: 'muted',
      tone: 'orange',
      icon: Clock,
    },
    {
      key: 'exam',
      label: '考试通过率',
      value: summary.examPassRate,
      unit: '%',
      subText: `通过 ${summary.passedCount} 人`,
      subTone: 'muted',
      tone: 'purple',
      icon: CircleCheckFilled,
    },
  ]
})

const hasTrendData = computed(() => {
  const trend = data.value?.charts.learningTrend
  if (!trend) return false
  return (trend.learners ?? []).some(v => v > 0) || (trend.completedCourses ?? []).some(v => v > 0)
})

const hasDepartmentData = computed(() =>
  (data.value?.charts.departmentCompletion ?? []).some(item => item.rate > 0),
)

const hasExamData = computed(() =>
  (data.value?.charts.examScoreDistribution ?? []).some(item => item.value > 0),
)

const trendOption = computed<EChartsOption>(() => {
  const trend = data.value?.charts.learningTrend
  return {
    color: ['#5b96f7', '#34b75b'],
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 8, itemWidth: 14, itemHeight: 4, textStyle: { color: '#6b7687', fontSize: 11 } },
    grid: { left: 44, right: 16, top: 36, bottom: 24 },
    xAxis: {
      type: 'category',
      data: trend?.labels ?? [],
      axisLine: { lineStyle: { color: '#e8edf4' } },
      axisTick: { show: false },
      axisLabel: { color: '#8791a1', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef2f7' } },
      axisLabel: { color: '#8791a1', fontSize: 10 },
    },
    series: [
      {
        name: '学习人数',
        type: 'line',
        smooth: true,
        data: trend?.learners ?? [],
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        areaStyle: { color: 'rgba(91, 150, 247, 0.08)' },
      },
      {
        name: '完成课程数',
        type: 'line',
        smooth: true,
        data: trend?.completedCourses ?? [],
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        areaStyle: { color: 'rgba(52, 183, 91, 0.08)' },
      },
    ],
  }
})

const courseDonutOption = computed<EChartsOption>(() => {
  const items = data.value?.charts.courseCompletion.items ?? []
  const hasRate = items.some(item => item.rate > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: {
      orient: 'vertical',
      right: 12,
      top: 'middle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 10,
      textStyle: { color: '#586578', fontSize: 11 },
      formatter: (name: string) => {
        const item = items.find(entry => entry.name === name)
        const label = name.length > 8 ? `${name.slice(0, 8)}…` : name
        return `${label}  ${item?.rate ?? 0}%`
      },
    },
    series: [{
      type: 'pie',
      center: ['36%', '54%'],
      radius: ['52%', '72%'],
      label: { show: false },
      emphasis: { scale: true, scaleSize: 4 },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      data: hasRate
        ? items.map(item => ({
          name: item.name,
          value: Math.max(item.rate, 0.01),
          itemStyle: { color: item.color },
        }))
        : [{ name: '暂无数据', value: 1, itemStyle: { color: '#edf1f6' } }],
    }],
  }
})

const departmentBarOption = computed<EChartsOption>(() => {
  const items = [...(data.value?.charts.departmentCompletion ?? [])].reverse()
  return {
    tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
    grid: { left: 84, right: 52, top: 8, bottom: 8 },
    xAxis: {
      type: 'value',
      max: 100,
      splitLine: { lineStyle: { color: '#eef2f7', type: 'dashed' } },
      axisLabel: { color: '#8791a1', fontSize: 10, formatter: '{value}%' },
    },
    yAxis: {
      type: 'category',
      data: items.map(item => item.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#586578', fontSize: 11 },
    },
    series: [{
      type: 'bar',
      data: items.map(item => ({
        value: item.rate,
        itemStyle: {
          color: item.rate > 0 ? '#5b96f7' : '#e8edf4',
          borderRadius: [0, 4, 4, 0],
        },
      })),
      barWidth: 12,
      label: { show: true, position: 'right', formatter: '{c}%', color: '#8791a1', fontSize: 10 },
    }],
  }
})

const examBarOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 12, top: 12, bottom: 24 },
  xAxis: {
    type: 'category',
    data: (data.value?.charts.examScoreDistribution ?? []).map(item => item.name),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#e8edf4' } },
    axisLabel: { color: '#8791a1', fontSize: 10 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#eef2f7', type: 'dashed' } },
    axisLabel: { color: '#8791a1', fontSize: 10 },
  },
  series: [{
    type: 'bar',
    data: (data.value?.charts.examScoreDistribution ?? []).map(item => ({
      value: item.value,
      itemStyle: { color: item.color, borderRadius: [4, 4, 0, 0] },
    })),
    barWidth: 32,
    label: {
      show: true,
      position: 'top',
      color: '#8791a1',
      fontSize: 10,
      formatter: (params: { value: number }) => (params.value > 0 ? String(params.value) : ''),
    },
  }],
}))

function warningLabel(status: string) {
  return WARNING_LABELS[status] || status
}

function buildQuery() {
  return {
    keyword: filters.keyword.trim() || undefined,
    department: filters.department === 'all' ? undefined : filters.department,
    category: filters.category === 'all' ? undefined : filters.category,
    learningStatus: filters.learningStatus === 'all' ? undefined : filters.learningStatus,
    from: filters.dateRange?.[0],
    to: filters.dateRange?.[1],
    trendDays: trendDays.value,
    page: page.value,
    pageSize: pageSize.value,
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.getLearningReport(buildQuery())
    data.value = res.data
  } catch {
    ElMessage.error('加载学习报表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadData()
}

function resetFilters() {
  filters.keyword = ''
  filters.department = 'all'
  filters.category = 'all'
  filters.learningStatus = 'all'
  filters.dateRange = defaultDateRange()
  trendDays.value = 7
  search()
}

function onPageSizeChange() {
  page.value = 1
  loadData()
}

function goMonitoring(row: AdminLearningReportFollowUpItem) {
  router.push({ path: '/admin/learning/monitoring', query: { courseId: row.courseId } })
}

function goUserDetail(row: AdminLearningReportFollowUpItem) {
  router.push(`/admin/users/${row.userId}`)
}

function remindLearner(row: AdminLearningReportFollowUpItem) {
  const key = `${row.userId}-${row.courseId}`
  remindingKey.value = key
  adminApi.sendLearningReminder({
    userId: row.userId,
    courseId: row.courseId,
    warningStatus: row.warningStatus,
    progress: row.progress,
  })
    .then(res => {
      if (res.data.duplicate) {
        ElMessage.warning(res.data.message || '今日已提醒过该学员')
        return
      }
      ElMessage.success(res.data.message || `已向 ${row.username} 发送学习提醒`)
    })
    .catch(() => {
      ElMessage.error('发送提醒失败')
    })
    .finally(() => {
      if (remindingKey.value === key) {
        remindingKey.value = null
      }
    })
}

function csvCell(value: string | number | undefined | null) {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

function exportRows(rows: AdminLearningReportFollowUpItem[], filename: string) {
  if (!rows.length) {
    ElMessage.warning('暂无可导出数据')
    return
  }
  const header = ['学员姓名', '所属部门', '必修课程', '完成进度', '学习时长', '最近考试', '预警状态']
  const lines = rows.map(row => [
    csvCell(row.username),
    csvCell(row.department),
    csvCell(row.courseTitle),
    csvCell(`${row.progress}%`),
    csvCell(row.studyDuration),
    csvCell(row.examLabel),
    csvCell(warningLabel(row.warningStatus)),
  ].join(','))
  const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

async function exportReport() {
  try {
    const res = await adminApi.getLearningReport({
      ...buildQuery(),
      page: 1,
      pageSize: 10000,
    })
    exportRows(res.data.followUp.items, `学习报表-${filters.dateRange?.[0]}_${filters.dateRange?.[1]}.csv`)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

function exportFollowUp() {
  exportRows(data.value?.followUp.items ?? [], '待跟进学员.csv')
}

onMounted(loadData)
</script>

<style scoped>
.learning-report-page {
  min-height: 100%;
  padding: 20px 24px 16px;
  box-sizing: border-box;
  color: #243044;
  background: #f5f7fc;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #8b95a4;
  font-size: 13px;
}

.page-breadcrumb button {
  border: none;
  background: none;
  color: #8b95a4;
  cursor: pointer;
  padding: 0;
}

.page-breadcrumb strong {
  color: #243044;
}

.page-header {
  margin-bottom: 14px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.page-header p {
  margin: 6px 0 0;
  color: #7c8695;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  min-width: 0;
  height: 108px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
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
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 24px;
}

.tone-blue { color: #3478ef; background: #edf3ff; }
.tone-green { color: #20ad82; background: #eaf8f3; }
.tone-orange { color: #f38b3b; background: #fff5ea; }
.tone-purple { color: #8255ea; background: #f2edff; }

.stat-content strong {
  display: block;
  font-size: 24px;
  line-height: 1.1;
  color: #1c2738;
}

.stat-content strong small {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 500;
  color: #7f8998;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #8b95a4;
  margin-bottom: 4px;
}

.stat-content em {
  display: block;
  margin-top: 6px;
  font-style: normal;
  font-size: 11px;
}

.stat-content em.positive { color: #20ad82; }
.stat-content em.negative { color: #ef5960; }
.stat-content em.muted { color: #98a1ae; }

.filter-panel,
.panel {
  background: #fff;
  border: 1px solid #e7ebf1;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(34, 54, 86, 0.04);
  overflow: hidden;
}

.filter-panel {
  padding: 14px 16px;
  margin-bottom: 14px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 120px;
  flex: 1;
}

.filter-field--keyword {
  flex: 1.4;
  min-width: 180px;
}

.filter-field--date {
  flex: 1.2;
  min-width: 240px;
}

.filter-label {
  font-size: 12px;
  color: #8b95a4;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}

.w-full { width: 100%; }

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
  align-items: stretch;
}

.chart-panel {
  min-width: 0;
  height: 320px;
  display: flex;
  flex-direction: column;
}

.chart-panel--compact {
  height: 280px;
}

.panel-header {
  min-height: 56px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px 8px;
  box-sizing: border-box;
  flex-shrink: 0;
}

.panel-header h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #263246;
}

.panel-header p {
  margin: 4px 0 0;
  font-size: 11px;
  color: #9ba4b0;
}

.trend-select {
  width: 88px;
}

.trend-select :deep(.el-select__wrapper) {
  min-height: 28px;
  font-size: 11px;
  box-shadow: none;
  background: #f8fafc;
}

.chart-wrap,
.donut-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
}

.chart-box {
  height: 100%;
  min-height: 220px;
}

.chart-box--compact {
  min-height: 190px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #b0b8c4;
  font-size: 12px;
  pointer-events: none;
}

.donut-center {
  position: absolute;
  left: 36%;
  top: 54%;
  width: 96px;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}

.donut-center span {
  display: block;
  color: #8994a3;
  font-size: 11px;
}

.donut-center strong {
  display: block;
  margin-top: 4px;
  font-size: 22px;
  font-weight: 700;
  color: #263247;
}

.table-panel {
  overflow: hidden;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 16px 0;
}

.table-header h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #263246;
}

.table-header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8b95a4;
}

.plain-link {
  border: none;
  background: none;
  color: #3478ef;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.table-wrap {
  overflow-x: auto;
  padding: 0 16px;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.report-table th,
.report-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #f0f3f8;
  text-align: left;
  white-space: nowrap;
}

.report-table th {
  color: #8b95a4;
  font-weight: 500;
  font-size: 12px;
  background: #fafbfc;
}

.report-table tbody tr:hover {
  background: #fafcff;
}

.course-cell {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-track {
  width: 72px;
  height: 6px;
  border-radius: 99px;
  background: #edf1f6;
  overflow: hidden;
}

.progress-track i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #4b87f2, #6ba0ff);
  border-radius: inherit;
  transition: width 0.2s ease;
}

.progress-track i.done {
  background: linear-gradient(90deg, #20ad82, #3ec99a);
}

.progress-cell small {
  color: #738096;
  font-size: 11px;
}

.warn-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.warn-exam_fail { color: #ef4c55; background: #fff0f1; }
.warn-incomplete { color: #f38b3b; background: #fff5ea; }
.warn-low_progress { color: #eab308; background: #fefce8; }
.warn-retake { color: #3478ef; background: #edf3ff; }
.warn-not_started { color: #06b6d4; background: #e8f8ff; }

.action-cell {
  display: flex;
  gap: 10px;
}

.action-cell button {
  border: none;
  background: none;
  color: #3478ef;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
}

.action-cell button:disabled {
  color: #b0b8c4;
  cursor: not-allowed;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 16px;
  font-size: 12px;
  color: #8b95a4;
}

@media (max-width: 1100px) {
  .stats-grid,
  .charts-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}

@media (max-width: 720px) {
  .learning-report-page {
    padding: 14px;
  }

  .stats-grid,
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-panel,
  .chart-panel--compact {
    height: auto;
    min-height: 280px;
  }
}
</style>
