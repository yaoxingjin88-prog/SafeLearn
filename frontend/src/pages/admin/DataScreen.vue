<template>
  <div class="sl-page data-screen">
    <div class="sl-page-head">
      <h2 class="sl-page-title">数据大屏</h2>
    </div>

    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-blue-500">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-green-500">
            <el-icon :size="32"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCourses }}</div>
            <div class="stat-label">培训课程</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-orange-500">
            <el-icon :size="32"><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalSimulations }}</div>
            <div class="stat-label">推演次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon bg-purple-500">
            <el-icon :size="32"><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgScore }}</div>
            <div class="stat-label">平均分数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-6">
      <!-- 培训完成率 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="font-bold">培训完成率</span>
          </template>
          <div ref="completionChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 事故类型分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="font-bold">事故类型分布</span>
          </template>
          <div ref="accidentChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-6">
      <!-- 月度趋势 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span class="font-bold">月度培训趋势</span>
          </template>
          <div ref="trendChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 风险等级分布 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="font-bold">风险等级分布</span>
          </template>
          <div ref="riskChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { User, Reading, Warning, Trophy } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const completionChart = ref<HTMLElement>()
const accidentChart = ref<HTMLElement>()
const trendChart = ref<HTMLElement>()
const riskChart = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

const stats = reactive({
  totalUsers: 0,
  totalCourses: 0,
  totalSimulations: 0,
  avgScore: 0,
})

const chartData = ref<{
  completion: { name: string; value: number; color: string }[]
  accidentTypes: { name: string; value: number; color: string }[]
  monthlyTrend: { months: string[]; trainingUsers: number[]; simulationCounts: number[] }
  riskLevels: { name: string; value: number; color: string }[]
} | null>(null)

onMounted(async () => {
  const [statsRes, chartsRes] = await Promise.all([
    request.get('/admin/stats'),
    request.get('/admin/charts'),
  ])
  Object.assign(stats, statsRes.data)
  chartData.value = chartsRes.data
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  charts.forEach(chart => chart.dispose())
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  charts.forEach(chart => chart.resize())
}

function toPieData(items: { name: string; value: number; color: string }[]) {
  return items.map(item => ({
    name: item.name,
    value: item.value,
    itemStyle: { color: item.color },
  }))
}

function initCharts() {
  if (!chartData.value) return

  if (completionChart.value) {
    const chart = echarts.init(completionChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
        data: toPieData(chartData.value.completion),
      }],
    })
  }

  if (accidentChart.value) {
    const chart = echarts.init(accidentChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '50%',
        data: toPieData(chartData.value.accidentTypes),
      }],
    })
  }

  if (trendChart.value) {
    const trend = chartData.value.monthlyTrend
    const chart = echarts.init(trendChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['培训人数', '推演次数'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: trend.months,
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '培训人数',
          type: 'line',
          smooth: true,
          data: trend.trainingUsers,
          itemStyle: { color: '#3b82f6' },
        },
        {
          name: '推演次数',
          type: 'line',
          smooth: true,
          data: trend.simulationCounts,
          itemStyle: { color: '#10b981' },
        },
      ],
    })
  }

  if (riskChart.value) {
    const chart = echarts.init(riskChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: chartData.value.riskLevels.map(item => item.name),
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: chartData.value.riskLevels.map(item => ({
          value: item.value,
          itemStyle: { color: item.color },
        })),
      }],
    })
  }
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1f2937;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}
</style>
