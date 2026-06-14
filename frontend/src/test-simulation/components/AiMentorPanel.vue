<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ reply: string; faqs: string[] }>()
const emit = defineEmits<{ ask: [q: string] }>()
const input = ref('')

function quick(q: string) {
  emit('ask', q)
}

function submit() {
  if (input.value.trim()) {
    emit('ask', input.value.trim())
    input.value = ''
  }
}
</script>

<template>
  <div class="mentor">
    <div class="mentor-head">
      <span class="icon">🛡️</span>
      <span>AI 安全导师</span>
    </div>
    <div class="quick">
      <button v-for="q in faqs" :key="q" @click="quick(q)">{{ q }}</button>
    </div>
    <div v-if="reply" class="reply">{{ reply }}</div>
    <div class="input-row">
      <input v-model="input" placeholder="提问：为什么温度升高？现在怎么办？" @keyup.enter="submit" />
      <button @click="submit">发送</button>
    </div>
  </div>
</template>

<style scoped>
.mentor {
  flex-shrink: 0;
  background: linear-gradient(135deg, #061226, #0c1a30);
  border-top: 1px solid rgba(14, 165, 233, 0.35);
  padding: 12px 16px;
}
.mentor-head {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #7dd3fc; margin-bottom: 8px;
}
.quick { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.quick button {
  font-size: 11px; padding: 4px 10px; border-radius: 999px;
  border: 1px solid rgba(14, 165, 233, 0.4); background: rgba(14, 165, 233, 0.1);
  color: #94a3b8; cursor: pointer;
}
.quick button:hover { color: #7dd3fc; border-color: #0ea5e9; }
.reply {
  font-size: 12px; line-height: 1.6; color: #cbd5e1;
  background: rgba(0, 0, 0, 0.3); padding: 10px; border-radius: 8px;
  margin-bottom: 8px; white-space: pre-wrap;
}
.input-row { display: flex; gap: 8px; }
.input-row input {
  flex: 1; background: #0f172a; border: 1px solid #1e3a5f; border-radius: 6px;
  padding: 8px 12px; color: #e2e8f0; font-size: 12px;
}
.input-row button {
  background: #0ea5e9; border: none; color: #fff; padding: 8px 16px;
  border-radius: 6px; cursor: pointer; font-size: 12px;
}
</style>
