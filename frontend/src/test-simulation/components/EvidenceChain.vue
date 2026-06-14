<script setup lang="ts">
import type { EvidenceItem } from '../types'

defineProps<{
  items: EvidenceItem[]
  unlocked: string[]
}>()
</script>

<template>
  <div class="evidence">
    <div class="ev-head">事故证据链</div>
    <div v-for="ev in items" :key="ev.id" class="ev-item" :class="{ locked: !unlocked.includes(ev.id) }">
      <span class="ev-type">{{ ev.type.toUpperCase() }}</span>
      <div>
        <div class="ev-title">{{ ev.title }}</div>
        <p>{{ unlocked.includes(ev.id) ? ev.summary : '推演推进后解锁' }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.evidence { padding: 12px; }
.ev-head { font-size: 12px; color: #38bdf8; font-weight: 600; margin-bottom: 10px; }
.ev-item {
  display: flex; gap: 10px; padding: 10px; margin-bottom: 8px;
  background: rgba(14, 165, 233, 0.06); border: 1px solid rgba(14, 165, 233, 0.2);
  border-radius: 8px;
}
.ev-item.locked { opacity: 0.4; filter: grayscale(0.8); }
.ev-type {
  font-size: 9px; padding: 2px 6px; border-radius: 4px;
  background: #1e3a5f; color: #7dd3fc; height: fit-content;
}
.ev-title { font-size: 12px; font-weight: 600; color: #e2e8f0; }
p { margin: 4px 0 0; font-size: 11px; color: #94a3b8; }
</style>
