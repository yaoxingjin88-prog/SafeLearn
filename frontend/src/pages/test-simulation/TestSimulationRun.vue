<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VideoPlay, VideoPause, ArrowLeft } from '@element-plus/icons-vue'
import { useTestSimulation } from '@/test-simulation/composables/useTestSimulation'
import { guangzhou614Scenario } from '@/test-simulation/scenarios/guangzhou614'
import RiskIndexBar from '@/test-simulation/components/RiskIndexBar.vue'
import SimTimeline from '@/test-simulation/components/SimTimeline.vue'
import TestScene3D from '@/test-simulation/components/TestScene3D.vue'
import TelemetryPanel from '@/test-simulation/components/TelemetryPanel.vue'
import AiMentorPanel from '@/test-simulation/components/AiMentorPanel.vue'
import DecisionModal from '@/test-simulation/components/DecisionModal.vue'
import EvidenceChain from '@/test-simulation/components/EvidenceChain.vue'
import SimDebrief from '@/test-simulation/components/SimDebrief.vue'
import { useAppBase } from '@/composables/useAppBase'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()
const code = (route.params.code as string) || 'guangzhou_614'
const scenario = guangzhou614Scenario
const playing = ref(true)

const {
  state, report, mentorReply,
  setup, start, pause, resume, setSpeed, choose, askMentor, restart,
} = useTestSimulation(code)

onMounted(() => setup())

const current = computed(() => {
  const h = state.value?.telemetryHistory
  return h?.[h.length - 1] ?? scenario.nodes[0].telemetry
})

const narrative = computed(() =>
  scenario.nodes.find(n => n.key === state.value?.currentNodeKey)?.narrative ?? '',
)

const showDecision = computed(() => state.value?.status === 'decision' && state.value.pendingDecision)
const showDebrief = computed(() => state.value?.status === 'finished' && report.value)
const isRunning = computed(() => state.value?.status === 'running' && !state.value?.paused)
const mentorFaqs = scenario.mentorFaqs.map(f => f.q)

function togglePlay() {
  if (!state.value) return
  if (state.value.status === 'idle') { start(); playing.value = true }
  else if (state.value.paused) { resume(); playing.value = true }
  else { pause(); playing.value = false }
}

function onTelemetryToggle() {
  if (state.value?.paused) resume()
  else pause()
  playing.value = !playing.value
}

function goBack() {
  router.push({ name: 'UserScenarioList' })
}
</script>

<template>
  <div class="test-sim-page">
    <header class="cockpit-header">
      <button class="back" @click="goBack"><el-icon><ArrowLeft /></el-icon> 退出推演</button>
      <div class="title-block">
        <h1>{{ scenario.title }}</h1>
        <p>{{ scenario.subtitle }}</p>
      </div>
      <RiskIndexBar v-if="state" :index="state.riskIndex" :level="state.riskLevel" />
    </header>

    <SimDebrief
      v-if="showDebrief && report"
      :report="report"
      :scenario="scenario"
      @restart="restart"
      @back="goBack"
    />

    <template v-else-if="state">
      <div class="cockpit-body">
        <aside class="col-tl">
          <SimTimeline
            :nodes="scenario.nodes"
            :current-key="state.currentNodeKey"
            :phase="state.phase"
            :offset-min="state.offsetMin"
          />
        </aside>
        <main class="col-main">
          <div class="scene-bar">
            <span class="clock">{{ state.clock }}</span>
            <span class="phase">{{ state.phase }}</span>
            <div class="ctrl">
              <span class="progress-hint">模拟进度 {{ Math.round(state.offsetMin) }}/{{ scenario.durationMinutes }} 分</span>
              <el-select :model-value="state.playbackSpeed" size="small" style="width:72px" @change="setSpeed">
                <el-option :value="0.5" label="0.5x" />
                <el-option :value="1" label="1x" />
                <el-option :value="2" label="2x" />
              </el-select>
              <el-button :icon="isRunning ? VideoPause : VideoPlay" size="small" type="primary" @click="togglePlay">
                {{ state.status === 'idle' ? '开始推演' : isRunning ? '暂停' : '继续' }}
              </el-button>
            </div>
          </div>
          <p class="narrative">{{ narrative }}</p>
          <div class="scene-wrap">
            <TestScene3D :scene="state.scene" :risk-index="state.riskIndex" />
            <DecisionModal
              v-if="showDecision && state.pendingDecision"
              :gate="state.pendingDecision"
              @choose="choose"
            />
          </div>
          <TelemetryPanel
            :history="state.telemetryHistory"
            :current="current"
            :playing="playing"
            @toggle-play="onTelemetryToggle"
          />
        </main>
        <aside class="col-ev">
          <EvidenceChain :items="scenario.evidence" :unlocked="state.evidenceUnlocked" />
        </aside>
      </div>
      <AiMentorPanel :reply="mentorReply" :faqs="mentorFaqs" @ask="askMentor" />
    </template>
  </div>
</template>

<style scoped>
.test-sim-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  margin: 0;
  overflow: hidden;
  background: #030712;
  color: #e2e8f0;
}
.cockpit-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding: 10px 16px;
  background: linear-gradient(90deg, #061226, #0c1a30);
  border-bottom: 1px solid rgba(14, 165, 233, 0.25);
}
.back {
  display: flex; align-items: center; gap: 4px;
  background: transparent; border: 1px solid #334155; color: #94a3b8;
  padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 12px;
}
.title-block { flex: 1; min-width: 0; }
.title-block h1 {
  margin: 0;
  font-size: 14px;
  color: #f1f5f9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.title-block p {
  margin: 2px 0 0;
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cockpit-body {
  display: grid;
  grid-template-columns: 220px 1fr 190px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.col-tl, .col-ev { overflow-y: auto; }
.col-main { display: flex; flex-direction: column; padding: 10px; gap: 8px; min-height: 0; }
.scene-bar {
  display: flex; align-items: center; gap: 12px;
}
.clock { font-family: monospace; font-size: 22px; color: #38bdf8; font-weight: 700; }
.phase { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: rgba(249, 115, 22, 0.2); color: #fb923c; text-transform: uppercase; }
.ctrl { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.progress-hint { font-size: 11px; color: #64748b; font-family: monospace; }
.narrative { margin: 0; font-size: 12px; color: #94a3b8; line-height: 1.5; }
.scene-wrap { position: relative; flex: 1; min-height: 200px; border: 1px solid rgba(14, 165, 233, 0.2); border-radius: 8px; overflow: hidden; }
@media (max-width: 1000px) {
  .cockpit-body { grid-template-columns: 1fr; height: auto; }
}
</style>
