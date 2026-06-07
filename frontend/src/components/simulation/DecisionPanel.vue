<template>
  <el-dialog
    :model-value="!!decision"
    :title="dialogTitle"
    width="520px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="decision-dialog"
  >
    <div v-if="decision" class="decision-body">
      <div class="decision-timer">
        <el-icon><Timer /></el-icon>
        请在 {{ decision.timePressureSec }} 秒内完成决策
      </div>
      <p class="decision-question">{{ decision.question }}</p>
      <div class="decision-options">
        <button
          v-for="opt in decision.options"
          :key="opt.id"
          type="button"
          class="decision-option"
          @click="$emit('select', decision.id, opt.id)"
        >
          <span class="option-label">{{ opt.label }}</span>
          <span v-if="opt.description" class="option-desc">{{ opt.description }}</span>
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Timer } from '@element-plus/icons-vue'
import type { DecisionPointDef } from '@/simulation/types/deduction.types'

const props = defineProps<{
  decision: DecisionPointDef | null
  phase?: string
}>()

defineEmits<{
  select: [decisionPointId: string, optionId: string]
}>()

const phaseLabels: Record<string, string> = {
  earlyWarning: '早期预警',
  thermalRunaway: '热失控',
  isolation: '电气隔离',
}

const dialogTitle = computed(() => {
  const label = phaseLabels[props.phase ?? ''] ?? '应急决策'
  return `${label} — 请选择处置措施`
})
</script>

<style scoped>
.decision-timer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #f59e0b;
  background: #fffbeb;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.decision-question {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
  line-height: 1.6;
  margin-bottom: 16px;
}

.decision-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.decision-option {
  text-align: left;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px 16px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.decision-option:hover {
  border-color: #2b5aed;
  background: #f0f4ff;
}

.option-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.option-desc {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}
</style>
