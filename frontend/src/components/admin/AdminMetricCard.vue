<template>
  <div class="metric-card" :class="[`tone-${tone}`, { compact, 'is-live': pulse }]">
    <div class="metric-accent" aria-hidden="true" />
    <div class="metric-inner">
      <div class="metric-label-row">
        <el-icon class="metric-glyph"><component :is="icon" /></el-icon>
        <span class="metric-label">{{ label }}</span>
        <span v-if="pulse" class="metric-live">LIVE</span>
      </div>
      <div class="metric-value">{{ value }}</div>
    </div>
    <div class="metric-watermark" aria-hidden="true">
      <el-icon><component :is="icon" /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(defineProps<{
  value: number | string
  label: string
  icon: Component
  tone?: 'green' | 'amber' | 'blue' | 'violet' | 'rose' | 'slate'
  pulse?: boolean
  compact?: boolean
}>(), {
  tone: 'blue',
  pulse: false,
  compact: false,
})
</script>

<style scoped>
.metric-card {
  position: relative;
  overflow: hidden;
  min-height: 92px;
  padding: 16px 18px 16px 20px;
  background: #fafbfc;
  border: 1px solid #e4e9f0;
  border-radius: 4px;
  font-family: 'Segoe UI', system-ui, sans-serif;
  transition: border-color 0.2s, background 0.2s;
}

.metric-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 12px 12px;
  pointer-events: none;
  opacity: 0.55;
}

.metric-card.compact {
  min-height: 78px;
  padding: 12px 14px 12px 16px;
}

.metric-card.compact .metric-value {
  font-size: 22px;
}

.metric-card:hover {
  background: #fff;
  border-color: #cbd5e1;
}

.metric-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  z-index: 1;
}

.tone-green .metric-accent { background: #059669; }
.tone-amber .metric-accent { background: #d97706; }
.tone-blue .metric-accent { background: #2563eb; }
.tone-violet .metric-accent { background: #7c3aed; }
.tone-rose .metric-accent { background: #e11d48; }
.tone-slate .metric-accent { background: #64748b; }

.tone-green .metric-glyph { color: #059669; }
.tone-amber .metric-glyph { color: #d97706; }
.tone-blue .metric-glyph { color: #2563eb; }
.tone-violet .metric-glyph { color: #7c3aed; }
.tone-rose .metric-glyph { color: #e11d48; }
.tone-slate .metric-glyph { color: #64748b; }

.metric-inner {
  position: relative;
  z-index: 2;
}

.metric-label-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.metric-glyph {
  font-size: 14px;
  flex-shrink: 0;
}

.metric-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.metric-live {
  flex-shrink: 0;
  padding: 1px 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #059669;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.35);
  border-radius: 2px;
  animation: live-blink 2s ease-in-out infinite;
}

@keyframes live-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
}

.is-live .metric-value {
  color: #047857;
}

.metric-watermark {
  position: absolute;
  right: 8px;
  bottom: -6px;
  z-index: 0;
  font-size: 56px;
  color: #94a3b8;
  opacity: 0.07;
  pointer-events: none;
  transform: rotate(-8deg);
}

.metric-watermark :deep(svg) {
  width: 1em;
  height: 1em;
}
</style>
