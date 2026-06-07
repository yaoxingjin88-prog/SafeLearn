<template>
  <el-dialog v-model="visible" title="推演评分报告" width="560px" :close-on-click-modal="true">
    <div v-if="report" class="score-report">
      <div class="score-header">
        <div class="score-circle" :class="report.rating">
          {{ report.totalScore }}
        </div>
        <div>
          <div class="score-rating">{{ ratingLabel(report.rating) }}</div>
          <div class="score-sub">规则分 {{ report.ruleScore }} · AI 分 {{ report.aiScore ?? '-' }}</div>
        </div>
      </div>
      <div v-if="dimensionList.length" class="dimensions">
        <div v-for="d in dimensionList" :key="d.key" class="dim-row">
          <span class="dim-name">{{ d.label }}</span>
          <el-progress :percentage="d.score * 4" :stroke-width="8" :format="() => `${d.score}/25`" />
          <p class="dim-comment">{{ d.comment }}</p>
        </div>
      </div>
      <p v-if="report.instructorSummary" class="score-summary">{{ report.instructorSummary }}</p>
      <div v-if="report.highlights?.length" class="score-section">
        <div class="section-title">亮点</div>
        <ul>
          <li v-for="(h, i) in report.highlights" :key="i">{{ h }}</li>
        </ul>
      </div>
      <div v-if="report.improvements?.length" class="score-section">
        <div class="section-title">改进建议</div>
        <ul>
          <li v-for="(h, i) in report.improvements" :key="i">{{ h }}</li>
        </ul>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="visible = false">知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: boolean
  report: Record<string, any> | null
}>()

const emit = defineEmits<{ 'update:modelValue': [v: boolean] }>()

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v),
})

const dimensionList = computed(() => {
  const dims = props.report?.dimensions
  if (!dims || typeof dims !== 'object') return []
  const labels: Record<string, string> = {
    timeliness: '响应时效',
    procedure: '处置顺序',
    decision: '决策合理性',
    outcome: '结果控制',
  }
  return Object.entries(dims).map(([key, val]: [string, any]) => ({
    key,
    label: labels[key] || key,
    score: val?.score ?? 0,
    comment: val?.comment ?? '',
  }))
})

function ratingLabel(rating: string) {
  const map: Record<string, string> = {
    excellent: '优秀',
    good: '良好',
    average: '及格',
    poor: '不及格',
  }
  return map[rating] || rating
}
</script>

<style scoped>
.score-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.score-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.score-circle.excellent { background: #10b981; }
.score-circle.good { background: #2b5aed; }
.score-circle.average { background: #f59e0b; }
.score-circle.poor { background: #ef4444; }

.score-rating {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.score-sub {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.score-summary {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.score-section {
  margin-bottom: 12px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 6px;
}

.score-section ul {
  margin: 0;
  padding-left: 18px;
  font-size: 14px;
  color: #374151;
}

.dimensions { margin-bottom: 16px; }
.dim-row { margin-bottom: 14px; }
.dim-name { font-size: 13px; font-weight: 600; color: #374151; display: block; margin-bottom: 6px; }
.dim-comment { font-size: 12px; color: #6b7280; margin: 4px 0 0; }
</style>
