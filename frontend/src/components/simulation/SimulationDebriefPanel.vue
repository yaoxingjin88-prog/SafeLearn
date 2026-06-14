<script setup lang="ts">
import { computed } from 'vue'

export interface DebriefDimension {
  key: string
  label: string
  score: number
  max?: number
}

export interface DebriefRecommendation {
  title: string
  reason?: string
}

export interface SessionDebrief {
  totalScore?: number
  rating?: string
  ratingLabel?: string
  instructorComment?: string
  branch?: string
  outcome?: string
  dimensions?: DebriefDimension[]
  strengths?: string[]
  weaknesses?: string[]
  fiveWhy?: string[]
  recommendations?: DebriefRecommendation[]
}

const props = defineProps<{
  debrief: SessionDebrief
  scenarioName?: string
  compact?: boolean
}>()

const ratingDisplay = computed(() => {
  if (props.debrief.ratingLabel) return props.debrief.ratingLabel
  const map: Record<string, string> = {
    excellent: '优秀', good: '良好', average: '合格', poor: '待提升',
  }
  return map[props.debrief.rating ?? ''] || props.debrief.rating || '—'
})

const outcomeLabel = computed(() => {
  const o = props.debrief.outcome
  const map: Record<string, string> = {
    contained: '遏制成功', partial_loss: '部分损失', major_accident: '重大事故',
    controlled: '事故受控', partial: '部分损失', catastrophic: '闪爆事故',
    success: '事态受控', failure: '事故扩大',
  }
  return map[o ?? ''] || o || '—'
})
</script>

<template>
  <div class="debrief-panel" :class="{ compact }">
    <div class="score-hero">
      <div class="score-ring">
        <span class="score-num">{{ debrief.totalScore ?? '—' }}</span>
        <span class="score-label">综合得分</span>
      </div>
      <div class="hero-text">
        <h2>教官点评 · {{ ratingDisplay }}</h2>
        <p v-if="scenarioName" class="scenario-name">{{ scenarioName }}</p>
        <p class="comment">{{ debrief.instructorComment || '暂无总评内容' }}</p>
        <div v-if="debrief.branch || debrief.outcome" class="outcome-tags">
          <span v-if="debrief.branch" class="tag">分支：{{ debrief.branch }}</span>
          <span v-if="debrief.outcome" class="tag">结局：{{ outcomeLabel }}</span>
        </div>
      </div>
    </div>

    <div v-if="debrief.dimensions?.length" class="grid-2">
      <section>
        <h3>四维评分</h3>
        <div v-for="d in debrief.dimensions" :key="d.key" class="dim-row">
          <span>{{ d.label }}</span>
          <div class="bar-track"><div class="bar-fill" :style="{ width: Math.min(100, d.score) + '%' }" /></div>
          <span class="dim-score">{{ d.score }}</span>
        </div>
      </section>
      <section v-if="debrief.strengths?.length || debrief.weaknesses?.length">
        <h3>亮点与不足</h3>
        <ul v-if="debrief.strengths?.length" class="bullet ok">
          <li v-for="(s, i) in debrief.strengths" :key="'s' + i">{{ s }}</li>
        </ul>
        <ul v-if="debrief.weaknesses?.length" class="bullet warn">
          <li v-for="(w, i) in debrief.weaknesses" :key="'w' + i">{{ w }}</li>
        </ul>
      </section>
    </div>

    <div v-if="!compact && (debrief.fiveWhy?.length || debrief.recommendations?.length)" class="grid-2">
      <section v-if="debrief.fiveWhy?.length">
        <h3>5Why 根因链</h3>
        <ol class="why-list">
          <li v-for="(w, i) in debrief.fiveWhy" :key="i">{{ w }}</li>
        </ol>
      </section>
      <section v-if="debrief.recommendations?.length">
        <h3>推荐课程</h3>
        <div v-for="(c, i) in debrief.recommendations" :key="i" class="course-card">
          <strong>{{ c.title }}</strong>
          <p v-if="c.reason">{{ c.reason }}</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.debrief-panel {
  color: #e2e8f0;
  padding: 4px;
}
.score-hero {
  display: flex;
  gap: 20px;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #0c1a30, #061226);
  border-radius: 12px;
  border: 1px solid rgba(14, 165, 233, 0.3);
  margin-bottom: 16px;
}
.score-ring {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 3px solid #0ea5e9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 0 24px rgba(14, 165, 233, 0.2);
}
.score-num {
  font-size: 28px;
  font-weight: 800;
  color: #38bdf8;
  line-height: 1;
}
.score-label {
  font-size: 10px;
  color: #64748b;
  margin-top: 4px;
}
.hero-text { flex: 1; min-width: 0; }
.hero-text h2 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #f1f5f9;
}
.scenario-name {
  margin: 0 0 6px;
  font-size: 12px;
  color: #64748b;
}
.comment {
  margin: 0;
  font-size: 14px;
  color: #94a3b8;
  line-height: 1.7;
}
.outcome-tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 6px;
  background: rgba(14, 165, 233, 0.12);
  color: #7dd3fc;
  border: 1px solid rgba(14, 165, 233, 0.25);
}
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}
@media (max-width: 640px) {
  .grid-2 { grid-template-columns: 1fr; }
}
section {
  background: #0a1628;
  border: 1px solid #1e3a5f;
  border-radius: 10px;
  padding: 14px;
}
section h3 {
  margin: 0 0 10px;
  font-size: 12px;
  color: #64748b;
  letter-spacing: 0.5px;
}
.dim-row {
  display: grid;
  grid-template-columns: 72px 1fr 32px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}
.bar-track {
  height: 6px;
  background: #1e293b;
  border-radius: 3px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1d4ed8, #38bdf8);
  border-radius: 3px;
}
.dim-score {
  text-align: right;
  color: #38bdf8;
  font-weight: 600;
  font-family: Consolas, monospace;
}
.bullet {
  margin: 0 0 8px;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.6;
}
.bullet.ok { color: #34d399; }
.bullet.warn { color: #fca5a5; }
.why-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.7;
}
.course-card {
  padding: 8px 0;
  border-bottom: 1px solid #1e293b;
  font-size: 13px;
}
.course-card:last-child { border-bottom: none; }
.course-card strong { color: #e2e8f0; display: block; margin-bottom: 4px; }
.course-card p { margin: 0; color: #64748b; font-size: 12px; }

.compact .score-hero { padding: 14px; }
.compact .score-ring { width: 72px; height: 72px; }
.compact .score-num { font-size: 22px; }
</style>
