<template>
  <div class="sl-page simulation-replay" v-loading="loading">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="text-lg font-bold">事故推演回放</span>
        <el-tag class="ml-3" type="info" size="small">回放模式</el-tag>
      </template>
    </el-page-header>

    <el-row :gutter="24" class="mt-6">
      <el-col :span="18">
        <el-card class="scene-card">
          <div ref="sceneContainer" class="scene-container">
            <BatteryScene :cells="cells" />
            <div class="phase-badge replay">{{ phaseLabel }}</div>
          </div>
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
              :model-value="currentSec"
              :max="durationSec"
              :format-tooltip="formatTime"
              class="time-slider"
              @change="seek"
            />
            <div class="time-display">{{ formatTime(currentSec) }} / {{ formatTime(durationSec) }}</div>
            <el-select v-model="speedModel" size="small" style="width: 80px" @change="setSpeed">
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
          <template #header><span class="font-bold">回放信息</span></template>
          <div class="info-list">
            <div class="info-item"><span class="label">结果</span>
              <el-tag :type="sessionMeta.outcome === 'success' ? 'success' : 'danger'" size="small">
                {{ sessionMeta.outcome === 'success' ? '受控' : '扩大' }}
              </el-tag>
            </div>
            <div class="info-item"><span class="label">得分</span><span class="value">{{ sessionMeta.totalScore ?? '-' }}</span></div>
            <div class="info-item"><span class="label">当前阶段</span><span class="value">{{ phaseLabel }}</span></div>
            <div class="info-item"><span class="label">事件数</span><span class="value">{{ sessionMeta.eventCount ?? 0 }}</span></div>
          </div>
        </el-card>
        <el-card class="mt-4">
          <template #header><span class="font-bold">事件告警</span></template>
          <div class="alert-list">
            <div v-for="(a, i) in alerts" :key="i" class="alert-item">{{ formatTime(a.time) }} — {{ a.message }}</div>
            <div v-if="!alerts.length" class="text-gray-400 text-sm text-center py-4">播放后显示关键事件</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { VideoPlay, VideoPause, RefreshRight } from '@element-plus/icons-vue'
import BatteryScene from '@/components/simulation/BatteryScene.vue'
import { useDeductionReplay } from '@/simulation/composables/useDeductionReplay'

const route = useRoute()
const sessionId = route.params.sessionId as string
const loading = ref(true)
const speedModel = ref(1)

const {
  cells, isPlaying, currentMs, durationMs, phase, alerts, sessionMeta,
  load, togglePlay, seek: seekMs, setSpeed, reset,
} = useDeductionReplay(sessionId)

const currentSec = computed(() => Math.floor(currentMs.value / 1000))
const durationSec = computed(() => Math.max(1, Math.floor(durationMs.value / 1000)))

const phaseLabels: Record<string, string> = {
  replay: '回放中', monitoring: '正常监控', earlyWarning: '早期预警',
  thermalRunaway: '热失控', contained: '事态受控', escalation: '事故扩大', completed: '推演结束',
}
const phaseLabel = computed(() => phaseLabels[phase.value] ?? phase.value)

function formatTime(sec: number) {
  const min = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${min.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

function seek(sec: number) {
  seekMs(sec)
}

onMounted(async () => {
  try {
    await load()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.scene-card { height: calc(100vh - 200px); }
.scene-container {
  height: 100%;
  background: radial-gradient(ellipse at 50% 80%, #1a2744 0%, #0a0f1a 70%);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}
.phase-badge {
  position: absolute; top: 12px; right: 12px; padding: 6px 14px;
  border-radius: 20px; font-size: 13px; font-weight: 600; color: #fff; z-index: 10;
}
.phase-badge.replay { background: rgba(99, 102, 241, 0.9); }
.control-bar { display: flex; align-items: center; gap: 16px; padding: 16px 0 0; }
.time-slider { flex: 1; }
.time-display { font-size: 14px; color: #6b7280; white-space: nowrap; font-variant-numeric: tabular-nums; }
.info-list { display: flex; flex-direction: column; gap: 12px; }
.info-item { display: flex; justify-content: space-between; align-items: center; }
.info-item .label { color: #6b7280; }
.info-item .value { font-weight: 500; }
.alert-list { max-height: 280px; overflow-y: auto; }
.alert-item { font-size: 13px; padding: 8px 0; border-bottom: 1px solid #f3f4f6; color: #374151; }
</style>
