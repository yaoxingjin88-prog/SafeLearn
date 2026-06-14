<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import type { TelemetrySnapshot } from '../types'

use([CanvasRenderer, LineChart, GridComponent, LegendComponent, TooltipComponent])

const props = defineProps<{
  history: TelemetrySnapshot[]
  current: TelemetrySnapshot
  playing: boolean
}>()

const emit = defineEmits<{ togglePlay: [] }>()

const chartOpt = computed(() => {
  const h = props.history
  return {
    backgroundColor: 'transparent',
    textStyle: { color: '#64748b', fontSize: 10 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度', '电压', '电流', 'SOC', '气体'], textStyle: { color: '#64748b' }, top: 0 },
    grid: { left: 40, right: 12, top: 32, bottom: 24 },
    xAxis: { type: 'category', data: h.map(p => p.clock), axisLine: { lineStyle: { color: '#1e3a5f' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#0f172a' } } },
    series: [
      { name: '温度', type: 'line', smooth: true, data: h.map(p => p.packTemp), lineStyle: { color: '#f97316' }, showSymbol: false },
      { name: '电压', type: 'line', smooth: true, data: h.map(p => p.voltage), lineStyle: { color: '#38bdf8' }, showSymbol: false },
      { name: '电流', type: 'line', smooth: true, data: h.map(p => p.current), lineStyle: { color: '#a78bfa' }, showSymbol: false },
      { name: 'SOC', type: 'line', smooth: true, data: h.map(p => p.soc), lineStyle: { color: '#4ade80' }, showSymbol: false },
      { name: '气体', type: 'line', smooth: true, data: h.map(p => p.gasPpm), lineStyle: { color: '#f472b6' }, showSymbol: false },
    ],
  }
})

const metrics = computed(() => [
  { k: '环境温度', v: `${props.current.envTemp}℃` },
  { k: '电池温度', v: `${props.current.packTemp}℃` },
  { k: '电压', v: `${props.current.voltage}V` },
  { k: '电流', v: `${props.current.current}A` },
  { k: 'SOC', v: `${props.current.soc}%` },
  { k: 'SOH', v: `${props.current.soh}%` },
  { k: '绝缘', v: props.current.insulation },
  { k: '可燃气体', v: `${props.current.gasPpm} ppm` },
  { k: 'VOC', v: `${props.current.vocPpm} ppm` },
  { k: '告警数', v: String(props.current.alarmCount) },
])
</script>

<template>
  <div class="telemetry">
    <div class="metrics">
      <div v-for="m in metrics" :key="m.k" class="metric">
        <span class="mk">{{ m.k }}</span>
        <span class="mv">{{ m.v }}</span>
      </div>
    </div>
    <div class="chart-head">
      <span>SCADA 实时曲线</span>
      <button class="play-btn" @click="emit('togglePlay')">{{ playing ? '⏸ 暂停' : '▶ 播放' }}</button>
    </div>
    <VChart class="chart" :option="chartOpt" autoresize />
  </div>
</template>

<style scoped>
.telemetry { padding: 8px; }
.metrics {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  margin-bottom: 8px;
}
.metric {
  background: rgba(14, 165, 233, 0.08);
  border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: 6px;
  padding: 6px 8px;
}
.mk { display: block; font-size: 10px; color: #64748b; }
.mv { font-family: monospace; font-size: 13px; color: #7dd3fc; font-weight: 600; }
.chart-head {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 11px; color: #64748b; margin-bottom: 4px;
}
.play-btn {
  background: transparent; border: 1px solid #0ea5e9; color: #7dd3fc;
  padding: 2px 10px; border-radius: 4px; cursor: pointer; font-size: 11px;
}
.chart { height: 160px; width: 100%; }
</style>
