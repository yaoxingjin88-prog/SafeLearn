<template>
  <div class="classic-sim-page">
    <header class="cockpit-header">
      <button class="back" @click="goBack"><el-icon><ArrowLeft /></el-icon> 退出推演</button>
      <div class="title-block">
        <h1>{{ displayName }}</h1>
        <p v-if="caseMeta">{{ caseMeta.subtitle }}</p>
        <p v-else class="muted">{{ apiScenario.description }}</p>
      </div>
      <RiskIndexBar :index="riskIndex" :level="riskLevel" />
    </header>

    <div class="cockpit-body">
      <aside class="col-side">
        <section class="panel">
          <h3>场景信息</h3>
          <div class="info-list">
            <div class="info-item">
              <span class="label">电池数量</span>
              <span class="value">{{ scenarioInfo.batteryCount }}</span>
            </div>
            <div class="info-item">
              <span class="label">初始温度</span>
              <span class="value">{{ scenarioInfo.initialTemperature }}°C</span>
            </div>
            <div class="info-item">
              <span class="label">当前阶段</span>
              <span class="value phase-val">{{ phaseLabel }}</span>
            </div>
            <div v-if="deductionMode && context" class="info-item">
              <span class="label">当前得分</span>
              <span class="value score-val">{{ context.score }}</span>
            </div>
            <div class="info-item">
              <span class="label">推演时长</span>
              <span class="value">{{ totalDuration }}s</span>
            </div>
          </div>
        </section>

        <section v-if="caseMeta" class="panel case-ref">
          <h3>参考案例</h3>
          <p class="case-title">{{ caseMeta.caseTitle }}</p>
          <p class="case-loc">{{ caseMeta.location }} · {{ caseMeta.accidentDate }}</p>
          <el-button size="small" text type="primary" @click="openCase">查看案例详情</el-button>
        </section>

        <section class="panel">
          <h3>实时遥测</h3>
          <div class="telemetry-grid">
            <div class="tel-item">
              <span class="tel-label">环境温度</span>
              <span class="tel-val warm">{{ environment.temperature.toFixed(1) }}°C</span>
            </div>
            <div class="tel-item">
              <span class="tel-label">湿度</span>
              <span class="tel-val cool">{{ environment.humidity.toFixed(0) }}%</span>
            </div>
            <div class="tel-item">
              <span class="tel-label">气体浓度</span>
              <span class="tel-val danger">{{ environment.gasLevel.toFixed(0) }}ppm</span>
            </div>
          </div>
        </section>

        <section class="panel temp-panel">
          <h3>温度场</h3>
          <TemperatureField :cells="cells" :cols="Math.min(4, cells.length)" />
        </section>
      </aside>

      <main class="col-main">
        <div class="scene-bar">
          <span class="clock">{{ formatTime(currentTime) }} / {{ formatTime(totalDuration) }}</span>
          <span class="phase-tag" :class="phase">{{ phaseLabel }}</span>
          <el-tag v-if="deductionMode" type="warning" size="small" effect="dark">决策推演</el-tag>
          <div class="ctrl">
            <el-slider
              v-if="!deductionMode"
              :model-value="currentTime"
              :max="totalDuration"
              :format-tooltip="formatTime"
              class="time-slider"
              @input="handleSeek"
            />
            <el-select v-model="speedModel" size="small" style="width:72px" @change="handleSetSpeed">
              <el-option :value="0.5" label="0.5x" />
              <el-option :value="1" label="1x" />
              <el-option :value="2" label="2x" />
              <el-option :value="4" label="4x" />
            </el-select>
            <el-button
              :icon="isPlaying ? VideoPause : VideoPlay"
              size="small"
              type="primary"
              :disabled="!!pendingDecision || isCompleted"
              @click="handleTogglePlay"
            >
              {{ isPlaying ? '暂停' : '播放' }}
            </el-button>
            <el-button size="small" :icon="RefreshRight" @click="handleReset">重置</el-button>
            <el-button
              v-if="scoreReport"
              size="small"
              type="success"
              @click="scoreVisible = true"
            >
              查看评分
            </el-button>
          </div>
        </div>

        <div ref="sceneContainer" class="scene-wrap">
          <BatteryScene :cells="cells" />
          <ParticleSystem
            v-if="cells.length > 1"
            :cells="cells"
            :width="sceneWidth"
            :height="sceneHeight"
          />
          <div class="temperature-overlay">
            <div v-for="cell in cells.slice(0, 8)" :key="cell.id" class="temp-item">
              <div class="temp-indicator" :style="{ background: getTempColor(cell.temperature) }" />
              <span class="temp-value">#{{ cell.id + 1 }} {{ cell.temperature }}°C</span>
            </div>
          </div>
        </div>
      </main>

      <aside class="col-alerts">
        <section class="panel alerts-panel">
          <h3>告警信息 <em>{{ events.length }}</em></h3>
          <div class="alert-list">
            <div
              v-for="(evt, idx) in events"
              :key="idx"
              class="alert-item"
              :class="`alert-${evt.type}`"
            >
              <el-icon><Warning /></el-icon>
              <span class="alert-msg">{{ evt.message }}</span>
              <span class="alert-time">{{ formatTime(evt.time) }}</span>
            </div>
            <div v-if="events.length === 0" class="empty-alerts">暂无告警</div>
          </div>
        </section>
      </aside>
    </div>

    <DecisionPanel
      v-if="deductionMode"
      :decision="pendingDecision"
      :phase="phase"
      @select="handleDecision"
    />

    <ScoreReportDialog v-model="scoreVisible" :report="scoreReport" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Warning, VideoPlay, VideoPause, RefreshRight, ArrowLeft } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import { useSimulation, normalizeScenarioEvents } from '@/composables/useSimulation'
import { useDeductionEngine } from '@/simulation/composables/useDeductionEngine'
import { getClassicMeta } from '@/simulation/classicScenarioMeta'
import { isClassicDeductionScenario } from '@/simulation/scenarios'
import { useClassicLegacySession } from '@/simulation/composables/useClassicLegacySession'
import BatteryScene from '@/components/simulation/BatteryScene.vue'
import ParticleSystem from '@/components/simulation/ParticleSystem.vue'
import TemperatureField from '@/components/simulation/TemperatureField.vue'
import DecisionPanel from '@/components/simulation/DecisionPanel.vue'
import ScoreReportDialog from '@/components/simulation/ScoreReportDialog.vue'
import RiskIndexBar from '@/test-simulation/components/RiskIndexBar.vue'
import type { RiskLevel } from '@/test-simulation/types'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()
const scenarioId = route.params.id as string

const apiScenario = ref<any>({
  id: '', name: '', duration: 120, description: '',
  initialConditions: { batteryCount: 1, initialTemperature: 35, batteryType: '', capacity: 0.1 },
})

const deductionMode = ref(false)
const scoreVisible = ref(false)
const scoreReport = ref<Record<string, any> | null>(null)
const scoreFetched = ref(false)

const sceneContainer = ref<HTMLElement>()
const sceneWidth = ref(600)
const sceneHeight = ref(400)
const speedModel = ref(1)

const legacy = useSimulation()
const deduction = useDeductionEngine(scenarioId)
const legacySession = useClassicLegacySession(scenarioId)

const cells = computed(() => deductionMode.value ? deduction.cells.value : legacy.cells.value)
const events = computed(() => deductionMode.value ? deduction.events.value : legacy.events.value)
const environment = computed(() => deductionMode.value ? deduction.environment.value : legacy.environment.value)
const isPlaying = computed(() => deductionMode.value ? deduction.isPlaying.value : legacy.isPlaying.value)
const currentTime = computed(() => deductionMode.value ? deduction.currentTime.value : legacy.currentTime.value)
const totalDuration = computed(() => deductionMode.value ? deduction.totalDuration.value : legacy.totalDuration.value)
const phase = computed(() => deductionMode.value ? deduction.phase.value : 'demo')
const context = computed(() => deductionMode.value ? deduction.context.value : null)
const pendingDecision = computed(() => deductionMode.value ? deduction.pendingDecision.value : null)
const isCompleted = computed(() => deductionMode.value ? deduction.isCompleted.value : false)

const caseMeta = computed(() => getClassicMeta(scenarioId))
const displayName = computed(() => apiScenario.value.name || '事故推演')
const scenarioInfo = computed(() => ({
  batteryCount: apiScenario.value.initialConditions?.batteryCount ?? 1,
  initialTemperature: apiScenario.value.initialConditions?.initialTemperature ?? 35,
}))

const riskIndex = computed(() => {
  const maxTemp = cells.value.length
    ? Math.max(...cells.value.map(c => c.temperature))
    : scenarioInfo.value.initialTemperature
  const gas = environment.value.gasLevel ?? 0
  return Math.min(100, Math.round((maxTemp - 25) * 1.1 + gas * 0.08))
})

const riskLevel = computed<RiskLevel>(() => {
  const v = riskIndex.value
  if (v < 25) return 'low'
  if (v < 50) return 'medium'
  if (v < 75) return 'high'
  return 'critical'
})

const phaseLabels: Record<string, string> = {
  idle: '待命',
  monitoring: '正常监控',
  earlyWarning: '早期预警',
  venting: '通风处置',
  isolation: '电气隔离',
  thermalRunaway: '热失控',
  fireSuppression: '灭火处置',
  evacuation: '人员撤离',
  contained: '事态受控',
  escalation: '事故扩大',
  completed: '推演结束',
  replay: '回放中',
  demo: '演示模式',
}

const phaseLabel = computed(() => phaseLabels[phase.value] ?? phase.value)

function goBack() {
  router.push({ name: 'UserScenarioList' })
}

function openCase() {
  if (caseMeta.value) router.push(p(`/cases/${caseMeta.value.caseId}`))
}

function formatTime(seconds: number) {
  const min = Math.floor(seconds / 60)
  const sec = Math.floor(seconds % 60)
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}

function getTempColor(temp: number) {
  if (temp < 40) return '#10b981'
  if (temp < 60) return '#f59e0b'
  if (temp < 80) return '#f97316'
  return '#ef4444'
}

function handleTogglePlay() {
  if (deductionMode.value) deduction.togglePlay()
  else legacy.togglePlay()
}

async function handleReset() {
  scoreFetched.value = false
  scoreReport.value = null
  if (deductionMode.value) {
    await deduction.reset()
  } else {
    await legacySession.abandon()
    legacy.reset()
    await legacySession.start()
    legacy.play()
  }
}

function handleSeek(val: number) {
  if (!deductionMode.value) legacy.seek(val)
}

function handleSetSpeed(val: number) {
  if (deductionMode.value) deduction.setSpeed(val)
  else legacy.setSpeed(val)
}

function handleDecision(decisionPointId: string, optionId: string) {
  deduction.submitDecision(decisionPointId, optionId)
}

async function showScoreReport() {
  if (scoreReport.value) {
    scoreVisible.value = true
    return
  }
  const data = await deduction.finishSession()
  if (data) {
    scoreReport.value = data
    scoreVisible.value = true
  }
}

watch(isCompleted, async completed => {
  if (!deductionMode.value || !completed || scoreFetched.value) return
  scoreFetched.value = true
  const data = await deduction.finishSession()
  if (data) {
    scoreReport.value = data
    scoreVisible.value = true
  }
})

watch(
  () => [currentTime.value, isPlaying.value, deductionMode.value, totalDuration.value],
  async () => {
    if (deductionMode.value || scoreFetched.value) return
    if (totalDuration.value <= 0) return
    if (currentTime.value < totalDuration.value) return
    if (isPlaying.value) return
    scoreFetched.value = true
    const data = await legacySession.finishFromObservation(
      cells.value,
      environment.value,
      totalDuration.value,
      currentTime.value,
    )
    if (data) {
      scoreReport.value = data
      scoreVisible.value = true
    } else {
      scoreFetched.value = false
    }
  },
)

function updateSceneSize() {
  if (sceneContainer.value) {
    sceneWidth.value = sceneContainer.value.clientWidth
    sceneHeight.value = sceneContainer.value.clientHeight
  }
}

onMounted(async () => {
  const res = await request.get(`/simulation/scenarios/${scenarioId}`)
  apiScenario.value = res.data

  const batteryCount = res.data.initialConditions?.batteryCount ?? 1
  deductionMode.value = isClassicDeductionScenario(scenarioId)

  if (deductionMode.value) {
    await deduction.startSession()
  } else {
    legacy.reinit(
      res.data.duration,
      batteryCount,
      res.data.initialConditions.initialTemperature,
      normalizeScenarioEvents(res.data.events),
    )
    await legacySession.start()
    legacy.play()
  }

  updateSceneSize()
  window.addEventListener('resize', updateSceneSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateSceneSize)
})
</script>

<style scoped>
.classic-sim-page {
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
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 6px;
  color: #94a3b8;
  cursor: pointer;
  font-size: 13px;
  flex-shrink: 0;
}
.back:hover { color: #e2e8f0; border-color: rgba(14, 165, 233, 0.5); }

.title-block {
  flex: 1;
  min-width: 0;
}
.title-block h1 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #f1f5f9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.title-block p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #7dd3fc;
  opacity: 0.85;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.title-block p.muted { color: #64748b; }

.cockpit-body {
  flex: 1;
  display: grid;
  grid-template-columns: 220px 1fr 240px;
  gap: 0;
  min-height: 0;
  overflow: hidden;
}

.col-side,
.col-alerts {
  overflow-y: auto;
  background: rgba(6, 18, 38, 0.6);
  border-right: 1px solid rgba(14, 165, 233, 0.12);
}
.col-alerts {
  border-right: none;
  border-left: 1px solid rgba(14, 165, 233, 0.12);
}

.panel {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(14, 165, 233, 0.1);
}
.panel h3 {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 600;
  color: #7dd3fc;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.panel h3 em {
  font-style: normal;
  color: #f97316;
}

.info-list { display: flex; flex-direction: column; gap: 8px; }
.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}
.info-item .label { color: #64748b; }
.info-item .value { font-weight: 600; color: #e2e8f0; }
.phase-val { color: #38bdf8 !important; }
.score-val { color: #4ade80 !important; }

.case-ref .case-title {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
  line-height: 1.4;
}
.case-ref .case-loc {
  margin: 0 0 8px;
  font-size: 11px;
  color: #64748b;
}

.telemetry-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}
.tel-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 6px;
  border: 1px solid rgba(51, 65, 85, 0.5);
}
.tel-label { font-size: 11px; color: #64748b; }
.tel-val { font-size: 14px; font-weight: 700; font-family: Consolas, monospace; }
.tel-val.warm { color: #fb923c; }
.tel-val.cool { color: #38bdf8; }
.tel-val.danger { color: #f87171; }

.temp-panel :deep(.temperature-field) {
  background: transparent;
}

.col-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.scene-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  padding: 8px 14px;
  background: rgba(6, 18, 38, 0.8);
  border-bottom: 1px solid rgba(14, 165, 233, 0.15);
}
.clock {
  font-family: Consolas, monospace;
  font-size: 14px;
  color: #7dd3fc;
  min-width: 100px;
}
.phase-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(43, 90, 237, 0.5);
  color: #e2e8f0;
}
.phase-tag.earlyWarning,
.phase-tag.thermalRunaway,
.phase-tag.escalation {
  background: rgba(239, 68, 68, 0.7);
}
.phase-tag.contained,
.phase-tag.completed {
  background: rgba(16, 185, 129, 0.7);
}
.ctrl {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  justify-content: flex-end;
  min-width: 0;
}
.time-slider {
  flex: 1;
  max-width: 200px;
  margin-right: 4px;
}

.scene-wrap {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: radial-gradient(ellipse at 50% 80%, #1a2744 0%, #0a0f1a 70%);
  min-height: 0;
}

.temperature-overlay {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  z-index: 5;
}
.temp-item {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.75);
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid rgba(51, 65, 85, 0.6);
}
.temp-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.temp-value {
  color: #e2e8f0;
  font-size: 11px;
  font-family: Consolas, monospace;
}

.alert-list {
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}
.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px;
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 1.4;
}
.alert-msg { flex: 1; }
.alert-time {
  font-size: 10px;
  opacity: 0.7;
  font-family: Consolas, monospace;
  flex-shrink: 0;
}
.alert-warning {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.3);
}
.alert-danger,
.alert-critical {
  background: rgba(239, 68, 68, 0.15);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.3);
}
.alert-info {
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.3);
}
.empty-alerts {
  text-align: center;
  padding: 24px 0;
  font-size: 12px;
  color: #475569;
}

@media (max-width: 1100px) {
  .cockpit-body {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr auto;
  }
  .col-side, .col-alerts {
    max-height: 180px;
    border: none;
    border-bottom: 1px solid rgba(14, 165, 233, 0.12);
  }
}
</style>
