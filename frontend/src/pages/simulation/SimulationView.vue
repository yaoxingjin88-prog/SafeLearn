<template>
  <div class="sl-page simulation-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">{{ displayName }}</span>
        <el-tag v-if="deductionMode" class="ml-3" type="warning" size="small">决策推演</el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="24" class="mt-6">
      <el-col :span="18">
        <el-card class="scene-card">
          <div ref="sceneContainer" class="scene-container">
            <BatteryScene :cells="cells" />
            <ParticleSystem
              v-if="cells.length > 1"
              :cells="cells"
              :width="sceneWidth"
              :height="sceneHeight"
            />

            <div v-if="deductionMode" class="phase-badge" :class="phase">
              {{ phaseLabel }}
            </div>

            <div class="temperature-overlay">
              <div class="temp-item" v-for="cell in cells.slice(0, 8)" :key="cell.id">
                <div class="temp-indicator" :style="{ background: getTempColor(cell.temperature) }" />
                <span class="temp-value">#{{ cell.id + 1 }} {{ cell.temperature }}°C</span>
              </div>
            </div>
          </div>

          <div class="control-bar">
            <el-button-group>
              <el-button :disabled="!!pendingDecision || isCompleted" @click="handleTogglePlay">
                <el-icon class="mr-1"><VideoPlay v-if="!isPlaying" /><VideoPause v-else /></el-icon>
                {{ isPlaying ? '暂停' : '播放' }}
              </el-button>
              <el-button @click="handleReset">
                <el-icon class="mr-1"><RefreshRight /></el-icon>
                重置
              </el-button>
              <el-button v-if="deductionMode && isCompleted" type="primary" @click="showScoreReport">
                查看评分
              </el-button>
            </el-button-group>

            <el-slider
              :model-value="currentTime"
              :max="totalDuration"
              :format-tooltip="formatTime"
              class="time-slider"
              :disabled="deductionMode"
              @input="handleSeek"
            />

            <div class="time-display">
              {{ formatTime(currentTime) }} / {{ formatTime(totalDuration) }}
            </div>

            <el-select v-model="speedModel" size="small" style="width: 80px" @change="handleSetSpeed">
              <el-option :value="0.5" label="0.5x" />
              <el-option :value="1" label="1x" />
              <el-option :value="2" label="2x" />
              <el-option :value="4" label="4x" />
            </el-select>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card>
          <template #header><span class="font-bold">场景信息</span></template>
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
              <span class="value">{{ phaseLabel }}</span>
            </div>
            <div v-if="deductionMode && context" class="info-item">
              <span class="label">当前得分</span>
              <span class="value">{{ context.score }}</span>
            </div>
            <div class="info-item">
              <span class="label">推演时长</span>
              <span class="value">{{ totalDuration }}秒</span>
            </div>
          </div>
        </el-card>

        <el-card class="mt-4">
          <template #header><span class="font-bold">实时数据</span></template>
          <div class="realtime-data">
            <div class="data-item">
              <div class="data-label">环境温度</div>
              <div class="data-value text-orange-500">{{ environment.temperature.toFixed(1) }}°C</div>
            </div>
            <div class="data-item">
              <div class="data-label">湿度</div>
              <div class="data-value text-blue-500">{{ environment.humidity.toFixed(0) }}%</div>
            </div>
            <div class="data-item">
              <div class="data-label">气体浓度</div>
              <div class="data-value text-red-500">{{ environment.gasLevel.toFixed(0) }}ppm</div>
            </div>
          </div>
        </el-card>

        <el-card class="mt-4">
          <TemperatureField :cells="cells" :cols="Math.min(4, cells.length)" />
        </el-card>

        <el-card class="mt-4">
          <template #header>
            <span class="font-bold">告警信息 ({{ events.length }})</span>
          </template>
          <div class="alert-list">
            <div
              v-for="(evt, idx) in events"
              :key="idx"
              class="alert-item"
              :class="`alert-${evt.type}`"
            >
              <el-icon><Warning /></el-icon>
              <span>{{ evt.message }}</span>
              <span class="alert-time">{{ formatTime(evt.time) }}</span>
            </div>
            <div v-if="events.length === 0" class="text-gray-400 text-sm text-center py-4">
              暂无告警
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
import { useRoute } from 'vue-router'
import { Warning, VideoPlay, VideoPause, RefreshRight } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useSimulation, normalizeScenarioEvents } from '@/composables/useSimulation'
import { useDeductionEngine } from '@/simulation/composables/useDeductionEngine'
import BatteryScene from '@/components/simulation/BatteryScene.vue'
import ParticleSystem from '@/components/simulation/ParticleSystem.vue'
import TemperatureField from '@/components/simulation/TemperatureField.vue'
import DecisionPanel from '@/components/simulation/DecisionPanel.vue'
import ScoreReportDialog from '@/components/simulation/ScoreReportDialog.vue'

const route = useRoute()
const scenarioId = route.params.id as string

const apiScenario = ref<any>({
  id: '', name: '', duration: 120,
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

const displayName = computed(() => apiScenario.value.name || '事故推演')
const scenarioInfo = computed(() => ({
  batteryCount: apiScenario.value.initialConditions?.batteryCount ?? 1,
  initialTemperature: apiScenario.value.initialConditions?.initialTemperature ?? 35,
}))

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

function handleReset() {
  scoreFetched.value = false
  scoreReport.value = null
  if (deductionMode.value) deduction.reset()
  else legacy.reset()
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
  deductionMode.value = batteryCount <= 1

  if (deductionMode.value) {
    await deduction.startSession()
  } else {
    legacy.reinit(
      res.data.duration,
      batteryCount,
      res.data.initialConditions.initialTemperature,
      normalizeScenarioEvents(res.data.events),
    )
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
.scene-card {
  height: calc(100vh - 200px);
}

.scene-container {
  height: 100%;
  background: radial-gradient(ellipse at 50% 80%, #1a2744 0%, #0a0f1a 70%);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}

.phase-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: rgba(43, 90, 237, 0.85);
  z-index: 10;
}

.phase-badge.earlyWarning,
.phase-badge.thermalRunaway {
  background: rgba(239, 68, 68, 0.9);
}

.phase-badge.contained,
.phase-badge.completed {
  background: rgba(16, 185, 129, 0.9);
}

.temperature-overlay {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.temp-item {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.7);
  padding: 4px 8px;
  border-radius: 4px;
}

.temp-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.temp-value {
  color: white;
  font-size: 11px;
}

.control-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0 0;
}

.time-slider {
  flex: 1;
}

.time-display {
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
}

.info-item .label {
  color: #6b7280;
}

.info-item .value {
  font-weight: 500;
}

.realtime-data {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  text-align: center;
}

.data-item {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.data-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.data-value {
  font-size: 18px;
  font-weight: bold;
}

.alert-list {
  max-height: 200px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 13px;
}

.alert-time {
  margin-left: auto;
  font-size: 11px;
  opacity: 0.7;
}

.alert-warning {
  background: #fef3c7;
  color: #92400e;
}

.alert-danger,
.alert-critical {
  background: #fee2e2;
  color: #991b1b;
}

.alert-info {
  background: #dbeafe;
  color: #1e40af;
}
</style>
