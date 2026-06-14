<script setup lang="ts">
import type { SimScoreReport, TestSimScenario } from '../types'

defineProps<{
  report: SimScoreReport
  scenario: TestSimScenario
}>()
const emit = defineEmits<{ restart: []; back: [] }>()
</script>

<template>
  <div class="debrief">
    <div class="hero">
      <div class="score-ring">{{ report.totalScore }}</div>
      <div>
        <h2>{{ report.reportTitle }}</h2>
        <p>{{ report.instructorSummary }}</p>
        <el-tag type="success">{{ report.rating }}</el-tag>
      </div>
    </div>
    <div class="grid">
      <section>
        <h3>四维能力评分</h3>
        <div v-for="d in report.dimensions" :key="d.key" class="dim">
          <span>{{ d.label }}</span>
          <div class="bar"><i :style="{ width: d.score + '%' }" /></div>
          <span>{{ d.score }}</span>
        </div>
      </section>
      <section>
        <h3>5Why 根因链</h3>
        <ol><li v-for="(w, i) in scenario.fiveWhy" :key="i">{{ w }}</li></ol>
      </section>
    </div>
    <div class="grid">
      <section>
        <h3>事故树 FTA</h3>
        <div v-for="node in scenario.fta" :key="node.label" class="fta">
          <div class="fta-root">{{ node.label }}</div>
          <div v-if="node.children" class="fta-children">
            <span v-for="c in node.children" :key="c">↳ {{ c }}</span>
          </div>
        </div>
      </section>
      <section>
        <h3>推荐课程</h3>
        <div v-for="c in report.courseLinks" :key="c.title" class="course">
          <strong>{{ c.title }}</strong>
          <p>{{ c.reason }}</p>
        </div>
      </section>
    </div>
    <div class="actions">
      <el-button @click="emit('back')">返回场景</el-button>
      <el-button type="primary" @click="emit('restart')">重新推演</el-button>
    </div>
  </div>
</template>

<style scoped>
.debrief { padding: 20px; color: #e2e8f0; max-width: 1000px; margin: 0 auto; }
.hero {
  display: flex; gap: 20px; align-items: center; padding: 20px;
  background: linear-gradient(135deg, #0c1a30, #061226); border-radius: 12px;
  border: 1px solid rgba(14, 165, 233, 0.3); margin-bottom: 16px;
}
.score-ring {
  width: 100px; height: 100px; border-radius: 50%; border: 3px solid #0ea5e9;
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; font-weight: 800; color: #38bdf8;
}
.hero h2 { margin: 0 0 8px; font-size: 16px; }
.hero p { margin: 0 0 8px; font-size: 13px; color: #94a3b8; line-height: 1.6; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px; }
section {
  background: #0a1628; border: 1px solid #1e3a5f; border-radius: 10px; padding: 14px;
}
section h3 { margin: 0 0 10px; font-size: 12px; color: #64748b; }
.dim { display: grid; grid-template-columns: 72px 1fr 32px; gap: 8px; align-items: center; margin-bottom: 6px; font-size: 12px; }
.bar { height: 6px; background: #1e293b; border-radius: 3px; overflow: hidden; }
.bar i { display: block; height: 100%; background: linear-gradient(90deg, #0ea5e9, #22d3ee); }
ol { margin: 0; padding-left: 18px; font-size: 12px; line-height: 1.7; color: #cbd5e1; }
.fta { margin-bottom: 8px; }
.fta-root { font-weight: 600; color: #f97316; font-size: 13px; }
.fta-children { display: flex; flex-direction: column; gap: 4px; margin-left: 12px; font-size: 11px; color: #94a3b8; }
.course { background: #061226; padding: 8px; border-radius: 6px; margin-bottom: 6px; }
.course p { margin: 4px 0 0; font-size: 11px; color: #64748b; }
.actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
@media (max-width: 768px) { .grid { grid-template-columns: 1fr; } }
</style>
