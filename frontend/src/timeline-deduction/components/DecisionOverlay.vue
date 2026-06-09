<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import type { DecisionGate } from '../types'

const props = defineProps<{
  gate: DecisionGate
}>()

const emit = defineEmits<{
  choose: [optionId: string]
}>()

const remaining = ref(props.gate.timeLimitSec)
let timer: ReturnType<typeof setInterval> | null = null

function startTimer() {
  remaining.value = props.gate.timeLimitSec
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) {
      if (timer) clearInterval(timer)
      emit('choose', props.gate.options[props.gate.options.length - 1].id)
    }
  }, 1000)
}

watch(() => props.gate.id, startTimer)
onMounted(startTimer)
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="decision-overlay">
    <div class="decision-card">
      <div class="decision-header">
        <span class="tag">关键决策点</span>
        <span class="timer" :class="{ urgent: remaining <= 15 }">{{ remaining }}s</span>
      </div>
      <h3>{{ gate.question }}</h3>
      <p v-if="gate.regulationRef" class="regulation">{{ gate.regulationRef }}</p>
      <div class="options">
        <button
          v-for="opt in gate.options"
          :key="opt.id"
          class="opt-btn"
          @click="emit('choose', opt.id)"
        >
          <span class="opt-id">{{ opt.id.toUpperCase() }}</span>
          <span class="opt-label">{{ opt.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.decision-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.45);
  z-index: 20;
}
.decision-card {
  width: min(720px, 100%);
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
}
.decision-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.tag {
  font-size: 12px;
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.15);
  padding: 2px 10px;
  border-radius: 999px;
}
.timer {
  font-family: monospace;
  font-size: 18px;
  color: #38bdf8;
}
.timer.urgent {
  color: #ef4444;
  animation: pulse 0.8s infinite;
}
@keyframes pulse {
  50% { opacity: 0.5; }
}
h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #f1f5f9;
  line-height: 1.5;
}
.regulation {
  margin: 0 0 16px;
  font-size: 12px;
  color: #64748b;
}
.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.opt-btn {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  text-align: left;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #475569;
  background: #0f172a;
  color: #e2e8f0;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.opt-btn:hover {
  border-color: #38bdf8;
  background: #172554;
}
.opt-id {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.opt-label {
  font-size: 14px;
  line-height: 1.45;
}
</style>
