<script setup lang="ts">
import { computed } from 'vue'
import type { RiskLevel } from '../utils/trainingUi'

const props = defineProps<{
  sceneType?: 'station_operation' | 'test_operation' | string
  maxTemp: number
  maxGas: number
  risk: RiskLevel
  phase?: string
  compact?: boolean
}>()

const isTest = computed(() => props.sceneType === 'test_operation')

const tempColor = computed(() => {
  if (props.maxTemp >= 120) return '#ef4444'
  if (props.maxTemp >= 60) return '#f97316'
  if (props.maxTemp >= 42) return '#eab308'
  return '#22c55e'
})

const gasPct = computed(() => Math.min(100, Math.round(props.maxGas / 12)))
const tempPct = computed(() => Math.min(100, Math.round((props.maxTemp / 200) * 100)))

const riskLabel = computed(() => {
  const m: Record<RiskLevel, string> = {
    low: '低风险', medium: '预警', high: '高危', critical: '临界',
  }
  return m[props.risk]
})

const riskColor = computed(() => {
  const m: Record<RiskLevel, string> = {
    low: '#22c55e', medium: '#eab308', high: '#f97316', critical: '#ef4444',
  }
  return m[props.risk]
})
</script>

<template>
  <div class="scene-diagram" :class="[`risk-${risk}`, isTest ? 'type-test' : 'type-station', { compact }]">
    <div class="diag-title">{{ isTest ? '测试区电池包' : '储能舱示意' }}</div>
    <svg viewBox="0 0 200 140" class="diag-svg" aria-hidden="true">
      <defs>
        <linearGradient id="containerGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" :stop-color="tempColor" stop-opacity="0.35" />
          <stop offset="100%" stop-color="#0f172a" stop-opacity="0.9" />
        </linearGradient>
      </defs>
      <!-- 网格 -->
      <g opacity="0.15" stroke="#38bdf8" stroke-width="0.5">
        <line v-for="i in 8" :key="'h'+i" :x1="20" :y1="i*16" :x2="180" :y2="i*16" />
        <line v-for="i in 9" :key="'v'+i" :x1="i*20" y1="10" :x2="i*20" y2="130" />
      </g>
      <!-- 舱体 / 电池包 -->
      <rect
        v-if="!isTest"
        x="50" y="35" width="100" height="70" rx="6"
        fill="url(#containerGrad)" stroke="#38bdf8" stroke-width="1.5" opacity="0.9"
      />
      <rect v-else x="65" y="45" width="70" height="50" rx="4" fill="url(#containerGrad)" stroke="#fb923c" stroke-width="1.5" />
      <!-- 电池模组 -->
      <g v-if="!isTest">
        <rect v-for="i in 4" :key="i" :x="58 + (i-1)*22" y="48" width="18" height="44" rx="2"
          :fill="maxTemp >= 60 && i === 2 ? tempColor : '#1e293b'"
          :opacity="maxTemp >= 60 && i === 2 ? 0.9 : 0.6"
          stroke="#475569" stroke-width="0.8" />
      </g>
      <g v-else>
        <rect v-for="i in 3" :key="i" :x="72 + (i-1)*16" y="55" width="12" height="30" rx="1"
          fill="#1e293b" :stroke="i===2 ? tempColor : '#475569'" stroke-width="1" />
      </g>
      <!-- 烟雾 -->
      <ellipse v-if="maxTemp >= 60" cx="100" cy="28" rx="28" ry="10"
        fill="#94a3b8" opacity="0.25" />
      <!-- 告警脉冲 -->
      <circle v-if="risk === 'critical' || risk === 'high'" cx="155" cy="40" r="6"
        fill="none" stroke="#ef4444" stroke-width="1.5" opacity="0.8">
        <animate attributeName="r" values="6;10;6" dur="1.5s" repeatCount="indefinite" />
        <animate attributeName="opacity" values="0.8;0.2;0.8" dur="1.5s" repeatCount="indefinite" />
      </circle>
    </svg>
    <div class="diag-metrics">
      <div class="metric">
        <span class="m-label">温度</span>
        <div class="m-bar"><i :style="{ width: tempPct + '%', background: tempColor }" /></div>
        <span class="m-val" :style="{ color: tempColor }">{{ maxTemp }}℃</span>
      </div>
      <div class="metric">
        <span class="m-label">气体</span>
        <div class="m-bar"><i :style="{ width: gasPct + '%', background: maxGas >= 50 ? '#f97316' : '#38bdf8' }" /></div>
        <span class="m-val">{{ maxGas }}ppm</span>
      </div>
      <div class="risk-pill" :style="{ borderColor: riskColor, color: riskColor }">
        {{ riskLabel }}
      </div>
    </div>
    <p v-if="phase && !compact" class="diag-phase">{{ phase }}</p>
  </div>
</template>

<style scoped>
.scene-diagram {
  flex-shrink: 0;
  width: 200px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(6, 18, 38, 0.85);
  border: 1px solid rgba(14, 165, 233, 0.2);
}
.type-test { border-color: rgba(251, 146, 60, 0.3); }
.diag-title {
  font-size: 11px;
  font-weight: 600;
  color: #7dd3fc;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
  text-transform: uppercase;
}
.diag-svg { width: 100%; height: auto; display: block; }
.diag-metrics { margin-top: 8px; display: flex; flex-direction: column; gap: 6px; }
.metric { display: grid; grid-template-columns: 32px 1fr 44px; align-items: center; gap: 6px; }
.m-label { font-size: 10px; color: #64748b; }
.m-bar {
  height: 5px; background: #1e293b; border-radius: 3px; overflow: hidden;
}
.m-bar i { display: block; height: 100%; border-radius: 3px; transition: width 0.4s; }
.m-val { font-size: 11px; font-family: Consolas, monospace; color: #e2e8f0; text-align: right; }
.risk-pill {
  margin-top: 4px;
  text-align: center;
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 12px;
  border: 1px solid;
  background: rgba(0,0,0,0.3);
}
.diag-phase {
  margin: 8px 0 0;
  font-size: 10px;
  color: #64748b;
  text-align: center;
}
.risk-critical { box-shadow: 0 0 20px rgba(239, 68, 68, 0.15); }

.scene-diagram.compact {
  width: 100%;
  padding: 8px 10px;
}
.scene-diagram.compact .diag-title {
  margin-bottom: 4px;
  font-size: 10px;
}
.scene-diagram.compact .diag-svg {
  max-height: 88px;
}
.scene-diagram.compact .diag-metrics {
  margin-top: 6px;
  gap: 4px;
}
.scene-diagram.compact .metric {
  grid-template-columns: 28px 1fr 40px;
}
.scene-diagram.compact .m-label,
.scene-diagram.compact .m-val {
  font-size: 10px;
}
.scene-diagram.compact .risk-pill {
  margin-top: 2px;
  padding: 2px 6px;
  font-size: 9px;
}
</style>
