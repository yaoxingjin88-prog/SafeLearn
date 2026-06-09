<template>
  <el-dialog
    v-model="visible"
    class="review-guide-dialog"
    width="720px"
    :close-on-click-modal="false"
    :show-close="!loading"
    destroy-on-close
    @closed="handleClosed"
  >
    <template #header>
      <div class="guide-header">
        <div class="guide-header-top">
          <span class="guide-badge">复盘引导</span>
          <span class="guide-step-count">第 {{ currentIndex + 1 }} / {{ steps.length }} 步</span>
        </div>
        <h3 class="guide-title">{{ caseTitle }}</h3>
        <el-progress
          :percentage="progressPercent"
          :stroke-width="8"
          :show-text="false"
          color="#2b5aed"
        />
      </div>
    </template>

    <div v-loading="loading" class="guide-body">
      <!-- 导读 -->
      <div v-if="currentStep?.kind === 'intro'" class="step-intro">
        <div class="intro-icon">
          <el-icon :size="48"><Reading /></el-icon>
        </div>
        <h4>{{ isPreviouslyCompleted ? '欢迎回来' : '开始结构化复盘' }}</h4>
        <p v-if="isPreviouslyCompleted" class="completed-hint">
          你已完成本案例复盘，可以查看总结或重新开始一轮复盘（选项顺序将重新打乱）。
        </p>
        <p v-else>你将按时间线逐步回顾事故发展，并在关键节点回答反思题，最后归纳原因与教训。</p>
        <ul v-if="!isPreviouslyCompleted" class="intro-tips">
          <li>共 {{ Math.max((caseData.timeline?.length || 0), 0) }} 个时间节点 + 原因与教训归纳</li>
          <li>反思题答错可重新选择，帮助巩固处置要点</li>
          <li>完成后将记录复盘进度</li>
        </ul>
        <div v-if="isPreviouslyCompleted" class="intro-actions">
          <el-button type="primary" @click="startNewReview">重新复盘</el-button>
          <el-button @click="goToSummary">查看上次总结</el-button>
        </div>
      </div>

      <!-- 时间线节点 -->
      <div v-else-if="currentStep?.kind === 'timeline'" class="step-timeline">
        <div class="event-card" :style="{ borderLeftColor: phaseColor(currentStep.event.type) }">
          <div class="event-meta">
            <span class="event-phase" :style="{ color: phaseColor(currentStep.event.type) }">
              {{ phaseLabel(currentStep.event.type) }}
            </span>
            <span v-if="currentStep.event.time && currentStep.event.time !== '—'" class="event-time">
              {{ currentStep.event.time }}
            </span>
          </div>
          <h4>{{ currentStep.event.title }}</h4>
          <p>{{ currentStep.event.description }}</p>
        </div>
        <div class="reflection-block">
          <div class="reflection-label">反思题</div>
          <p class="reflection-question">{{ currentStep.question }}</p>
          <div class="option-list">
            <button
              v-for="opt in currentStep.options"
              :key="opt.id"
              type="button"
              class="option-btn"
              :class="optionClass(currentStep.key, opt)"
              @click="selectOption(currentStep.key, opt)"
            >
              {{ opt.text }}
            </button>
          </div>
          <p v-if="feedback[currentStep.key]" class="option-feedback" :class="{ ok: feedback[currentStep.key]?.correct }">
            {{ feedback[currentStep.key]?.message }}
          </p>
        </div>
      </div>

      <!-- 原因分析 -->
      <div v-else-if="currentStep?.kind === 'cause'" class="step-cause">
        <h4><el-icon><Connection /></el-icon> 原因归因</h4>
        <div v-if="caseData.directCause" class="cause-snippet direct">直接：{{ caseData.directCause }}</div>
        <div v-if="caseData.indirectCause" class="cause-snippet">间接：{{ caseData.indirectCause }}</div>
        <div v-if="caseData.rootCause" class="cause-snippet root">根本：{{ caseData.rootCause }}</div>
        <div class="reflection-block">
          <div class="reflection-label">反思题</div>
          <p class="reflection-question">{{ currentStep.question }}</p>
          <div class="option-list">
            <button
              v-for="opt in currentStep.options"
              :key="opt.id"
              type="button"
              class="option-btn"
              :class="optionClass(currentStep.key, opt)"
              @click="selectOption(currentStep.key, opt)"
            >
              {{ opt.text }}
            </button>
          </div>
          <p v-if="feedback[currentStep.key]" class="option-feedback" :class="{ ok: feedback[currentStep.key]?.correct }">
            {{ feedback[currentStep.key]?.message }}
          </p>
        </div>
      </div>

      <!-- 经验教训 -->
      <div v-else-if="currentStep?.kind === 'lessons'" class="step-lessons">
        <h4><el-icon><MagicStick /></el-icon> 经验教训</h4>
        <ol v-if="caseData.lessons?.length" class="lessons-preview">
          <li v-for="(lesson, i) in caseData.lessons" :key="i">{{ lesson }}</li>
        </ol>
        <div class="reflection-block">
          <div class="reflection-label">反思题</div>
          <p class="reflection-question">{{ currentStep.question }}</p>
          <div class="option-list">
            <button
              v-for="opt in currentStep.options"
              :key="opt.id"
              type="button"
              class="option-btn"
              :class="optionClass(currentStep.key, opt)"
              @click="selectOption(currentStep.key, opt)"
            >
              {{ opt.text }}
            </button>
          </div>
          <p v-if="feedback[currentStep.key]" class="option-feedback" :class="{ ok: feedback[currentStep.key]?.correct }">
            {{ feedback[currentStep.key]?.message }}
          </p>
        </div>
      </div>

      <!-- 总结 -->
      <div v-else-if="currentStep?.kind === 'summary'" class="step-summary">
        <div class="summary-icon">
          <el-icon :size="56" color="#10b981"><CircleCheck /></el-icon>
        </div>
        <h4>复盘完成</h4>
        <p>你已完成本案例的结构化复盘，建议结合相关推演场景进一步演练。</p>
        <div class="summary-stats">
          <div class="stat-item">
            <span class="stat-num">{{ answeredCount }}</span>
            <span class="stat-label">反思作答</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ correctCount }}</span>
            <span class="stat-label">答对题数</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ scorePercent }}%</span>
            <span class="stat-label">正确率</span>
          </div>
        </div>
        <el-button class="restart-btn" type="primary" plain @click="startNewReview">
          重新复盘
        </el-button>
      </div>
    </div>

    <template #footer>
      <div class="guide-footer">
        <el-button v-if="currentIndex > 0" @click="prevStep">上一步</el-button>
        <div class="footer-spacer" />
        <el-button @click="closeGuide">退出</el-button>
        <el-button
          v-if="currentStep?.kind !== 'summary' && !(currentStep?.kind === 'intro' && isPreviouslyCompleted)"
          type="primary"
          :disabled="!canGoNext"
          @click="nextStep"
        >
          {{ currentIndex === steps.length - 2 ? '完成复盘' : '下一步' }}
        </el-button>
        <template v-else>
          <el-button @click="startNewReview">重新复盘</el-button>
          <el-button type="primary" @click="finishGuide">关闭并返回</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Reading, Connection, MagicStick, CircleCheck } from '@element-plus/icons-vue'
import request from '@/api/request'
import {
  buildGuideSteps, phaseColor, phaseLabel, createSessionSeed, pickWrongHint,
  SEED_META_KEY, stripReflectionMeta,
  type ReviewOption,
} from '@/composables/useCaseReviewGuide'

const props = defineProps<{
  modelValue: boolean
  caseId: string
  caseTitle: string
  caseData: {
    timeline?: { id: string; time: string; title: string; description: string; type: string }[]
    directCause?: string
    indirectCause?: string
    rootCause?: string
    lessons?: string[]
  }
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  completed: []
  progress: [payload: { completed: boolean; currentStep: number; totalSteps: number }]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const loading = ref(false)
const currentIndex = ref(0)
const sessionSeed = ref('')
const isPreviouslyCompleted = ref(false)
const reflections = ref<Record<string, string>>({})
const feedback = ref<Record<string, { correct: boolean; message: string }>>({})

const steps = computed(() =>
  sessionSeed.value ? buildGuideSteps(props.caseData, sessionSeed.value) : [])
const currentStep = computed(() => steps.value[currentIndex.value])
const progressPercent = computed(() =>
  steps.value.length ? Math.round(((currentIndex.value + 1) / steps.value.length) * 100) : 0)

const questionStepCount = computed(() =>
  steps.value.filter(s => s.kind !== 'intro' && s.kind !== 'summary').length)

const answeredCount = computed(() => Object.keys(stripReflectionMeta(reflections.value)).length)
const correctCount = computed(() =>
  Object.values(feedback.value).filter(f => f.correct).length)
const scorePercent = computed(() => {
  if (!questionStepCount.value) return 0
  return Math.round((correctCount.value / questionStepCount.value) * 100)
})

const canGoNext = computed(() => {
  const step = currentStep.value
  if (!step) return false
  if (step.kind === 'intro' || step.kind === 'summary') return true
  const fb = feedback.value[step.key]
  return !!fb?.correct
})

function restoreAnswers(saved: Record<string, string>) {
  reflections.value = { ...saved }
  feedback.value = {}
  for (const step of steps.value) {
    if (step.kind === 'intro' || step.kind === 'summary') continue
    const selectedId = saved[step.key]
    if (!selectedId) continue
    const opt = step.options.find(o => o.id === selectedId)
    if (!opt) continue
    feedback.value[step.key] = {
      correct: opt.correct,
      message: opt.correct
        ? '回答正确，处置思路符合安全规范。'
        : pickWrongHint(opt.text),
    }
  }
}

function buildReflectionsPayload() {
  return {
    [SEED_META_KEY]: sessionSeed.value,
    ...stripReflectionMeta(reflections.value),
  }
}

async function loadProgress() {
  if (!props.caseId) return
  loading.value = true
  try {
    const res = await request.get(`/cases/${props.caseId}/progress`)
    const data = res.data || {}
    const saved = (data.reflections || {}) as Record<string, string>
    isPreviouslyCompleted.value = !!data.completed
    sessionSeed.value = saved[SEED_META_KEY] || createSessionSeed(props.caseId)

    if (data.completed) {
      currentIndex.value = 0
      restoreAnswers(saved)
    } else if (typeof data.currentStep === 'number' && data.currentStep > 0) {
      currentIndex.value = Math.min(data.currentStep, steps.value.length - 1)
      restoreAnswers(saved)
    } else {
      currentIndex.value = 0
      reflections.value = { [SEED_META_KEY]: sessionSeed.value }
      feedback.value = {}
    }
  } finally {
    loading.value = false
  }
}

watch(() => props.modelValue, (open) => {
  if (open && props.caseId) loadProgress()
})

async function startNewReview() {
  await resetGuide(true)
}

async function resetGuide(goIntro: boolean) {
  if (!props.caseId) return
  loading.value = true
  try {
    await request.delete(`/cases/${props.caseId}/progress`)
    isPreviouslyCompleted.value = false
    sessionSeed.value = createSessionSeed(props.caseId)
    currentIndex.value = goIntro ? 0 : 0
    reflections.value = { [SEED_META_KEY]: sessionSeed.value }
    feedback.value = {}
    emit('progress', { completed: false, currentStep: 0, totalSteps: 0 })
  } finally {
    loading.value = false
  }
}

function goToSummary() {
  currentIndex.value = Math.max(steps.value.length - 1, 0)
}

function optionClass(stepKey: string, opt: ReviewOption) {
  const selected = reflections.value[stepKey]
  const fb = feedback.value[stepKey]
  if (!selected) return {}
  if (selected !== opt.id) return { dimmed: true }
  return { selected: true, correct: fb?.correct, wrong: fb && !fb.correct }
}

function selectOption(stepKey: string, opt: ReviewOption) {
  reflections.value = {
    [SEED_META_KEY]: sessionSeed.value,
    ...stripReflectionMeta(reflections.value),
    [stepKey]: opt.id,
  }
  if (opt.correct) {
    feedback.value[stepKey] = { correct: true, message: '回答正确，处置思路符合安全规范。' }
  } else {
    feedback.value[stepKey] = { correct: false, message: pickWrongHint(opt.text) }
  }
  saveProgress()
}

async function saveProgress() {
  if (!props.caseId) return
  try {
    const res = await request.put(`/cases/${props.caseId}/progress`, {
      currentStep: currentIndex.value,
      totalSteps: steps.value.length,
      reflections: buildReflectionsPayload(),
    })
    emit('progress', {
      completed: !!res.data?.completed,
      currentStep: res.data?.currentStep ?? currentIndex.value,
      totalSteps: res.data?.totalSteps ?? steps.value.length,
    })
  } catch {
    // 静默失败，不打断引导
  }
}

async function nextStep() {
  if (!canGoNext.value) return
  if (currentIndex.value >= steps.value.length - 1) return

  if (currentIndex.value === steps.value.length - 2) {
    await completeGuide()
    currentIndex.value = steps.value.length - 1
    return
  }

  currentIndex.value += 1
  await saveProgress()
}

function prevStep() {
  if (currentIndex.value > 0) currentIndex.value -= 1
}

async function completeGuide() {
  if (!props.caseId) return
  try {
    const res = await request.post(`/cases/${props.caseId}/complete`, {
      totalSteps: steps.value.length,
      reflections: buildReflectionsPayload(),
    })
    isPreviouslyCompleted.value = true
    emit('completed')
    emit('progress', {
      completed: true,
      currentStep: steps.value.length,
      totalSteps: steps.value.length,
    })
    return res
  } catch {
    // ignore
  }
}

function finishGuide() {
  visible.value = false
}

function closeGuide() {
  saveProgress()
  visible.value = false
}

function handleClosed() {
  currentIndex.value = 0
  isPreviouslyCompleted.value = false
  sessionSeed.value = ''
}
</script>

<style scoped>
.guide-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.guide-badge {
  font-size: 12px;
  font-weight: 600;
  color: #2b5aed;
  background: #e8efff;
  padding: 4px 10px;
  border-radius: 999px;
}

.guide-step-count {
  font-size: 13px;
  color: #6b7280;
}

.guide-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px;
  line-height: 1.4;
}

.guide-body {
  min-height: 280px;
}

.step-intro {
  text-align: center;
  padding: 12px 8px 8px;
}

.intro-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8efff, #f0f4ff);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2b5aed;
}

.step-intro h4 {
  font-size: 20px;
  margin-bottom: 8px;
  color: #111827;
}

.step-intro p {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.completed-hint {
  color: #059669;
  background: #ecfdf5;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.intro-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.intro-tips {
  text-align: left;
  margin: 0;
  padding: 16px 20px;
  background: #f9fafb;
  border-radius: 12px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.8;
}

.restart-btn {
  margin-top: 20px;
}

.event-card {
  background: #f8fafc;
  border-left: 4px solid #2b5aed;
  border-radius: 0 12px 12px 0;
  padding: 16px 18px;
  margin-bottom: 20px;
}

.event-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.event-phase {
  font-size: 12px;
  font-weight: 700;
}

.event-time {
  font-size: 13px;
  color: #6b7280;
}

.event-card h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.event-card p {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  margin: 0;
}

.reflection-block {
  margin-top: 8px;
}

.reflection-label {
  font-size: 12px;
  font-weight: 600;
  color: #2b5aed;
  margin-bottom: 6px;
}

.reflection-question {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 14px;
  line-height: 1.5;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-btn {
  text-align: left;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.5;
}

.option-btn:hover:not(.dimmed) {
  border-color: #2b5aed;
  background: #f8faff;
}

.option-btn.selected {
  border-color: #2b5aed;
  background: #e8efff;
  font-weight: 600;
}

.option-btn.correct {
  border-color: #10b981;
  background: #ecfdf5;
  color: #047857;
}

.option-btn.wrong {
  border-color: #f87171;
  background: #fef2f2;
  color: #b91c1c;
}

.option-btn.dimmed {
  opacity: 0.45;
}

.option-feedback {
  margin-top: 12px;
  font-size: 13px;
  color: #b91c1c;
  padding: 10px 12px;
  background: #fef2f2;
  border-radius: 8px;
}

.option-feedback.ok {
  color: #047857;
  background: #ecfdf5;
}

.step-cause h4,
.step-lessons h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  margin-bottom: 12px;
  color: #111827;
}

.cause-snippet {
  font-size: 13px;
  color: #4b5563;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 8px;
  line-height: 1.5;
}

.cause-snippet.direct { border-left: 3px solid #ef4444; }
.cause-snippet.root { border-left: 3px solid #8b5cf6; }

.lessons-preview {
  margin: 0 0 16px;
  padding-left: 20px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
}

.step-summary {
  text-align: center;
  padding: 20px 8px;
}

.summary-icon {
  margin-bottom: 12px;
}

.step-summary h4 {
  font-size: 22px;
  color: #111827;
  margin-bottom: 8px;
}

.step-summary p {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 24px;
}

.summary-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #2b5aed;
}

.stat-label {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.guide-footer {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}

.footer-spacer {
  flex: 1;
}
</style>
