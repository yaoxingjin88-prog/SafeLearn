<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { useTimelineDeduction } from '@/timeline-deduction/composables/useTimelineDeduction'
import { beijing416Scenario } from '@/timeline-deduction/scenarios/beijing416'
import TimelinePanel from '@/timeline-deduction/components/TimelinePanel.vue'
import DeductionScene3D from '@/timeline-deduction/components/DeductionScene3D.vue'
import TelemetryCharts from '@/timeline-deduction/components/TelemetryCharts.vue'
import DecisionOverlay from '@/timeline-deduction/components/DecisionOverlay.vue'
import DebriefReport from '@/timeline-deduction/components/DebriefReport.vue'

const route = useRoute()
const router = useRouter()
const code = (route.params.code as string) || 'beijing_416'

const {
  state,
  report,
  loading,
  error,
  initSession,
  start,
  pause,
  resume,
  setSpeed,
  choose,
  destroy,
} = useTimelineDeduction(code)

onMounted(() => initSession())

const scenario = beijing416Scenario

const currentNode = computed(() => {
  const key = state.value?.currentNodeKey
  return scenario.nodes.find(n => n.key === key) ?? scenario.nodes[0]
})

const isRunning = computed(() => state.value?.status === 'running' && !state.value?.paused)
const showDecision = computed(() => state.value?.status === 'decision' && state.value?.pendingDecision)
const showDebrief = computed(() => state.value?.status === 'finished' && report.value)

function togglePlay() {
  if (!state.value) return
  if (state.value.status === 'idle') start()
  else if (state.value.paused) resume()
  else pause()
}

async function restart() {
  destroy()
  await initSession()
  start()
}

function goBack() {
  router.push({ name: 'TimelineDeductionHub' })
}
</script>

<template>
  <div v-loading="loading" class="run-page">
    <el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" class="mb-3" />

    <DebriefReport
      v-if="showDebrief && report"
      :report="report"
      @restart="restart"
      @back="goBack"
    />

    <template v-else-if="state">
      <div class="run-toolbar">
        <div>
          <h2>{{ scenario.title }}</h2>
          <p class="narrative">{{ currentNode.narrative }}</p>
        </div>
        <div class="toolbar-actions">
          <span class="risk">风险指数 {{ state.riskIndex }}</span>
          <el-select :model-value="state.playbackSpeed" size="small" style="width: 88px" @change="setSpeed">
            <el-option :value="0.5" label="0.5x" />
            <el-option :value="1" label="1x" />
            <el-option :value="2" label="2x" />
            <el-option :value="4" label="4x" />
          </el-select>
          <el-button :icon="isRunning ? VideoPause : VideoPlay" type="primary" @click="togglePlay">
            {{ state.status === 'idle' ? '开始' : isRunning ? '暂停' : '继续' }}
          </el-button>
        </div>
      </div>

      <div class="run-grid">
        <aside class="col-left">
          <TimelinePanel
            :nodes="scenario.nodes"
            :current-key="state.currentNodeKey"
            :phase="state.phase"
            :offset-min="state.offsetMin"
          />
        </aside>
        <main class="col-center">
          <div class="scene-wrap">
            <DeductionScene3D :scene="state.scene" :phase="state.phase" />
            <DecisionOverlay
              v-if="showDecision && state.pendingDecision"
              :gate="state.pendingDecision"
              @choose="choose"
            />
          </div>
        </main>
        <aside class="col-right">
          <div class="charts-title">实时遥测</div>
          <TelemetryCharts :history="state.telemetryHistory" />
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.run-page {
  min-height: calc(100vh - 140px);
}
.mb-3 { margin-bottom: 12px; }
.run-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}
.run-toolbar h2 {
  margin: 0 0 4px;
  font-size: 18px;
}
.narrative {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  max-width: 640px;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.risk {
  font-size: 13px;
  font-weight: 600;
  color: #f97316;
}
.run-grid {
  display: grid;
  grid-template-columns: 280px 1fr 320px;
  gap: 12px;
  height: calc(100vh - 220px);
  min-height: 520px;
}
.col-left, .col-right {
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.col-center {
  min-height: 0;
}
.scene-wrap {
  position: relative;
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #1e293b;
}
.charts-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 8px;
}
.col-right {
  background: #0f172a;
  border-radius: 12px;
  padding: 12px;
}
@media (max-width: 1100px) {
  .run-grid {
    grid-template-columns: 1fr;
    height: auto;
  }
  .col-left { max-height: 280px; }
  .scene-wrap { min-height: 360px; }
}
</style>
