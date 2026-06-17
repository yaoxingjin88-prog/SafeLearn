<template>
  <div class="platform-stat" :class="`tone-${tone}`">
    <div class="platform-watermark" aria-hidden="true">
      <el-icon :size="80"><component :is="icon" /></el-icon>
    </div>

    <div class="platform-content">
      <div class="platform-body">
        <span class="platform-label">{{ label }}</span>
        <div class="platform-value-row">
          <span class="platform-value">{{ value }}</span>
          <span class="platform-unit">{{ unit }}</span>
          <span
            v-if="trend != null"
            class="platform-trend"
            :class="trend >= 0 ? 'up' : 'down'"
          >
            <el-icon v-if="trend >= 0" :size="10"><Top /></el-icon>
            <el-icon v-else :size="10"><Bottom /></el-icon>
            {{ Math.abs(trend) }}%
          </span>
        </div>
      </div>
      <div class="platform-icon">
        <el-icon :size="24"><component :is="icon" /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { Top, Bottom } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  label: string
  value: number | string
  unit?: string
  icon: Component
  tone?: 'blue' | 'green' | 'orange'
  trend?: number | null
}>(), {
  unit: '',
  tone: 'blue',
  trend: null,
})
</script>

<style scoped>
.platform-stat {
  position: relative;
  padding: 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-left: 4px solid var(--tone-border);
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.platform-stat:hover {
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.tone-blue { --tone-border: #3b82f6; --tone: #2563eb; --tone-bg: #eff6ff; --tone-wm: #dbeafe; }
.tone-green { --tone-border: #10b981; --tone: #059669; --tone-bg: #ecfdf5; --tone-wm: #d1fae5; }
.tone-orange { --tone-border: #f59e0b; --tone: #d97706; --tone-bg: #fffbeb; --tone-wm: #fef3c7; }

.platform-watermark {
  position: absolute;
  right: 12px;
  bottom: -8px;
  color: var(--tone-wm);
  opacity: 0.35;
  pointer-events: none;
  transition: transform 0.3s;
}

.platform-stat:hover .platform-watermark {
  transform: scale(1.08);
}

.platform-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.platform-body {
  min-width: 0;
}

.platform-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 4px;
}

.platform-value-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-wrap: wrap;
}

.platform-value {
  font-size: 30px;
  font-weight: 900;
  color: #1e293b;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.platform-unit {
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
}

.platform-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 700;
  border-radius: 6px;
}

.platform-trend.up {
  color: #059669;
  background: #ecfdf5;
}

.platform-trend.down {
  color: #dc2626;
  background: #fef2f2;
}

.platform-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--tone-bg);
  color: var(--tone);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
