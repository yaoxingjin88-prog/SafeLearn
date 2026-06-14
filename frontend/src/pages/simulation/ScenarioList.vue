<template>
  <div class="sl-page scenario-list">
    <div class="sl-page-head">
      <h2 class="sl-page-title">事故推演场景</h2>
      <p class="sl-page-desc">测试作业推演 · 时间轴推演 · 经典分级训练（关联真实案例）</p>
    </div>

    <!-- 测试作业推演 -->
    <section class="timeline-section">
      <div class="section-label">
        <el-tag type="warning" effect="dark" size="small">数字孪生</el-tag>
        <span>储能测试作业事故推演 · 分支决策 · 证据链 · AI导师</span>
      </div>
      <div class="timeline-card test-card" @click="startTest(guangzhou614Scenario.code)">
        <div class="timeline-visual test-visual" :style="coverStyle(SCENARIO_COVERS.guangzhou614)">
          <div class="visual-overlay" />
          <div class="phase-strip">
            <span v-for="(step, i) in testPhaseSteps" :key="i">{{ step }}</span>
          </div>
        </div>
        <div class="timeline-body">
          <div class="timeline-tags">
            <el-tag type="danger" size="small">特别重大</el-tag>
            <el-tag size="small">30 min</el-tag>
            <el-tag type="info" size="small">3 决策点</el-tag>
          </div>
          <h3 class="timeline-title">{{ guangzhou614Scenario.title }}</h3>
          <p class="timeline-sub">{{ guangzhou614Scenario.subtitle }}</p>
          <ul class="timeline-meta">
            <li>地点：{{ guangzhou614Scenario.location }}</li>
            <li>日期：{{ guangzhou614Scenario.accidentDate }}</li>
            <li>关联案例库 · 多结局分支推演</li>
          </ul>
          <div class="classic-actions" @click.stop>
            <el-button type="primary" size="large" @click="startTest(guangzhou614Scenario.code)">
              开始推演
            </el-button>
            <el-button
              size="large"
              @click="router.push(p(`/cases/${guangzhou614Scenario.caseId}`))"
            >
              查看案例
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <!-- 时间轴推演 -->
    <section class="timeline-section">
      <div class="section-label">
        <el-tag type="danger" effect="dark" size="small">推荐</el-tag>
        <span>时间轴推演 · 六阶段决策 · 3D 可视化</span>
      </div>
      <div class="timeline-card" @click="startTimeline(beijing416Scenario.code)">
        <div class="timeline-visual" :style="coverStyle(SCENARIO_COVERS.beijing416)">
          <div class="visual-overlay" />
          <div class="phase-strip">
            <span v-for="(step, i) in phaseSteps" :key="i">{{ step }}</span>
          </div>
        </div>
        <div class="timeline-body">
          <div class="timeline-tags">
            <el-tag type="danger" size="small">高级</el-tag>
            <el-tag size="small">45 min</el-tag>
            <el-tag type="info" size="small">3 决策点</el-tag>
          </div>
          <h3 class="timeline-title">{{ beijing416Scenario.title }}</h3>
          <p class="timeline-sub">{{ beijing416Scenario.subtitle }}</p>
          <ul class="timeline-meta">
            <li>地点：{{ beijing416Scenario.location }}</li>
            <li>日期：{{ beijing416Scenario.accidentDate }}</li>
            <li>多结局分支 · AI 教官评分</li>
          </ul>
          <div class="classic-actions" @click.stop>
            <el-button type="primary" size="large" @click="startTimeline(beijing416Scenario.code)">
              开始推演
            </el-button>
            <el-button
              size="large"
              @click="router.push(p(`/cases/${beijing416Scenario.caseId}`))"
            >
              查看案例
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <!-- 经典分级推演 -->
    <section v-if="scenarios.length" class="classic-section">
      <div class="section-label">
        <el-tag type="success" effect="plain" size="small">分级训练</el-tag>
        <span>经典推演场景 · L1→L3 递进 · 关联真实事故案例</span>
      </div>
      <div class="classic-grid">
        <div
          v-for="scenario in scenarios"
          :key="scenario.id"
          class="timeline-card classic-card"
          :class="`tier-${getMeta(scenario.id)?.tier ?? 'basic'}`"
          @click="router.push(p(`/simulation/${scenario.id}`))"
        >
          <div
            class="timeline-visual classic-visual"
            :style="coverStyle(getClassicCover(scenario.id))"
          >
            <div class="visual-overlay" />
            <div class="phase-strip">
              <span v-for="(step, i) in (getMeta(scenario.id)?.phaseSteps ?? defaultPhaseSteps)" :key="i">{{ step }}</span>
            </div>
          </div>
          <div class="timeline-body">
            <div class="timeline-tags">
              <el-tag :type="getDifficultyType(scenario.difficulty)" size="small">
                {{ getMeta(scenario.id)?.tierLabel ?? getDifficultyName(scenario.difficulty) }}
              </el-tag>
              <el-tag size="small">{{ scenario.duration }}s</el-tag>
              <el-tag type="info" size="small">{{ scenario.initialConditions.batteryCount }} 电池</el-tag>
              <el-tag v-if="deductionMode(scenario)" type="warning" size="small">决策推演</el-tag>
            </div>
            <h3 class="timeline-title">{{ scenario.name }}</h3>
            <p class="timeline-sub">{{ getMeta(scenario.id)?.subtitle ?? scenario.description }}</p>
            <ul v-if="getMeta(scenario.id)" class="timeline-meta">
              <li>参考案例：{{ getMeta(scenario.id)!.caseTitle }}</li>
              <li>地点：{{ getMeta(scenario.id)!.location }}</li>
              <li>日期：{{ getMeta(scenario.id)!.accidentDate }}</li>
            </ul>
            <div class="classic-actions" @click.stop>
              <el-button type="primary" size="large" @click="router.push(p(`/simulation/${scenario.id}`))">
                开始推演
              </el-button>
              <el-button
                v-if="getMeta(scenario.id)"
                size="large"
                @click="router.push(p(`/cases/${getMeta(scenario.id)!.caseId}`))"
              >
                查看案例
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import { beijing416Scenario } from '@/timeline-deduction/scenarios/beijing416'
import { guangzhou614Scenario } from '@/test-simulation/scenarios/guangzhou614'
import { getClassicMeta } from '@/simulation/classicScenarioMeta'
import { isClassicDeductionScenario } from '@/simulation/scenarios'
import { SCENARIO_COVERS, getClassicCover } from '@/simulation/scenarioCovers'
import type { AccidentScenario } from '@/types'

const router = useRouter()
const { p } = useAppBase()

const scenarios = ref<AccidentScenario[]>([])
const phaseSteps = ['正常运行', '异常预警', '热失控', '扩散', '爆炸', '复盘']
const testPhaseSteps = ['测试背景', '测试作业', '异常征兆', '热失控', '闪爆', '应急处置', '调查复盘']
const defaultPhaseSteps = ['监控', '预警', '热失控', '扩散', '处置']

function isPlayable(s: AccidentScenario) {
  const name = (s.name || '').trim().toLowerCase()
  if (!name || name === 'test' || name.startsWith('test_')) return false
  const ic = s.initialConditions as Record<string, unknown> | undefined
  if (ic?.trainingKind === 'emergency_case') return false
  const count = s.initialConditions?.batteryCount ?? 0
  return count > 0 && (s.duration ?? 0) > 0
}

function getMeta(id: string) {
  return getClassicMeta(id)
}

function coverStyle(src?: string) {
  if (!src) return undefined
  return {
    backgroundImage: `url(${src})`,
  }
}

function deductionMode(s: AccidentScenario) {
  return isClassicDeductionScenario(s.id)
}

onMounted(async () => {
  const res = await request.get('/simulation/scenarios')
  scenarios.value = (res.data as AccidentScenario[]).filter(isPlayable)
})

function startTest(code: string) {
  router.push(p(`/simulation/test/${code}`))
}

function startTimeline(code: string) {
  router.push(p(`/simulation/timeline/${code}`))
}

function getDifficultyType(difficulty: string) {
  const map: Record<string, string> = {
    easy: 'success',
    medium: 'warning',
    hard: 'danger',
    '1': 'success',
    '2': 'warning',
    '3': 'danger',
  }
  return map[String(difficulty)] || 'info'
}

function getDifficultyName(difficulty: string) {
  const map: Record<string, string> = {
    easy: '入门 · L1',
    medium: '进阶 · L2',
    hard: '高级 · L3',
    '1': '入门 · L1',
    '2': '进阶 · L2',
    '3': '高级 · L3',
  }
  return map[String(difficulty)] || String(difficulty)
}
</script>

<style scoped>
.sl-page-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: #64748b;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

.timeline-section,
.classic-section {
  margin-bottom: 32px;
}

.timeline-card {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  background: #fff;
  transition: box-shadow 0.2s, transform 0.2s;
}

.timeline-card:hover {
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.1);
  transform: translateY(-2px);
}

.test-visual {
  background-color: #0a1628;
}
.test-card {
  border-color: rgba(14, 165, 233, 0.35);
}

.classic-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.classic-card.tier-basic {
  border-color: rgba(16, 185, 129, 0.35);
}
.classic-card.tier-intermediate {
  border-color: rgba(245, 158, 11, 0.35);
}
.classic-card.tier-advanced {
  border-color: rgba(239, 68, 68, 0.35);
}

.classic-visual {
  background-color: #0f172a;
  min-height: 140px;
}

.timeline-visual {
  position: relative;
  background-color: #0f172a;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  padding: 24px;
  display: flex;
  align-items: flex-end;
  min-height: 200px;
}

.visual-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(15, 23, 42, 0.15) 0%,
    rgba(15, 23, 42, 0.55) 55%,
    rgba(15, 23, 42, 0.88) 100%
  );
  pointer-events: none;
}

.phase-strip {
  position: relative;
  z-index: 1;
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

.timeline-body {
  padding: 24px;
}

.timeline-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.timeline-title {
  margin: 12px 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.timeline-sub {
  margin: 0 0 12px;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.timeline-meta {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
  font-size: 13px;
  color: #475569;
  line-height: 1.8;
}

.classic-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .timeline-card {
    grid-template-columns: 1fr;
  }
}
</style>
