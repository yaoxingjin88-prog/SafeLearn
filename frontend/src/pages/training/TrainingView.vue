<template>
  <div v-if="currentDecisionPoint" class="training-cockpit">
    <div class="grid-bg" />

    <header class="cockpit-header">
      <button class="back-btn" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 退出训练
      </button>
      <div class="title-block">
        <h1>{{ scenario.name }}</h1>
        <div class="title-tags">
          <span v-if="sceneLabel" class="tag scene">{{ sceneLabel }}</span>
          <span v-if="riskLabel" class="tag danger">{{ riskLabel }}</span>
        </div>
      </div>
      <div class="score-chip">
        <span class="score-label">得分</span>
        <span class="score-val">{{ totalScore }}</span>
      </div>
    </header>

    <div class="progress-strip" :class="{ urgent: isTimerUrgent && !showResult }">
      <div class="progress-meta">
        <span>决策进度 {{ currentStep }}/{{ scenario.decisionPoints.length }}</span>
        <span v-if="currentDecisionPoint.timelinePhase" class="phase-label">{{ currentDecisionPoint.timelinePhase }}</span>
      </div>
      <div class="progress-track">
        <i
          class="progress-fill"
          :class="{ success: showResult && isCorrect, fail: showResult && !isCorrect }"
          :style="{ width: progressPct + '%' }"
        />
      </div>
    </div>

    <main class="cockpit-main">
      <div class="training-workspace">
        <!-- 场景区 -->
        <section class="panel scenario-panel">
          <div class="panel-head">
            <el-icon><Warning /></el-icon>
            <span>现场态势</span>
          </div>
          <div class="scenario-stack">
            <TrainingSceneDiagram
              compact
              :scene-type="scenario.initialConditions?.sceneType"
              :max-temp="telemetry.maxTemp"
              :max-gas="telemetry.maxGas"
              :risk="telemetry.risk"
            />
            <p class="scenario-desc">
              <template v-for="(tok, i) in scenarioTokens" :key="i">
                <span :class="['hl', `hl-${tok.kind}`]">{{ tok.text }}</span>
              </template>
            </p>
          </div>
        </section>

        <!-- 答题区 -->
        <section class="panel decision-panel">
        <div class="decision-top">
          <h2 class="question">{{ currentDecisionPoint.question }}</h2>
          <div class="timer" :class="{ urgent: isTimerUrgent && !showResult }">
            <el-icon><Timer /></el-icon>
            <span>{{ timeLeft }}</span>
            <small>s</small>
          </div>
        </div>

        <div class="decision-body">
          <div class="options-list">
            <button
              v-for="(option, idx) in currentDecisionPoint.options"
              :key="option.id"
              type="button"
              class="option-card"
              :class="optionClass(option)"
              :disabled="showResult"
              @click="selectOption(option.id)"
            >
              <span class="opt-index">{{ String.fromCharCode(65 + idx) }}</span>
              <span class="opt-text">{{ option.text }}</span>
              <span v-if="showResult && option.correct" class="opt-badge correct">✓ 正确</span>
              <span v-if="showResult && selectedOption === option.id && !option.correct" class="opt-badge wrong">✗</span>
            </button>
          </div>

          <!-- 结果与解析 -->
          <transition name="fade-up">
            <div v-if="showResult" class="feedback-block">
            <div class="feedback-banner" :class="isCorrect ? 'ok' : 'bad'">
              <el-icon v-if="isCorrect"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              <span>{{ isCorrect ? `回答正确 · +${getSelectedScore()} 分` : '回答错误' }}</span>
            </div>

            <div v-if="!isCorrect && wrongConsequence" class="feedback-card consequence">
              <div class="fc-title">⚠ 错误后果</div>
              <p>{{ wrongConsequence }}</p>
            </div>

            <div v-if="correctOption" class="feedback-card regulation">
              <div class="fc-title">📋 法规依据</div>
              <p class="reg-name">{{ currentDecisionPoint.regulationRef }}</p>
              <p class="reg-answer">正确处置：{{ correctOption.text }}</p>
            </div>

            <div v-if="currentDecisionPoint.explanation" class="feedback-card analysis">
              <div class="fc-title">专业解析</div>
              <p>
                <template v-if="currentDecisionPoint.regulationRef">
                  <span class="hl hl-std">{{ currentDecisionPoint.regulationRef }}</span>
                  <span> — </span>
                </template>
                {{ currentDecisionPoint.explanation }}
              </p>
            </div>
          </div>
        </transition>
        </div>

        <!-- 导航 -->
        <div class="nav-bar">
          <div class="nav-toolbar">
            <label class="auto-switch">
              <el-switch v-model="autoAdvanceEnabled" size="small" />
              <span>自动跳转下一题</span>
            </label>
            <p v-if="autoAdvanceHint" class="auto-hint">{{ autoAdvanceHint }}</p>
          </div>
          <div class="nav-actions">
            <el-button :disabled="currentStep <= 1" @click="goPrev">
              <el-icon class="mr-1"><ArrowLeft /></el-icon> 上一题
            </el-button>
            <el-button
              v-if="!isLastDecision"
              type="primary"
              :disabled="!showResult"
              @click="goNext"
            >
              下一题 <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
            <el-button
              v-else
              type="primary"
              :disabled="!showResult || finishing"
              :loading="finishing"
              @click="finishTraining"
            >
              查看训练报告
            </el-button>
          </div>
        </div>
      </section>
      </div>
    </main>

    <el-dialog
      v-model="completeVisible"
      title="训练完成"
      width="560px"
      append-to-body
      align-center
      :close-on-click-modal="false"
      :show-close="false"
      class="complete-dialog"
    >
      <div v-if="completeReport" class="complete-body">
        <div class="complete-score" :class="getScoreClass(completeReport.totalScore)">
          <span class="val">{{ completeReport.totalScore }}</span>
          <span class="lbl">总分</span>
        </div>
        <el-tag :type="getRatingTag(completeReport.rating)" size="large">
          {{ getRatingName(completeReport.rating) }}
        </el-tag>
        <p class="complete-summary">{{ completeReport.instructorSummary || completeReport.feedback }}</p>
        <div v-if="completeReport.highlights?.length" class="complete-section">
          <h4>处置亮点</h4>
          <ul><li v-for="(h, i) in completeReport.highlights" :key="i">{{ h }}</li></ul>
        </div>
        <div v-if="completeReport.improvements?.length" class="complete-section warn">
          <h4>改进建议</h4>
          <ul><li v-for="(h, i) in completeReport.improvements" :key="i">{{ h }}</li></ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="goToReport(false)">返回列表</el-button>
        <el-button type="primary" @click="goToReport(true)">查看完整复盘</el-button>
      </template>
    </el-dialog>
  </div>

  <div v-else-if="loadError" class="state-screen">
    <el-empty :description="loadError">
      <el-button type="primary" @click="router.back()">返回训练列表</el-button>
    </el-empty>
  </div>
  <div v-else class="state-screen">
    <el-icon class="is-loading" :size="40"><Loading /></el-icon>
    <p>加载训练场景...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Warning, Timer, Loading, ArrowLeft, ArrowRight,
  CircleCheck, CircleClose,
} from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import TrainingSceneDiagram from '@/training/components/TrainingSceneDiagram.vue'
import {
  tokenizeHighlight,
  extractTelemetry,
  getOptionConsequence,
  enrichDecisionPoints,
} from '@/training/utils/trainingUi'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

const scenario = ref<any>({
  id: '', name: '', description: '', difficulty: 3, timeLimit: 60,
  decisionPoints: [], initialConditions: {},
})

const sceneLabel = computed(() => scenario.value.initialConditions?.sceneLabel || '')
const riskLabel = computed(() => {
  const r = scenario.value.initialConditions?.riskLevel
  if (r === 'critical') return '特别严重'
  if (r === 'major') return '严重'
  if (r === 'moderate') return '中等'
  return ''
})

const recordId = ref('')
const loadError = ref('')
const currentStep = ref(1)
const selectedOption = ref('')
const showResult = ref(false)
const isCorrect = ref(false)
const totalScore = ref(0)

interface StepAnswer {
  optionId: string
  isCorrect: boolean
  score: number
  responseTime: number
}
const answersMap = ref<Record<string, StepAnswer>>({})

const timeLeft = ref(30)
let timerInterval: number | null = null
let autoAdvanceTimer: number | null = null
let autoAdvanceTickTimer: number | null = null

const AUTO_ADVANCE_MS = 2000
const AUTO_ADVANCE_STORAGE_KEY = 'safelearn:training-auto-advance'

function readAutoAdvancePref(): boolean {
  try {
    const stored = localStorage.getItem(AUTO_ADVANCE_STORAGE_KEY)
    if (stored === null) return true
    return stored === '1'
  } catch {
    return true
  }
}

const autoAdvanceEnabled = ref(readAutoAdvancePref())
const autoAdvanceHint = ref('')
const finishing = ref(false)
const completeVisible = ref(false)
const completeReport = ref<any>(null)

const currentDecisionPoint = computed(() => scenario.value.decisionPoints[currentStep.value - 1])
const isLastDecision = computed(() => currentStep.value === scenario.value.decisionPoints.length)
const progressPct = computed(() =>
  (currentStep.value / Math.max(scenario.value.decisionPoints.length, 1)) * 100,
)
const isTimerUrgent = computed(() => timeLeft.value < 30)

const scenarioTokens = computed(() =>
  tokenizeHighlight(currentDecisionPoint.value?.triggerCondition || ''),
)
const telemetry = computed(() =>
  extractTelemetry(currentDecisionPoint.value?.triggerCondition || ''),
)

const correctOption = computed(() =>
  currentDecisionPoint.value?.options?.find((o: any) => o.correct),
)

const wrongConsequence = computed(() => {
  if (!selectedOption.value || isCorrect.value) return ''
  const opt = currentDecisionPoint.value?.options?.find((o: any) => o.id === selectedOption.value)
  if (!opt) return ''
  return getOptionConsequence(opt.id, opt.text, false, opt.consequence)
})

function normalizeDecisionPoints(raw: unknown): any[] {
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string' && raw.trim()) {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch { return [] }
  }
  return []
}

function normalizeScenario(data: any) {
  const raw = normalizeDecisionPoints(data?.decisionPoints).map((dp: any) => ({
    ...dp,
    timePressure: dp.timePressure ?? 30,
    options: Array.isArray(dp.options) ? dp.options : [],
  }))
  const decisionPoints = enrichDecisionPoints(raw, data?.id)
  return {
    ...data,
    decisionPoints,
    timeLimit: data.timeLimit ?? data.duration ?? 60,
    initialConditions: data.initialConditions ?? {},
  }
}

function getDecisionTimeLimit() {
  const dp = currentDecisionPoint.value
  if (!dp) return 30
  return dp.timePressure ?? scenario.value.timeLimit ?? 30
}

function getSelectedScore() {
  const option = currentDecisionPoint.value?.options?.find((o: any) => o.id === selectedOption.value)
  return option?.score || 0
}

function optionClass(option: any) {
  if (!showResult.value) {
    return { 'opt-hover': true }
  }
  if (option.correct) return { 'opt-correct': true }
  if (selectedOption.value === option.id) return { 'opt-wrong': true }
  return { 'opt-dim': true }
}

function recalcTotalScore() {
  totalScore.value = Object.values(answersMap.value).reduce((s, a) => s + a.score, 0)
}

function loadStepState(step: number) {
  const dp = scenario.value.decisionPoints[step - 1]
  if (!dp) return
  clearAutoAdvance()
  const saved = answersMap.value[dp.id]
  if (saved) {
    selectedOption.value = saved.optionId
    showResult.value = true
    isCorrect.value = saved.isCorrect
    stopTimer()
  } else {
    selectedOption.value = ''
    showResult.value = false
    isCorrect.value = false
    startTimer()
  }
}

function selectOption(optionId: string) {
  if (showResult.value || finishing.value) return
  const dp = currentDecisionPoint.value
  const option = dp.options.find((o: any) => o.id === optionId)
  if (!option) return

  selectedOption.value = optionId
  isCorrect.value = !!option.correct
  showResult.value = true

  const responseTime = Math.max(0, getDecisionTimeLimit() - timeLeft.value)
  answersMap.value[dp.id] = {
    optionId,
    isCorrect: isCorrect.value,
    score: option.score || 0,
    responseTime,
  }
  recalcTotalScore()
  stopTimer()
  void submitDecisionToServer(dp.id, optionId, responseTime)
  scheduleAutoAdvance()
}

async function submitDecisionToServer(decisionPointId: string, optionId: string, responseTime: number) {
  if (!recordId.value) return
  try {
    const res = await request.post('/training/decision', {
      recordId: recordId.value,
      decisionPointId,
      optionId,
      responseTime,
    })
    if (res.data?.totalScore != null) totalScore.value = res.data.totalScore
  } catch {
    /* 本地仍保留答题记录，结束时重试提交 */
  }
}

function goPrev() {
  clearAutoAdvance()
  if (currentStep.value <= 1) return
  currentStep.value--
  loadStepState(currentStep.value)
  scrollDecisionToTop()
}

function goNext() {
  clearAutoAdvance()
  if (!showResult.value || isLastDecision.value) return
  currentStep.value++
  loadStepState(currentStep.value)
  scrollDecisionToTop()
}

function scrollDecisionToTop() {
  document.querySelector('.decision-body')?.scrollTo({ top: 0, behavior: 'smooth' })
}

async function finishTraining() {
  if (!showResult.value || finishing.value) return
  finishing.value = true
  clearAutoAdvance()
  stopTimer()

  const decisions = Object.entries(answersMap.value).map(([dpId, a]) => ({
    decisionPointId: dpId,
    optionId: a.optionId,
    responseTime: a.responseTime,
  }))
  try {
    for (const d of decisions) {
      await request.post('/training/decision', { recordId: recordId.value, ...d })
    }
    const res = await request.get(`/training/records/${recordId.value}`)
    completeReport.value = res.data
    totalScore.value = res.data.totalScore ?? totalScore.value
    completeVisible.value = true
  } catch {
    completeReport.value = {
      totalScore: totalScore.value,
      rating: totalScore.value >= 90 ? 'excellent' : totalScore.value >= 70 ? 'good' : totalScore.value >= 60 ? 'average' : 'poor',
      instructorSummary: `训练已完成，本次得分 ${totalScore.value} 分。`,
      highlights: [],
      improvements: [],
    }
    completeVisible.value = true
  } finally {
    finishing.value = false
  }
}

function getScoreClass(score: number) {
  if (score >= 90) return 'score-excellent'
  if (score >= 70) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-poor'
}

function getRatingTag(rating: string) {
  const map: Record<string, string> = { excellent: 'success', good: '', average: 'warning', poor: 'danger' }
  return map[rating] || ''
}

function getRatingName(rating: string) {
  const map: Record<string, string> = { excellent: '优秀', good: '良好', average: '及格', poor: '不及格' }
  return map[rating] || rating
}

function goToReport(full: boolean) {
  completeVisible.value = false
  if (full) {
    router.push({ path: p(`/training/records/${recordId.value}`), query: { scenarioId: scenario.value.id } })
  } else {
    router.push(p('/training/scenarios'))
  }
}

function startTimer() {
  const dp = currentDecisionPoint.value
  if (!dp || showResult.value) return
  stopTimer()
  timeLeft.value = getDecisionTimeLimit()
  timerInterval = window.setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      stopTimer()
      const fallback = dp.options?.find((o: any) => !o.correct)?.id
      if (fallback) selectOption(fallback)
    }
  }, 1000)
}

function stopTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

function clearAutoAdvance() {
  if (autoAdvanceTimer) {
    clearTimeout(autoAdvanceTimer)
    autoAdvanceTimer = null
  }
  if (autoAdvanceTickTimer) {
    clearInterval(autoAdvanceTickTimer)
    autoAdvanceTickTimer = null
  }
  autoAdvanceHint.value = ''
}

function scheduleAutoAdvance() {
  clearAutoAdvance()
  if (!showResult.value || !autoAdvanceEnabled.value) return

  let remainSec = Math.ceil(AUTO_ADVANCE_MS / 1000)
  autoAdvanceHint.value = isLastDecision.value
    ? `${remainSec} 秒后展示训练总评…`
    : `${remainSec} 秒后进入下一题…`

  autoAdvanceTickTimer = window.setInterval(() => {
    remainSec -= 1
    if (remainSec <= 0) {
      if (autoAdvanceTickTimer) clearInterval(autoAdvanceTickTimer)
      autoAdvanceTickTimer = null
      return
    }
    autoAdvanceHint.value = isLastDecision.value
      ? `${remainSec} 秒后展示训练总评…`
      : `${remainSec} 秒后进入下一题…`
  }, 1000)

  autoAdvanceTimer = window.setTimeout(async () => {
    autoAdvanceTimer = null
    clearAutoAdvance()
    if (!showResult.value) return
    if (isLastDecision.value) {
      await finishTraining()
    } else {
      await nextTick()
      goNext()
    }
  }, AUTO_ADVANCE_MS)
}

watch(autoAdvanceEnabled, enabled => {
  try {
    localStorage.setItem(AUTO_ADVANCE_STORAGE_KEY, enabled ? '1' : '0')
  } catch {
    /* ignore */
  }
  if (!enabled) {
    clearAutoAdvance()
    return
  }
  if (showResult.value) scheduleAutoAdvance()
})

onMounted(async () => {
  const id = route.params.id as string
  try {
    const [scenarioRes, startRes] = await Promise.all([
      request.get(`/training/scenarios/${id}`),
      request.post('/training/start', { scenarioId: id }),
    ])
    scenario.value = normalizeScenario(scenarioRes.data)
    recordId.value = startRes.data.recordId
    if (!scenario.value.decisionPoints.length) {
      loadError.value = '该训练场景暂无决策题目，请联系管理员配置'
      return
    }
    startTimer()
  } catch {
    loadError.value = '加载训练场景失败，请稍后重试'
  }
})

onUnmounted(() => {
  stopTimer()
  clearAutoAdvance()
})
</script>

<style scoped>
.training-cockpit {
  position: relative;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #030712;
  color: #e2e8f0;
  overflow: hidden;
}

.grid-bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(14, 165, 233, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(14, 165, 233, 0.04) 1px, transparent 1px),
    radial-gradient(ellipse at 50% 0%, rgba(14, 165, 233, 0.08) 0%, transparent 55%);
  background-size: 32px 32px, 32px 32px, 100% 100%;
  pointer-events: none;
}

.cockpit-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: linear-gradient(90deg, #061226 0%, #0c1a30 100%);
  border-bottom: 1px solid rgba(14, 165, 233, 0.2);
  flex-shrink: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #334155;
  border-radius: 6px;
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
}
.back-btn:hover { color: #e2e8f0; border-color: #38bdf8; }

.title-block { flex: 1; min-width: 0; }
.title-block h1 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #f1f5f9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.title-tags { display: flex; gap: 6px; margin-top: 4px; }
.tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.tag.scene { background: rgba(59, 130, 246, 0.2); color: #93c5fd; }
.tag.danger { background: rgba(239, 68, 68, 0.2); color: #fca5a5; }

.score-chip {
  text-align: right;
  padding: 6px 14px;
  border-radius: 8px;
  background: rgba(14, 165, 233, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.25);
}
.score-label { display: block; font-size: 10px; color: #64748b; }
.score-val { font-size: 22px; font-weight: 700; color: #38bdf8; font-family: Consolas, monospace; }

.progress-strip {
  position: relative;
  z-index: 1;
  padding: 8px 16px;
  background: rgba(6, 18, 38, 0.9);
  border-bottom: 1px solid rgba(14, 165, 233, 0.12);
  flex-shrink: 0;
  transition: background 0.3s;
}
.progress-strip.urgent {
  background: rgba(69, 10, 10, 0.35);
  animation: strip-flash 1.2s ease-in-out infinite;
}
@keyframes strip-flash {
  0%, 100% { box-shadow: inset 0 -2px 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: inset 0 -2px 0 rgba(239, 68, 68, 0.9); }
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}
.phase-label { color: #fbbf24; font-family: Consolas, monospace; }

.progress-track {
  height: 6px;
  background: #1e293b;
  border-radius: 3px;
  overflow: hidden;
}
.progress-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #1d4ed8, #38bdf8);
  border-radius: 3px;
  transition: width 0.4s ease;
}
.progress-fill.success { background: linear-gradient(90deg, #059669, #34d399); }
.progress-fill.fail { background: linear-gradient(90deg, #dc2626, #f87171); }

.cockpit-main {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 10px 14px 12px;
  display: flex;
  flex-direction: column;
  max-width: 1180px;
  margin: 0 auto;
  width: 100%;
}

.training-workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(220px, 268px) minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
}

.scenario-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.scenario-stack {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scenario-stack::-webkit-scrollbar {
  width: 4px;
}
.scenario-stack::-webkit-scrollbar-thumb {
  background: rgba(56, 189, 248, 0.3);
  border-radius: 2px;
}

.panel {
  border-radius: 10px;
  border: 1px solid rgba(14, 165, 233, 0.15);
  background: rgba(6, 18, 38, 0.75);
  backdrop-filter: blur(8px);
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 11px;
  font-weight: 600;
  color: #7dd3fc;
  letter-spacing: 0.5px;
  border-bottom: 1px solid rgba(14, 165, 233, 0.1);
  flex-shrink: 0;
}

.scenario-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
}

@media (max-width: 860px) {
  .training-workspace {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(0, 1fr);
  }

  .scenario-panel {
    max-height: 24vh;
  }
}

@media (max-height: 720px) {
  .scenario-desc {
    display: -webkit-box;
    -webkit-line-clamp: 4;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.hl-temp { color: #fb923c; font-weight: 700; font-family: Consolas, monospace; }
.hl-gas { color: #38bdf8; font-weight: 700; font-family: Consolas, monospace; }
.hl-std { color: #a78bfa; font-weight: 600; }
.hl-risk { color: #f87171; font-weight: 600; }

.decision-panel {
  flex: 1;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 12px 14px;
  overflow: hidden;
}

.decision-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 2px;
  margin-right: -2px;
}

.decision-body::-webkit-scrollbar {
  width: 5px;
}
.decision-body::-webkit-scrollbar-thumb {
  background: rgba(56, 189, 248, 0.35);
  border-radius: 3px;
}

.decision-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
  flex-shrink: 0;
}
.question {
  flex: 1;
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #f1f5f9;
  line-height: 1.45;
}

.timer {
  display: flex;
  align-items: baseline;
  gap: 3px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(14, 165, 233, 0.12);
  border: 1px solid rgba(14, 165, 233, 0.3);
  color: #38bdf8;
  font-size: 20px;
  font-weight: 700;
  font-family: Consolas, monospace;
  flex-shrink: 0;
}
.timer small { font-size: 12px; color: #64748b; }
.timer.urgent {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.5);
  animation: timer-flash 0.8s ease-in-out infinite;
}
@keyframes timer-flash {
  0%, 100% { opacity: 1; box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { opacity: 0.85; box-shadow: 0 0 12px 2px rgba(239, 68, 68, 0.5); }
}

.options-list {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

@media (max-width: 560px) {
  .options-list {
    grid-template-columns: 1fr;
  }
}

.option-card {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  min-height: 100%;
  text-align: left;
  padding: 10px 11px;
  border-radius: 8px;
  border: 1px solid #334155;
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  cursor: pointer;
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s, background 0.2s;
}
.option-card:not(:disabled):hover {
  transform: translateX(4px);
  border-color: #38bdf8;
  background: rgba(14, 165, 233, 0.08);
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.12);
}
.option-card:disabled { cursor: default; }

.opt-index {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  background: #1e293b;
  font-size: 11px;
  font-weight: 700;
  color: #7dd3fc;
}
.opt-text { flex: 1; font-size: 12px; line-height: 1.45; }
.opt-badge {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
}
.opt-badge.correct { background: rgba(16, 185, 129, 0.2); color: #34d399; }
.opt-badge.wrong { background: rgba(239, 68, 68, 0.2); color: #f87171; }

.option-card.opt-correct {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  box-shadow: 0 0 16px rgba(16, 185, 129, 0.15);
}
.option-card.opt-wrong {
  border-color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}
.option-card.opt-dim { opacity: 0.45; }

.feedback-block {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}
.feedback-banner.ok {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.3);
}
.feedback-banner.bad {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.feedback-card {
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid rgba(51, 65, 85, 0.6);
  background: rgba(15, 23, 42, 0.5);
  text-align: left;
}
.feedback-card.consequence { border-color: rgba(239, 68, 68, 0.3); }
.feedback-card.regulation { border-color: rgba(167, 139, 250, 0.3); }
.feedback-card.analysis { border-color: rgba(14, 165, 233, 0.25); }

.fc-title {
  font-size: 12px;
  font-weight: 700;
  color: #7dd3fc;
  margin-bottom: 8px;
  letter-spacing: 0.3px;
}
.feedback-card p { margin: 0; font-size: 13px; line-height: 1.7; color: #94a3b8; }
.reg-name { color: #a78bfa !important; font-weight: 600; margin-bottom: 6px !important; }
.reg-answer { color: #34d399 !important; }

.nav-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(51, 65, 85, 0.5);
  flex-shrink: 0;
}

.nav-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.auto-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  font-size: 12px;
  color: #94a3b8;
}

.auto-switch :deep(.el-switch.is-checked .el-switch__core) {
  background-color: #38bdf8;
  border-color: #38bdf8;
}

.auto-hint {
  margin: 0;
  font-size: 12px;
  color: #7dd3fc;
  text-align: right;
  flex: 1;
  min-width: 0;
}

.nav-actions {
  display: flex;
  justify-content: space-between;
}

.complete-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.complete-score {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.complete-score .val { font-size: 32px; font-weight: 700; line-height: 1; }
.complete-score .lbl { font-size: 12px; opacity: 0.9; margin-top: 4px; }
.complete-score.score-excellent { background: linear-gradient(135deg, #10b981, #059669); }
.complete-score.score-good { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.complete-score.score-average { background: linear-gradient(135deg, #f59e0b, #d97706); }
.complete-score.score-poor { background: linear-gradient(135deg, #ef4444, #dc2626); }

.complete-summary {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
}

.complete-section {
  width: 100%;
  text-align: left;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}
.complete-section.warn {
  background: #fff7ed;
  border-color: #fed7aa;
}
.complete-section h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #334155;
}
.complete-section ul {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.fade-up-enter-active { transition: all 0.35s ease; }
.fade-up-enter-from { opacity: 0; transform: translateY(12px); }

.state-screen {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #030712;
  color: #64748b;
  gap: 16px;
}

.mr-1 { margin-right: 4px; }
.ml-1 { margin-left: 4px; }
</style>
