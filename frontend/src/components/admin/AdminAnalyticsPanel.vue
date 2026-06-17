<template>
  <div v-loading="loading" class="analytics-panel">
    <template v-if="data">
      <div class="charts-grid">
        <!-- 平台运营趋势（合并 4 条月度趋势） -->
        <div class="chart-card chart-card--wide">
          <div class="chart-card-head">
            <span class="chart-title">平台运营趋势</span>
            <span class="chart-sub">近 6 个月 · 测验通过率 / 新用户 / AI 问答 / 证书颁发</span>
          </div>
          <div ref="platformTrendRef" class="chart-box" />
        </div>

        <!-- 学习成效 + 用户活跃 -->
        <div class="chart-card">
          <div class="chart-card-head">
            <span class="chart-title">学习成效</span>
            <span class="chart-sub">课程完成率 Top5 · 知识掌握度分布</span>
          </div>
          <div ref="learningRef" class="chart-box" />
        </div>

        <div class="chart-card">
          <div class="chart-card-head">
            <span class="chart-title">用户活跃</span>
            <span class="chart-sub">近 12 周活跃趋势 · 累计学习时长分布</span>
          </div>
          <div ref="activityRef" class="chart-box" />
        </div>

        <!-- 推演训练（得分 + 场景 + 雷达） -->
        <div class="chart-card chart-card--wide">
          <div class="chart-card-head">
            <span class="chart-title">推演训练分析</span>
            <span class="chart-sub">得分分布 · 场景成功率 · 决策维度均分</span>
          </div>
          <div ref="simulationRef" class="chart-box chart-box--tall" />
        </div>

        <!-- 部门对比 -->
        <div class="chart-card chart-card--wide">
          <div class="chart-card-head">
            <span class="chart-title">部门对比</span>
            <span class="chart-sub">平均得分排行 · 近 8 周学习活跃度</span>
          </div>
          <div ref="departmentRef" class="chart-box chart-box--tall" />
        </div>

        <!-- AI + 证书构成 -->
        <div class="chart-card">
          <div class="chart-card-head">
            <span class="chart-title">服务构成</span>
            <span class="chart-sub">AI 问题分类 · 证书类型分布</span>
          </div>
          <div ref="compositionRef" class="chart-box" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

interface ChartItem { name: string; value: number; color?: string }

const loading = ref(true)
const data = ref<any>(null)

const learningRef = ref<HTMLElement>()
const platformTrendRef = ref<HTMLElement>()
const simulationRef = ref<HTMLElement>()
const activityRef = ref<HTMLElement>()
const departmentRef = ref<HTMLElement>()
const compositionRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

const axisStyle = {
  axisLine: { lineStyle: { color: '#e2e8f0' } },
  axisLabel: { color: '#64748b', fontSize: 11 },
}

function pieData(items: ChartItem[]) {
  return items.map(i => ({ name: i.name, value: i.value, itemStyle: { color: i.color || '#3b82f6' } }))
}

function initChart(el: HTMLElement | undefined, option: echarts.EChartsOption) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption(option)
  charts.push(chart)
}

function renderLearning(d: any) {
  const rankItems = [...(d.learningEffect.courseCompletionRank as ChartItem[])].reverse()
  const mastery = d.learningEffect.masteryDistribution as ChartItem[]

  initChart(learningRef.value, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 4, textStyle: { color: '#64748b', fontSize: 11 } },
    grid: { left: '4%', right: '42%', top: '10%', bottom: '14%', containLabel: true },
    xAxis: {
      type: 'value', max: 100, gridIndex: 0,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b', formatter: '{value}%' },
    },
    yAxis: {
      type: 'category', gridIndex: 0,
      data: rankItems.map(i => i.name),
      axisLabel: { color: '#475569', fontSize: 11, width: 72, overflow: 'truncate' },
    },
    series: [
      {
        type: 'bar', xAxisIndex: 0, yAxisIndex: 0,
        data: rankItems.map(i => ({
          value: i.value,
          itemStyle: { color: i.color || '#3b82f6', borderRadius: [0, 4, 4, 0] },
        })),
        barWidth: 12,
        label: { show: true, position: 'right', formatter: '{c}%', fontSize: 10, color: '#94a3b8' },
      },
      {
        type: 'pie', center: ['78%', '46%'], radius: ['36%', '56%'],
        label: { show: false },
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        data: pieData(mastery),
      },
    ],
    graphic: [{
      type: 'text', left: '72%', top: '6%',
      style: { text: '掌握度', fill: '#94a3b8', fontSize: 11, fontWeight: 600 },
    }],
  })
}

function renderPlatformTrend(d: any) {
  const months = d.learningEffect.quizPassRateTrend.months
  const quizRates = d.learningEffect.quizPassRateTrend.rates
  const newUsers = d.userActivity.newUserTrend.counts
  const qaCounts = d.aiUsage.qaTrend.counts
  const certCounts = d.certificates.issueTrend.counts

  initChart(platformTrendRef.value, {
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155', fontSize: 12 } },
    legend: { top: 4, textStyle: { color: '#64748b', fontSize: 11 } },
    grid: { left: '3%', right: '5%', bottom: '8%', top: '18%', containLabel: true },
    xAxis: { type: 'category', data: months, ...axisStyle },
    yAxis: [
      { type: 'value', name: '通过率', max: 100, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#64748b', formatter: '{value}%' }, nameTextStyle: { color: '#94a3b8', fontSize: 11 } },
      { type: 'value', name: '次数', minInterval: 1, splitLine: { show: false }, axisLabel: { color: '#64748b' }, nameTextStyle: { color: '#94a3b8', fontSize: 11 } },
    ],
    series: [
      {
        name: '测验通过率', type: 'line', smooth: true, yAxisIndex: 0, data: quizRates,
        itemStyle: { color: '#10b981' }, lineStyle: { width: 2.5 },
        areaStyle: { color: 'rgba(16,185,129,0.08)' },
      },
      { name: '新用户', type: 'bar', yAxisIndex: 1, barGap: 0, data: newUsers, itemStyle: { color: '#3b82f6', borderRadius: [3, 3, 0, 0] }, barMaxWidth: 14 },
      { name: 'AI 问答', type: 'bar', yAxisIndex: 1, data: qaCounts, itemStyle: { color: '#06b6d4', borderRadius: [3, 3, 0, 0] }, barMaxWidth: 14 },
      { name: '证书颁发', type: 'bar', yAxisIndex: 1, data: certCounts, itemStyle: { color: '#f59e0b', borderRadius: [3, 3, 0, 0] }, barMaxWidth: 14 },
    ],
  })
}

function renderSimulation(d: any) {
  const scores = d.simulationEffect.scoreDistribution as ChartItem[]
  const scenarios = d.simulationEffect.scenarioSuccessRate
  const radar = d.simulationEffect.decisionRadar

  initChart(simulationRef.value, {
    tooltip: { trigger: 'item' },
    legend: { top: 4, textStyle: { color: '#64748b', fontSize: 11 }, data: ['得分人数', '场景成功率', '维度均分'] },
    grid: [
      { left: '4%', top: '16%', width: '38%', height: '34%' },
      { left: '4%', top: '58%', width: '38%', height: '34%' },
    ],
    xAxis: [
      { type: 'category', gridIndex: 0, data: scores.map(i => i.name), axisLabel: { color: '#64748b', fontSize: 9 }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
      { type: 'category', gridIndex: 1, data: scenarios.map((i: any) => i.name), axisLabel: { color: '#64748b', fontSize: 9, rotate: scenarios.length > 3 ? 15 : 0 }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    ],
    yAxis: [
      { type: 'value', gridIndex: 0, minInterval: 1, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#64748b', fontSize: 10 } },
      { type: 'value', gridIndex: 1, max: 100, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#64748b', fontSize: 10, formatter: '{value}%' } },
    ],
    radar: {
      center: ['76%', '52%'], radius: '46%',
      indicator: radar.indicators,
      splitArea: { areaStyle: { color: ['#f8fafc', '#fff'] } },
      axisName: { color: '#64748b', fontSize: 10 },
    },
    series: [
      {
        name: '得分人数', type: 'bar', xAxisIndex: 0, yAxisIndex: 0,
        data: scores.map(i => ({ value: i.value, itemStyle: { color: i.color, borderRadius: [3, 3, 0, 0] } })),
        barWidth: '50%',
      },
      {
        name: '场景成功率', type: 'bar', xAxisIndex: 1, yAxisIndex: 1,
        data: scenarios.map((i: any) => ({
          value: i.value,
          itemStyle: { color: i.value >= 80 ? '#10b981' : i.value >= 50 ? '#3b82f6' : '#f59e0b', borderRadius: [3, 3, 0, 0] },
        })),
        barWidth: '45%',
      },
      {
        name: '维度均分', type: 'radar',
        data: [{ value: radar.values, areaStyle: { color: 'rgba(139,92,246,0.18)' }, lineStyle: { color: '#8b5cf6', width: 2 }, itemStyle: { color: '#8b5cf6' } }],
      },
    ],
    graphic: [
      { type: 'text', left: '4%', top: '8%', style: { text: '得分分布', fill: '#94a3b8', fontSize: 10 } },
      { type: 'text', left: '4%', top: '50%', style: { text: '场景成功率', fill: '#94a3b8', fontSize: 10 } },
      { type: 'text', left: '62%', top: '8%', style: { text: '决策维度', fill: '#94a3b8', fontSize: 10 } },
    ],
  })
}

function renderActivity(d: any) {
  const weekly = d.userActivity.weeklyActiveTrend
  const duration = d.userActivity.studyDurationDistribution as ChartItem[]

  initChart(activityRef.value, {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 4, textStyle: { color: '#64748b', fontSize: 11 } },
    grid: { left: '4%', right: '42%', top: '10%', bottom: '14%', containLabel: true },
    xAxis: { type: 'category', gridIndex: 0, data: weekly.weeks, ...axisStyle },
    yAxis: { type: 'value', gridIndex: 0, minInterval: 1, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#64748b' } },
    series: [
      {
        type: 'line', smooth: true, xAxisIndex: 0, yAxisIndex: 0,
        data: weekly.counts, name: '周活跃',
        itemStyle: { color: '#8b5cf6' }, lineStyle: { width: 2.5 },
        areaStyle: { color: 'rgba(139,92,246,0.1)' },
      },
      {
        type: 'pie', name: '学习时长', center: ['78%', '46%'], radius: ['36%', '56%'],
        label: { show: false },
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        data: pieData(duration),
      },
    ],
    graphic: [{
      type: 'text', left: '70%', top: '6%',
      style: { text: '时长分布', fill: '#94a3b8', fontSize: 11, fontWeight: 600 },
    }],
  })
}

function renderDepartment(d: any) {
  const deptScores = [...(d.departmentCompare.deptScoreRank as ChartItem[])].reverse()
  const heat = d.departmentCompare.deptActivityHeatmap
  const heatMax = Math.max(1, ...heat.data.map((i: number[]) => i[2]))

  initChart(departmentRef.value, {
    tooltip: [
      { trigger: 'axis' },
      { trigger: 'item', formatter: (p: any) => `${heat.departments[p.data[1]]}<br/>${heat.weeks[p.data[0]]}: ${p.data[2]} 人` },
    ],
    grid: [
      { left: '4%', top: '10%', width: '38%', height: '78%', containLabel: true },
      { left: '48%', top: '10%', width: '48%', height: '72%' },
    ],
    xAxis: [
      { type: 'value', gridIndex: 0, max: 100, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#64748b' } },
      { type: 'category', gridIndex: 1, data: heat.weeks, splitArea: { show: true }, axisLabel: { color: '#64748b', fontSize: 10 } },
    ],
    yAxis: [
      { type: 'category', gridIndex: 0, data: deptScores.map(i => i.name), axisLabel: { color: '#475569', fontSize: 11 } },
      { type: 'category', gridIndex: 1, data: heat.departments, axisLabel: { color: '#475569', fontSize: 11 } },
    ],
    visualMap: {
      min: 0, max: heatMax, seriesIndex: 1, orient: 'horizontal',
      left: 'center', bottom: '2%', itemWidth: 12, itemHeight: 80,
      inRange: { color: ['#eff6ff', '#3b82f6', '#1d4ed8'] },
      textStyle: { color: '#64748b', fontSize: 10 },
    },
    series: [
      {
        type: 'bar', xAxisIndex: 0, yAxisIndex: 0,
        data: deptScores.map(i => ({ value: i.value, itemStyle: { color: i.color || '#8b5cf6', borderRadius: [0, 4, 4, 0] } })),
        barWidth: 14,
      },
      {
        type: 'heatmap', xAxisIndex: 1, yAxisIndex: 1,
        data: heat.data,
        label: { show: true, fontSize: 10, color: '#334155' },
        emphasis: { itemStyle: { shadowBlur: 4, shadowColor: 'rgba(0,0,0,0.12)' } },
      },
    ],
    graphic: [
      { type: 'text', left: '4%', top: '2%', style: { text: '平均得分', fill: '#94a3b8', fontSize: 10 } },
      { type: 'text', left: '48%', top: '2%', style: { text: '活跃热力', fill: '#94a3b8', fontSize: 10 } },
    ],
  })
}

function renderComposition(d: any) {
  const qaCats = d.aiUsage.qaCategoryDistribution as ChartItem[]
  const certTypes = d.certificates.typeDistribution as ChartItem[]

  initChart(compositionRef.value, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 4, textStyle: { color: '#64748b', fontSize: 11 } },
    series: [
      {
        type: 'pie', name: 'AI 问题', center: ['28%', '46%'], radius: ['34%', '54%'],
        label: { show: false },
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        data: pieData(qaCats),
      },
      {
        type: 'pie', name: '证书类型', center: ['72%', '46%'], radius: ['34%', '54%'],
        label: { show: false },
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        data: pieData(certTypes),
      },
    ],
    graphic: [
      { type: 'text', left: '20%', top: '6%', style: { text: 'AI 问题', fill: '#94a3b8', fontSize: 11, fontWeight: 600 } },
      { type: 'text', left: '64%', top: '6%', style: { text: '证书类型', fill: '#94a3b8', fontSize: 11, fontWeight: 600 } },
    ],
  })
}

function renderAll() {
  if (!data.value) return
  const d = data.value
  renderLearning(d)
  renderPlatformTrend(d)
  renderSimulation(d)
  renderActivity(d)
  renderDepartment(d)
  renderComposition(d)
}

function handleResize() {
  charts.forEach(c => c.resize())
}

onMounted(async () => {
  try {
    const res = await request.get('/admin/analytics')
    data.value = res.data
    loading.value = false
    await nextTick()
    renderAll()
    window.addEventListener('resize', handleResize)
  } catch (err) {
    loading.value = false
    console.warn('加载分析数据失败', err)
  }
})

onUnmounted(() => {
  charts.forEach(c => c.dispose())
  charts = []
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.analytics-panel {
  min-height: 160px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.chart-card {
  background: #fff;
  border: 1px solid #eef1f6;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.chart-card--wide {
  grid-column: span 2;
}

.chart-card-head {
  padding: 14px 16px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chart-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.chart-sub {
  font-size: 11px;
  color: #94a3b8;
}

.chart-box {
  height: 280px;
  padding: 4px 8px 10px;
}

.chart-box--tall {
  height: 320px;
}

@media (max-width: 960px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
  .chart-card--wide {
    grid-column: span 1;
  }
}
</style>
