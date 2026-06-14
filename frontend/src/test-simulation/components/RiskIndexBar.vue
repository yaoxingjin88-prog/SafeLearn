<script setup lang="ts">
import { computed } from 'vue'
import type { RiskLevel } from '../types'

const props = defineProps<{ index: number; level: RiskLevel }>()

const color = computed(() => {
  const m: Record<RiskLevel, string> = {
    low: '#22c55e', medium: '#eab308', high: '#f97316', critical: '#ef4444',
  }
  return m[props.level]
})

const blocks = computed(() =>
  Array.from({ length: 10 }, (_, i) => i < Math.round(props.index / 10)),
)
</script>

<template>
  <div class="risk-bar">
    <span class="label">风险指数</span>
    <div class="blocks">
      <i v-for="(on, i) in blocks" :key="i" :class="{ on }" :style="on ? { background: color, boxShadow: `0 0 8px ${color}` } : {}" />
    </div>
    <span class="pct" :style="{ color }">{{ index }}%</span>
  </div>
</template>

<style scoped>
.risk-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(6, 18, 38, 0.9);
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 8px;
  font-family: 'Consolas', monospace;
}
.label { font-size: 12px; color: #7dd3fc; letter-spacing: 1px; }
.blocks { display: flex; gap: 4px; }
.blocks i {
  display: block;
  width: 14px;
  height: 18px;
  background: #1e293b;
  border-radius: 2px;
  transition: all 0.3s;
}
.pct { font-size: 20px; font-weight: 700; min-width: 48px; text-align: right; }
</style>
