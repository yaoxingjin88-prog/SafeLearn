<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import type { TelemetryPoint } from '../types'

use([CanvasRenderer, LineChart, GridComponent, LegendComponent, TooltipComponent])

const props = defineProps<{
  history: TelemetryPoint[]
}>()

const chartOption = computed(() => {
  const h = props.history
  const times = h.map(p => `T+${Math.round(p.t)}`)
  return {
    backgroundColor: 'transparent',
    textStyle: { color: '#94a3b8', fontSize: 11 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度°C', '电压V', 'SOC%', 'H₂ ppm'], textStyle: { color: '#94a3b8' }, top: 0 },
    grid: { left: 42, right: 16, top: 36, bottom: 28 },
    xAxis: { type: 'category', data: times, axisLine: { lineStyle: { color: '#334155' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#1e293b' } }, axisLine: { show: false } },
    series: [
      { name: '温度°C', type: 'line', smooth: true, data: h.map(p => +p.temp.toFixed(1)), lineStyle: { color: '#f97316' }, showSymbol: false },
      { name: '电压V', type: 'line', smooth: true, data: h.map(p => +p.voltage.toFixed(2)), lineStyle: { color: '#38bdf8' }, showSymbol: false },
      { name: 'SOC%', type: 'line', smooth: true, data: h.map(p => +p.soc.toFixed(0)), lineStyle: { color: '#4ade80' }, showSymbol: false },
      { name: 'H₂ ppm', type: 'line', smooth: true, data: h.map(p => +p.h2.toFixed(0)), lineStyle: { color: '#f472b6' }, showSymbol: false },
    ],
  }
})
</script>

<template>
  <div class="charts-wrap">
    <VChart class="chart" :option="chartOption" autoresize />
  </div>
</template>

<style scoped>
.charts-wrap {
  height: 100%;
  min-height: 200px;
}
.chart {
  width: 100%;
  height: 100%;
  min-height: 200px;
}
</style>
