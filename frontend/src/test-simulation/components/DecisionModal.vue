<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import type { DecisionGate } from '../types'

const props = defineProps<{ gate: DecisionGate }>()
const emit = defineEmits<{ choose: [id: string] }>()
const remaining = ref(props.gate.timeLimitSec)
let timer: ReturnType<typeof setInterval> | null = null

function startTimer() {
  remaining.value = props.gate.timeLimitSec
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) {
      clearInterval(timer!)
      emit('choose', props.gate.options[props.gate.options.length - 1].id)
    }
  }, 1000)
}
watch(() => props.gate.id, startTimer)
onMounted(startTimer)
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="overlay">
    <div class="card">
      <div class="head">
        <span class="tag">{{ gate.title }}</span>
        <span class="timer">{{ remaining }}s</span>
      </div>
      <h3>{{ gate.question }}</h3>
      <p class="ref">{{ gate.regulationRef }}</p>
      <div class="opts">
        <button v-for="o in gate.options" :key="o.id" @click="emit('choose', o.id)">
          <span class="id">{{ o.id.toUpperCase() }}</span>
          <span>{{ o.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: absolute; inset: 0; background: rgba(0, 8, 20, 0.75);
  display: flex; align-items: center; justify-content: center; z-index: 30; padding: 20px;
}
.card {
  width: min(560px, 100%); background: #0c1a30; border: 1px solid #0ea5e9;
  border-radius: 12px; padding: 20px; box-shadow: 0 0 40px rgba(14, 165, 233, 0.2);
}
.head { display: flex; justify-content: space-between; margin-bottom: 12px; }
.tag { font-size: 12px; color: #fbbf24; }
.timer { font-family: monospace; color: #38bdf8; font-size: 18px; }
h3 { margin: 0 0 8px; font-size: 15px; color: #f1f5f9; line-height: 1.5; }
.ref { font-size: 11px; color: #64748b; margin: 0 0 14px; }
.opts { display: flex; flex-direction: column; gap: 8px; }
.opts button {
  display: flex; gap: 10px; text-align: left; padding: 12px;
  background: #061226; border: 1px solid #1e3a5f; border-radius: 8px;
  color: #e2e8f0; cursor: pointer;
}
.opts button:hover { border-color: #0ea5e9; background: #0f2744; }
.id {
  width: 22px; height: 22px; border-radius: 4px; background: #1e3a5f;
  display: flex; align-items: center; justify-content: center; font-size: 11px; flex-shrink: 0;
}
</style>
