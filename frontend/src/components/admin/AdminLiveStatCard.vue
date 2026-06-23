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
  tone?: 'green' | 'orange' | 'blue' | 'purple' | 'red'
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
  min-height: 88px;
  background: var(--card-gradient);
  border: 0;
  border-radius: 10px;
  box-shadow: 0 8px 20px color-mix(in srgb, var(--card-shadow) 24%, transparent);
  color: #fff;
  overflow: hidden;
  position: relative;
  transition: transform 0.2s, box-shadow 0.2s;
}

.live-stat::after {
  position: absolute;
  right: -26px;
  bottom: -38px;
  width: 100px;
  height: 100px;
  border: 18px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  content: '';
}

.live-stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px color-mix(in srgb, var(--card-shadow) 32%, transparent);
}

.live-stat-body {
  min-width: 0;
}

.live-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.88);
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
  color: rgba(255, 255, 255, 0.8);
}

.live-stat-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.14) !important;
  color: #fff !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tone-blue { --card-gradient: linear-gradient(135deg, #0866f5, #2f8cff); --card-shadow: #1575f7; }
.tone-green { --card-gradient: linear-gradient(135deg, #04a7b2, #18c9c9); --card-shadow: #09aeb8; }
.tone-orange { --card-gradient: linear-gradient(135deg, #0f83ed, #31a6fb); --card-shadow: #178ce9; }
.tone-purple { --card-gradient: linear-gradient(135deg, #7763ec, #9b7df4); --card-shadow: #7863e9; }
.tone-red { --card-gradient: linear-gradient(135deg, #ff4d58, #ff7878); --card-shadow: #ef4e5a; }
.live-num { color: #fff !important; }

.live-stat.pulse .live-num {
  transform: scale(1.08);
  color: #34d399;
}
</style>
