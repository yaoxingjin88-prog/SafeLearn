<template>
  <div class="temperature-field">
    <div class="field-header">
      <span class="title">温度场分布</span>
      <div class="legend">
        <span class="legend-item"><span class="dot" style="background:#10b981" />正常</span>
        <span class="legend-item"><span class="dot" style="background:#f59e0b" />警告</span>
        <span class="legend-item"><span class="dot" style="background:#f97316" />危险</span>
        <span class="legend-item"><span class="dot" style="background:#ef4444" />临界</span>
      </div>
    </div>
    <div class="field-grid" :style="{ gridTemplateColumns: `repeat(${cols}, 1fr)` }">
      <div
        v-for="cell in cells"
        :key="cell.id"
        class="cell-block"
        :style="{ background: getColor(cell.temperature) }"
        :title="`电池#${cell.id + 1}: ${cell.temperature}°C`"
      >
        <span class="cell-temp">{{ cell.temperature }}°</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BatteryCellState } from '@/composables/useSimulation'

const props = withDefaults(defineProps<{
  cells: BatteryCellState[]
  cols?: number
}>(), {
  cols: 4,
})

const cols = props.cols

function getColor(temp: number): string {
  if (temp < 40) return 'rgba(16,185,129,0.7)'
  if (temp < 60) return `rgba(245,158,11,${0.5 + (temp - 40) / 40})`
  if (temp < 100) return `rgba(249,115,22,${0.6 + (temp - 60) / 100})`
  return `rgba(239,68,68,${0.7 + Math.min(0.3, (temp - 100) / 300)})`
}
</script>

<script lang="ts">
export default { name: 'TemperatureField' }
</script>

<style scoped>
.temperature-field {
  background: rgba(0, 0, 0, 0.4);
  border-radius: 8px;
  padding: 12px;
}
.field-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.title {
  color: white;
  font-size: 13px;
  font-weight: 600;
}
.legend {
  display: flex;
  gap: 10px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #9ca3af;
  font-size: 11px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.field-grid {
  display: grid;
  gap: 4px;
}
.cell-block {
  aspect-ratio: 1;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s;
}
.cell-block:hover {
  transform: scale(1.1);
  z-index: 1;
}
.cell-temp {
  color: white;
  font-size: 11px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}
</style>
