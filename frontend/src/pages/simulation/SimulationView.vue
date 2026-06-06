<template>
  <div class="simulation-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">{{ scenario.name }}</span>
      </template>
    </el-page-header>

    <el-row :gutter="24" class="mt-6">
      <!-- 3D场景区域 -->
      <el-col :span="18">
        <el-card class="scene-card">
          <div ref="sceneContainer" class="scene-container">
            <BatteryScene :cells="cells" />

            <!-- 粒子效果叠加层 -->
            <ParticleSystem :cells="cells" :width="sceneWidth" :height="sceneHeight" />

            <!-- 温度指示器 -->
            <div class="temperature-overlay">
              <div class="temp-item" v-for="cell in cells.slice(0, 8)" :key="cell.id">
                <div
                  class="temp-indicator"
                  :style="{ background: getTempColor(cell.temperature) }"
                />
                <span class="temp-value">#{{ cell.id + 1 }} {{ cell.temperature }}°C</span>
              </div>
            </div>
          </div>

          <!-- 控制栏 -->
          <div class="control-bar">
            <el-button-group>
              <el-button @click="togglePlay">
                <el-icon class="mr-1"><VideoPlay v-if="!isPlaying" /><VideoPause v-else /></el-icon>
                {{ isPlaying ? '暂停' : '播放' }}
              </el-button>
              <el-button @click="reset">
                <el-icon class="mr-1"><RefreshRight /></el-icon>
                重置
              </el-button>
            </el-button-group>

            <el-slider
              :model-value="currentTime"
              :max="totalDuration"
              :format-tooltip="formatTime"
              class="time-slider"
              @input="seek($event)"
            />

            <div class="time-display">
              {{ formatTime(currentTime) }} / {{ formatTime(totalDuration) }}
            </div>

            <el-select v-model="speedModel" size="small" style="width: 80px" @change="setSpeed">
              <el-option :value="0.5" label="0.5x" />
              <el-option :value="1" label="1x" />
              <el-option :value="2" label="2x" />
              <el-option :value="4" label="4x" />
            </el-select>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧面板 -->
      <el-col :span="6">
        <!-- 场景信息 -->
        <el-card>
          <template #header>
            <span class="font-bold">场景信息</span>
          </template>
          <div class="info-list">
            <div class="info-item">
              <span class="label">电池数量</span>
              <span class="value">{{ scenario.initialConditions.batteryCount }}</span>
            </div>
            <div class="info-item">
              <span class="label">初始温度</span>
              <span class="value">{{ scenario.initialConditions.initialTemperature }}°C</span>
            </div>
            <div class="info-item">
              <span class="label">电池类型</span>
              <span class="value">{{ scenario.initialConditions.batteryType }}</span>
            </div>
            <div class="info-item">
              <span class="label">推演时长</span>
              <span class="value">{{ scenario.duration }}秒</span>
            </div>
          </div>
        </el-card>

        <!-- 实时数据 -->
        <el-card class="mt-4">
          <template #header>
            <span class="font-bold">实时数据</span>
          </template>
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

        <!-- 温度场 -->
        <el-card class="mt-4">
          <TemperatureField :cells="cells" :cols="4" />
        </el-card>

        <!-- 告警列表 -->
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { Warning, VideoPlay, VideoPause, RefreshRight } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useSimulation } from '@/composables/useSimulation'
import BatteryScene from '@/components/simulation/BatteryScene.vue'
import ParticleSystem from '@/components/simulation/ParticleSystem.vue'
import TemperatureField from '@/components/simulation/TemperatureField.vue'

const route = useRoute()

const scenario = ref<any>({
  id: '', name: '', description: '', difficulty: 'medium', duration: 120,
  initialConditions: { batteryCount: 16, initialTemperature: 30, batteryType: '', capacity: 1 },
})

const sceneContainer = ref<HTMLElement>()
const sceneWidth = ref(600)
const sceneHeight = ref(400)

const {
  currentTime, isPlaying, cells, events, environment, totalDuration,
  togglePlay, reset, seek, setSpeed, reinit,
} = useSimulation()

const speedModel = ref(1)

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

function updateSceneSize() {
  if (sceneContainer.value) {
    sceneWidth.value = sceneContainer.value.clientWidth
    sceneHeight.value = sceneContainer.value.clientHeight
  }
}

onMounted(async () => {
  const id = route.params.id as string
  const res = await request.get(`/simulation/scenarios/${id}`)
  scenario.value = res.data
  reinit(res.data.duration, res.data.initialConditions.batteryCount, res.data.initialConditions.initialTemperature)
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
  background: linear-gradient(135deg, #0f172a, #1e293b);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
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

.alert-danger {
  background: #fee2e2;
  color: #991b1b;
}

.alert-critical {
  background: #fce7f3;
  color: #9d174d;
}
</style>
