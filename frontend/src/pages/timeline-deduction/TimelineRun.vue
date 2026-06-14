<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VideoPlay, VideoPause, ArrowLeft } from '@element-plus/icons-vue'
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
  setup,
  start,
  pause,
  resume,
  setSpeed,
  choose,
  restart,
} = useTimelineDeduction(code)

onMounted(() => setup())

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

function goBack() {
  router.push({ name: 'UserScenarioList' })
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
        <button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 退出推演
        </button>
        <div class="toolbar-text">
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
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #0f172a;
  padding: 12px 16px;
  box-sizing: border-box;
}
.mb-3 { margin-bottom: 12px; }
.run-toolbar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-shrink: 0;
  margin-bottom: 12px;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  background: transparent;
  border: 1px solid #334155;
  color: #94a3b8;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
}
.toolbar-text {
  flex: 1;
  min-width: 0;
}
.run-toolbar h2 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #f1f5f9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  margin-left: auto;
}
.risk {
  font-size: 13px;
  font-weight: 600;
  color: #f97316;
}
.run-grid {
  display: grid;
  grid-template-columns: 260px 1fr 300px;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
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
