<script setup lang="ts">
import type { SimPhase, TimelineNode } from '../types'

const props = defineProps<{
  nodes: TimelineNode[]
  currentKey: string
  phase: SimPhase
  offsetMin?: number
}>()

function nodeStatus(node: TimelineNode) {
  const offset = props.offsetMin ?? 0
  if (node.key === props.currentKey) return 'active'
  if (node.offsetMin <= offset) return 'done'
  return 'pending'
}

const phaseLabel: Record<SimPhase, string> = {
  background: '背景', testing: '测试', anomaly: '异常', thermal_runaway: '热失控',
  flashover: '闪爆', emergency: '应急', investigation: '调查', debrief: '复盘',
}
</script>

<template>
  <div class="sim-tl">
    <div class="tl-head">事故时间线</div>
    <ul>
      <li v-for="n in nodes" :key="n.key" :class="nodeStatus(n)">
        <div class="dot" />
        <div class="body">
          <div class="time">{{ n.clock }}</div>
          <div class="title">{{ n.title }}</div>
          <p v-if="n.key === currentKey" class="nar">{{ n.narrative }}</p>
          <span class="phase-tag">{{ phaseLabel[n.phase] }}</span>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.sim-tl {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  background: linear-gradient(180deg, #061226 0%, #0a1628 100%);
  border-right: 1px solid rgba(14, 165, 233, 0.2);
}
.tl-head {
  font-size: 13px;
  font-weight: 600;
  color: #38bdf8;
  margin-bottom: 12px;
  letter-spacing: 2px;
}
ul { list-style: none; margin: 0; padding: 0; }
li {
  display: flex;
  gap: 10px;
  padding: 10px 0 10px 8px;
  border-left: 2px solid #1e3a5f;
  margin-left: 6px;
  opacity: 0.45;
}
li.done { opacity: 0.75; border-left-color: #22c55e; }
li.done .dot { background: #22c55e; }
li.active { opacity: 1; border-left-color: #0ea5e9; }
.dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #334155; margin-left: -13px; margin-top: 6px; flex-shrink: 0;
}
li.active .dot { background: #0ea5e9; box-shadow: 0 0 10px #0ea5e9; }
.time { font-family: monospace; font-size: 13px; color: #7dd3fc; }
.title { font-size: 13px; font-weight: 600; color: #e2e8f0; margin: 2px 0; }
.nar { font-size: 12px; color: #94a3b8; line-height: 1.5; margin: 6px 0; }
.phase-tag {
  font-size: 10px; padding: 1px 6px; border-radius: 4px;
  background: rgba(14, 165, 233, 0.15); color: #7dd3fc;
}
</style>
