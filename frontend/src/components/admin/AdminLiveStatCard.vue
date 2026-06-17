<template>
  <div class="live-stat" :class="[`tone-${tone}`, { pulse }]">
    <div class="live-stat-body">
      <span class="live-label">{{ label }}</span>
      <div class="live-value">
        <span class="live-num">{{ value }}</span>
        <span class="live-unit">{{ unit }}</span>
      </div>
    </div>
    <div class="live-stat-icon">
      <el-icon :size="20"><component :is="icon" /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(defineProps<{
  label: string
  value: number | string
  unit?: string
  icon: Component
  tone?: 'green' | 'orange' | 'blue' | 'purple'
  pulse?: boolean
}>(), {
  unit: '人',
  tone: 'blue',
  pulse: false,
})
</script>

<style scoped>
.live-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(to bottom right, #fff, #f8fafc);
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.2s;
}

.live-stat:hover {
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.live-stat-body {
  min-width: 0;
}

.live-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 4px;
}

.live-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  line-height: 1;
}

.live-num {
  font-size: 24px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  transition: transform 0.5s, color 0.3s;
}

.live-unit {
  font-size: 10px;
  font-weight: 500;
  color: #94a3b8;
}

.live-stat-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tone-green .live-stat-icon { background: #ecfdf5; color: #10b981; }
.tone-green .live-num { color: #059669; }
.tone-orange .live-stat-icon { background: #fffbeb; color: #f59e0b; }
.tone-orange .live-num { color: #f59e0b; }
.tone-blue .live-stat-icon { background: #eff6ff; color: #2563eb; }
.tone-blue .live-num { color: #2563eb; }
.tone-purple .live-stat-icon { background: #f5f3ff; color: #8b5cf6; }
.tone-purple .live-num { color: #7c3aed; }

.live-stat.pulse .live-num {
  transform: scale(1.08);
  color: #34d399;
}
</style>
