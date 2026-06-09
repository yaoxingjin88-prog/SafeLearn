<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { beijing416Scenario } from '@/timeline-deduction/scenarios/beijing416'
import * as api from '@/api/timelineDeduction'

const router = useRouter()
const scenarios = ref([beijing416Scenario])
const records = ref<Record<string, unknown>[]>([])

onMounted(async () => {
  try {
    const list = await api.listScenarios()
    if (Array.isArray(list.data) && list.data.length) scenarios.value = list.data as typeof scenarios.value
  } catch { /* use bundled scenario */ }
  try {
    const res = await api.listSessions()
    records.value = res.data || []
  } catch { /* ignore */ }
})

function start(code: string) {
  router.push({ name: 'TimelineDeductionRun', params: { code } })
}

const phaseSteps = ['正常运行', '异常预警', '热失控', '扩散', '爆炸', '复盘']
</script>

<template>
  <div class="hub">
    <header class="hub-header">
      <div>
        <h1>时间轴事故推演</h1>
        <p>六阶段沉浸式推演 · 三决策点分支 · 3D 可视化 · AI 教官评分</p>
      </div>
      <div class="arch-pill">创新引擎 v1</div>
    </header>

    <div class="scenario-card" @click="start(beijing416Scenario.code)">
      <div class="card-visual">
        <div class="phase-strip">
          <span v-for="(p, i) in phaseSteps" :key="i">{{ p }}</span>
        </div>
      </div>
      <div class="card-body">
        <el-tag type="danger" size="small">高级</el-tag>
        <el-tag size="small" class="ml-2">45 min</el-tag>
        <h2>{{ beijing416Scenario.title }}</h2>
        <p>{{ beijing416Scenario.subtitle }}</p>
        <ul class="meta">
          <li>地点：{{ beijing416Scenario.location }}</li>
          <li>日期：{{ beijing416Scenario.accidentDate }}</li>
          <li>决策点：3 个 · 多结局分支</li>
        </ul>
        <el-button type="primary" size="large">开始推演</el-button>
      </div>
    </div>

    <section v-if="records.length" class="records">
      <h3>最近推演记录</h3>
      <el-table :data="records.slice(0, 5)" stripe>
        <el-table-column prop="scenarioTitle" label="场景" />
        <el-table-column prop="outcome" label="结局" width="120" />
        <el-table-column prop="totalScore" label="得分" width="80" />
        <el-table-column prop="startedAt" label="时间" width="180" />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.hub {
  max-width: 960px;
  margin: 0 auto;
}
.hub-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.hub-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
}
.hub-header p {
  margin: 0;
  color: #64748b;
}
.arch-pill {
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1e40af, #7c3aed);
  color: #fff;
}
.scenario-card {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 0;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: box-shadow 0.2s, transform 0.2s;
  background: #fff;
}
.scenario-card:hover {
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.12);
  transform: translateY(-2px);
}
.card-visual {
  background: linear-gradient(160deg, #0f172a, #1e3a5f);
  padding: 24px;
  display: flex;
  align-items: flex-end;
}
.phase-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.phase-strip span {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 6px;
  background: rgba(56, 189, 248, 0.15);
  color: #7dd3fc;
}
.card-body {
  padding: 24px;
}
.card-body h2 {
  margin: 12px 0 8px;
  font-size: 18px;
}
.card-body p {
  color: #64748b;
  font-size: 14px;
  margin: 0 0 12px;
}
.meta {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
  font-size: 13px;
  color: #475569;
  line-height: 1.8;
}
.records {
  margin-top: 32px;
}
.records h3 {
  margin-bottom: 12px;
}
.ml-2 { margin-left: 8px; }
@media (max-width: 768px) {
  .scenario-card { grid-template-columns: 1fr; }
}
</style>
