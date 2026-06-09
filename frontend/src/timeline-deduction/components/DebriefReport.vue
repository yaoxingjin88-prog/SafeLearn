<script setup lang="ts">
import type { DebriefReport } from '../types'

defineProps<{
  report: DebriefReport
}>()

const emit = defineEmits<{
  restart: []
  back: []
}>()
</script>

<template>
  <div class="debrief">
    <div class="score-hero">
      <div class="score-ring">
        <span class="score-num">{{ report.totalScore }}</span>
        <span class="score-label">综合得分</span>
      </div>
      <div class="hero-text">
        <h2>教官点评 · {{ report.rating }}</h2>
        <p>{{ report.instructorComment }}</p>
        <div class="outcome-tags">
          <span class="tag">分支：{{ report.branch }}</span>
          <span class="tag">结局：{{ report.outcome }}</span>
        </div>
      </div>
    </div>

    <div class="grid-2">
      <section>
        <h3>四维评分</h3>
        <div v-for="d in report.dimensions" :key="d.key" class="dim-row">
          <span>{{ d.label }}</span>
          <div class="bar-track"><div class="bar-fill" :style="{ width: d.score + '%' }" /></div>
          <span class="dim-score">{{ d.score }}</span>
        </div>
      </section>
      <section>
        <h3>亮点与不足</h3>
        <ul class="bullet ok">
          <li v-for="(s, i) in report.strengths" :key="'s' + i">{{ s }}</li>
        </ul>
        <ul class="bullet warn">
          <li v-for="(w, i) in report.weaknesses" :key="'w' + i">{{ w }}</li>
        </ul>
      </section>
    </div>

    <div class="grid-2">
      <section>
        <h3>5Why 根因链</h3>
        <ol class="why-list">
          <li v-for="(w, i) in report.fiveWhy" :key="i">{{ w }}</li>
        </ol>
      </section>
      <section>
        <h3>推荐课程</h3>
        <div v-for="(c, i) in report.recommendations" :key="i" class="course-card">
          <strong>{{ c.title }}</strong>
          <p>{{ c.reason }}</p>
        </div>
      </section>
    </div>

    <div class="actions">
      <el-button @click="emit('back')">返回列表</el-button>
      <el-button type="primary" @click="emit('restart')">重新推演</el-button>
    </div>
  </div>
</template>

<style scoped>
.debrief {
  padding: 20px;
  color: #e2e8f0;
}
.score-hero {
  display: flex;
  gap: 24px;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #1e3a5f, #0f172a);
  border-radius: 16px;
}
.score-ring {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid #38bdf8;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.score-num {
  font-size: 36px;
  font-weight: 800;
  color: #38bdf8;
}
.score-label {
  font-size: 12px;
  color: #94a3b8;
}
.hero-text h2 {
  margin: 0 0 8px;
  font-size: 18px;
}
.hero-text p {
  margin: 0;
  line-height: 1.6;
  color: #cbd5e1;
}
.outcome-tags {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
.tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.15);
  color: #7dd3fc;
}
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
section {
  background: #1e293b;
  border-radius: 12px;
  padding: 16px;
}
section h3 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #94a3b8;
}
.dim-row {
  display: grid;
  grid-template-columns: 72px 1fr 36px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}
.bar-track {
  height: 8px;
  background: #334155;
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #38bdf8);
  border-radius: 4px;
}
.dim-score {
  text-align: right;
  font-weight: 600;
}
.bullet {
  margin: 0 0 12px;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.6;
}
.bullet.ok { color: #86efac; }
.bullet.warn { color: #fca5a5; }
.why-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.7;
  color: #cbd5e1;
}
.course-card {
  background: #0f172a;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.course-card p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
@media (max-width: 900px) {
  .grid-2 { grid-template-columns: 1fr; }
  .score-hero { flex-direction: column; text-align: center; }
}
</style>
