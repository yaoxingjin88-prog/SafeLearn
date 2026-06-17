<template>
  <div class="charts-panel">
    <div class="charts-row">
      <div class="chart-panel">
        <div class="chart-panel-head">培训完成率</div>
        <div ref="completionChart" class="chart-box" />
      </div>
      <div class="chart-panel">
        <div class="chart-panel-head">事故类型分布</div>
        <div ref="accidentChart" class="chart-box" />
      </div>
    </div>

    <div class="charts-row charts-row--wide">
      <div class="chart-panel chart-panel--wide">
        <div class="chart-panel-head">月度培训趋势</div>
        <div ref="trendChart" class="chart-box chart-box--tall" />
      </div>
      <div class="chart-panel chart-panel--narrow">
        <div class="chart-panel-head">风险等级分布</div>
        <div ref="riskChart" class="chart-box chart-box--tall" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const completionChart = ref<HTMLElement>()
const accidentChart = ref<HTMLElement>()
const trendChart = ref<HTMLElement>()
const riskChart = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

const chartData = ref<{
  completion: { name: string; value: number; color: string }[]
  accidentTypes: { name: string; value: number; color: string }[]
  monthlyTrend: { months: string[]; trainingUsers: number[]; simulationCounts: number[] }
  riskLevels: { name: string; value: number; color: string }[]
} | null>(null)

onMounted(async () => {
  try {
    const chartsRes = await request.get('/admin/charts')
    chartData.value = chartsRes.data
    initCharts()
    window.addEventListener('resize', handleResize)
  } catch (err) {
    console.warn('加载图表数据失败', err)
  }
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
      legend: { bottom: 0, textStyle: { color: '#64748b', fontSize: 12 } },
      series: [{
        type: 'pie',
        radius: ['42%', '68%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: toPieData(chartData.value.completion),
      }],
    })
  }

  if (accidentChart.value) {
    const chart = echarts.init(accidentChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: '#64748b', fontSize: 12 } },
      series: [{
        type: 'pie',
        radius: '58%',
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
      legend: { data: ['培训人数', '推演次数'], textStyle: { color: '#64748b' } },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '12%', containLabel: true },
      xAxis: {
        type: 'category',
        data: trend.months,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b' },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#64748b' },
      },
      series: [
        {
          name: '培训人数',
          type: 'line',
          smooth: true,
          data: trend.trainingUsers,
          itemStyle: { color: '#3b82f6' },
          areaStyle: { color: 'rgba(59, 130, 246, 0.08)' },
        },
        {
          name: '推演次数',
          type: 'line',
          smooth: true,
          data: trend.simulationCounts,
          itemStyle: { color: '#10b981' },
          areaStyle: { color: 'rgba(16, 185, 129, 0.08)' },
        },
      ],
    })
  }

  if (riskChart.value) {
    const chart = echarts.init(riskChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        data: chartData.value.riskLevels.map(item => item.name),
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b', fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#64748b' },
      },
      series: [{
        type: 'bar',
        barWidth: '48%',
        data: chartData.value.riskLevels.map(item => ({
          value: item.value,
          itemStyle: { color: item.color, borderRadius: [4, 4, 0, 0] },
        })),
      }],
    })
  }
}
</script>

<style scoped>
.charts-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.charts-row--wide {
  grid-template-columns: 1.6fr 1fr;
}

.chart-panel {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.chart-panel-head {
  padding: 14px 18px 0;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.chart-box {
  height: 280px;
  padding: 8px 12px 12px;
}

.chart-box--tall {
  height: 300px;
}

@media (max-width: 960px) {
  .charts-row,
  .charts-row--wide {
    grid-template-columns: 1fr;
  }
}
</style>
