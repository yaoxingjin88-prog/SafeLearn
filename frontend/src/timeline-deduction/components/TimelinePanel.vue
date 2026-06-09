<script setup lang="ts">
import type { TimelineNode, TimelinePhase } from '../types'

const props = defineProps<{
  nodes: TimelineNode[]
  currentKey: string
  phase: TimelinePhase
  offsetMin: number
}>()

const phaseLabels: Record<TimelinePhase, string> = {
  normal: '正常运行',
  warning: '异常预警',
  thermal_runaway: '热失控',
  spread: '扩散蔓延',
  explosion: '爆炸冲击',
  debrief: '复盘总结',
}

function status(key: string, offsetMin: number) {
  const node = props.nodes.find(n => n.key === key)!
  if (props.currentKey === key) return 'active'
  if (offsetMin >= node.offsetMin) return 'done'
  return 'pending'
}
</script>

<template>
  <div class="timeline-panel">
    <div class="panel-title">事故时间轴</div>
    <div class="phase-badge">{{ phaseLabels[phase] }}</div>
    <div class="clock">T+{{ offsetMin.toFixed(0) }} min</div>
    <ul class="node-list">
      <li
        v-for="node in nodes"
        :key="node.key"
        class="node-item"
        :class="status(node.key, offsetMin)"
      >
        <div class="dot" />
        <div class="node-body">
          <div class="node-title">{{ node.title }}</div>
          <p class="node-text">{{ node.narrative }}</p>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.timeline-panel {
  height: 100%;
  padding: 16px;
  background: linear-gradient(180deg, #0f172a 0%, #111827 100%);
  border-radius: 12px;
  color: #e2e8f0;
  overflow-y: auto;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
.phase-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.2);
  color: #fb923c;
  font-size: 12px;
  margin-bottom: 6px;
}
.clock {
  font-family: 'Consolas', monospace;
  font-size: 22px;
  font-weight: 700;
  color: #38bdf8;
  margin-bottom: 16px;
}
.node-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.node-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-left: 2px solid #334155;
  margin-left: 6px;
  padding-left: 14px;
  opacity: 0.45;
}
.node-item.done {
  opacity: 0.75;
  border-left-color: #22c55e;
}
.node-item.active {
  opacity: 1;
  border-left-color: #f97316;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #475569;
  margin-top: 6px;
  flex-shrink: 0;
  margin-left: -19px;
}
.node-item.active .dot {
  background: #f97316;
  box-shadow: 0 0 8px #f97316;
}
.node-item.done .dot {
  background: #22c55e;
}
.node-title {
  font-size: 13px;
  font-weight: 600;
}
.node-text {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #94a3b8;
}
</style>
