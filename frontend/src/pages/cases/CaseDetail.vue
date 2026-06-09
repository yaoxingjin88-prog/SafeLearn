<template>
  <div class="sl-page case-detail">
    <!-- 顶部返回栏 -->
    <div class="detail-topbar">
      <button class="back-btn" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回案例列表
      </button>
      <div class="topbar-actions">
        <el-tag v-if="caseProgress.completed" type="success" effect="plain" round>已复盘</el-tag>
        <el-button type="primary" @click="openGuide">
          <el-icon><Reading /></el-icon>
          {{ caseProgress.completed ? '查看 / 再次复盘' : '开始复盘引导' }}
        </el-button>
        <FavoriteButton v-if="caseData.id" :link="false" target-type="case" :target-id="caseData.id" />
      </div>
    </div>

    <!-- 事故概览卡 -->
    <div class="overview-card">
      <!-- 现场示意图占位 -->
      <div class="scene-visual" :class="`scene-${caseData.type}`">
        <el-icon :size="72"><component :is="typeIcon(caseData.type)" /></el-icon>
        <span class="scene-badge" :style="{ background: severityColor(caseData.severity) }">
          {{ getSeverityName(caseData.severity) }}
        </span>
      </div>

      <div class="overview-main">
        <div class="overview-tags">
          <span class="severity-pill" :style="{ background: severityColor(caseData.severity) }">
            {{ getSeverityName(caseData.severity) }}
          </span>
          <el-tag effect="plain" round>{{ getTypeName(caseData.type) }}</el-tag>
          <el-tag v-if="caseData.difficulty" effect="plain" round :type="difficultyType(caseData.difficulty)">
            {{ difficultyLabel(caseData.difficulty) }}
          </el-tag>
        </div>
        <h1 class="overview-title">{{ caseData.title }}</h1>
        <div class="overview-meta">
          <span><el-icon><Location /></el-icon> {{ caseData.location }}</span>
          <span><el-icon><Calendar /></el-icon> {{ caseData.date }}</span>
          <span v-if="caseData.studyMinutes"><el-icon><Clock /></el-icon> 建议学习 {{ caseData.studyMinutes }} 分钟</span>
        </div>
        <p class="overview-desc">{{ caseData.description }}</p>

        <!-- 关键数据大数字 -->
        <div class="key-stats">
          <div class="key-stat">
            <div class="key-icon loss"><el-icon><Money /></el-icon></div>
            <div>
              <div class="key-value">{{ caseData.lossAmount ? caseData.lossAmount : '—' }}<span class="key-unit" v-if="caseData.lossAmount">万元</span></div>
              <div class="key-label">直接经济损失</div>
            </div>
          </div>
          <div class="key-stat">
            <div class="key-icon people"><el-icon><User /></el-icon></div>
            <div>
              <div class="key-value sm">{{ caseData.casualties || '—' }}</div>
              <div class="key-label">人员伤亡</div>
            </div>
          </div>
          <div class="key-stat">
            <div class="key-icon time"><el-icon><Timer /></el-icon></div>
            <div>
              <div class="key-value">{{ timelineSpan }}</div>
              <div class="key-label">处置历时</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-row :gutter="24" class="mt-6">
      <!-- 左侧内容 -->
      <el-col :span="16">
        <!-- 时间线 -->
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title"><el-icon><Histogram /></el-icon> 事故时间线</span>
            </div>
          </template>

          <!-- 阶段图例 -->
          <div class="phase-legend">
            <span class="legend-item"><i class="dot" style="background:#f59e0b"></i>预警</span>
            <el-icon class="legend-arrow"><Right /></el-icon>
            <span class="legend-item"><i class="dot" style="background:#ef4444"></i>发生 / 扩大</span>
            <el-icon class="legend-arrow"><Right /></el-icon>
            <span class="legend-item"><i class="dot" style="background:#3b82f6"></i>处置</span>
            <el-icon class="legend-arrow"><Right /></el-icon>
            <span class="legend-item"><i class="dot" style="background:#10b981"></i>控制</span>
          </div>

          <el-timeline class="case-timeline">
            <el-timeline-item
              v-for="(event, idx) in caseData.timeline"
              :key="event.id"
              :timestamp="event.time"
              :color="phaseColor(event.type)"
              :hollow="!isKeyEvent(event, idx)"
              size="large"
              placement="top"
            >
              <div class="timeline-event" :class="{ key: isKeyEvent(event, idx) }" :style="isKeyEvent(event, idx) ? { borderLeftColor: phaseColor(event.type) } : {}">
                <div class="event-head">
                  <span class="event-phase" :style="{ background: phaseBg(event.type), color: phaseColor(event.type) }">
                    {{ phaseLabel(event.type) }}
                  </span>
                  <h4 class="event-title">{{ event.title }}</h4>
                </div>
                <p class="event-desc">{{ event.description }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 原因分析 - 分层归因 -->
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title"><el-icon><Connection /></el-icon> 原因分析</span>
              <el-tag v-if="caseData.responsibleParty" type="danger" effect="dark" round size="small">
                责任方：{{ caseData.responsibleParty }}
              </el-tag>
            </div>
          </template>

          <div v-if="hasStructuredCause" class="cause-layers">
            <div v-if="caseData.directCause" class="cause-card direct">
              <div class="cause-tag">直接原因</div>
              <p>{{ caseData.directCause }}</p>
            </div>
            <div class="cause-connector" v-if="caseData.directCause && (caseData.indirectCause || caseData.rootCause)"></div>
            <div v-if="caseData.indirectCause" class="cause-card indirect">
              <div class="cause-tag">间接原因</div>
              <p>{{ caseData.indirectCause }}</p>
            </div>
            <div class="cause-connector" v-if="caseData.indirectCause && caseData.rootCause"></div>
            <div v-if="caseData.rootCause" class="cause-card root">
              <div class="cause-tag">根本原因</div>
              <p>{{ caseData.rootCause }}</p>
            </div>
          </div>
          <p v-else class="fallback-text">{{ caseData.causeAnalysis }}</p>
        </el-card>

        <!-- 经验教训 -->
        <el-card class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title"><el-icon><MagicStick /></el-icon> 经验教训</span>
            </div>
          </template>
          <ol v-if="caseData.lessons?.length" class="lessons-list">
            <li v-for="(lesson, idx) in caseData.lessons" :key="idx" class="lesson-item">
              <span class="lesson-num">{{ idx + 1 }}</span>
              <span class="lesson-text">{{ lesson }}</span>
            </li>
          </ol>
          <p v-else class="fallback-text">{{ caseData.lessonsLearned }}</p>
        </el-card>
      </el-col>

      <!-- 右侧信息 -->
      <el-col :span="8">
        <!-- 事故概况卡片组 -->
        <el-card class="section-card">
          <template #header>
            <span class="section-title sm"><el-icon><Document /></el-icon> 事故概况</span>
          </template>
          <div class="profile-grid">
            <div class="profile-item">
              <div class="profile-icon" style="background:#fef2f2;color:#ef4444"><el-icon><Warning /></el-icon></div>
              <div>
                <div class="profile-label">事故类型</div>
                <div class="profile-value">{{ getTypeName(caseData.type) }}</div>
              </div>
            </div>
            <div class="profile-item">
              <div class="profile-icon" style="background:#fff7ed;color:#f59e0b"><el-icon><CircleClose /></el-icon></div>
              <div>
                <div class="profile-label">严重程度</div>
                <div class="profile-value">{{ getSeverityName(caseData.severity) }}</div>
              </div>
            </div>
            <div class="profile-item">
              <div class="profile-icon" style="background:#eff6ff;color:#3b82f6"><el-icon><Location /></el-icon></div>
              <div>
                <div class="profile-label">发生地点</div>
                <div class="profile-value">{{ caseData.location }}</div>
              </div>
            </div>
            <div class="profile-item">
              <div class="profile-icon" style="background:#f0fdf4;color:#10b981"><el-icon><Calendar /></el-icon></div>
              <div>
                <div class="profile-label">发生日期</div>
                <div class="profile-value">{{ caseData.date }}</div>
              </div>
            </div>
          </div>

          <!-- 损失明细 -->
          <div v-if="caseData.lossBreakdown?.length" class="loss-breakdown">
            <div class="loss-title">损失明细（万元）</div>
            <div v-for="(item, idx) in caseData.lossBreakdown" :key="idx" class="loss-row">
              <div class="loss-row-head">
                <span>{{ item.label }}</span>
                <span class="loss-amount">{{ item.amount }}</span>
              </div>
              <el-progress
                :percentage="lossPercent(item.amount)"
                :show-text="false"
                :stroke-width="8"
                :color="lossColor(idx)"
              />
            </div>
          </div>
        </el-card>

        <!-- 相关案例推荐 -->
        <el-card v-if="relatedCases.length" class="section-card">
          <template #header>
            <span class="section-title sm"><el-icon><Files /></el-icon> 相关案例</span>
          </template>
          <div class="related-list">
            <div
              v-for="item in relatedCases"
              :key="item.id"
              class="related-item"
              @click="goToCase(item.id)"
            >
              <span class="related-sev" :style="{ background: severityColor(item.severity) }"></span>
              <div class="related-body">
                <div class="related-title">{{ item.title }}</div>
                <div class="related-meta">
                  <el-tag size="small" effect="plain">{{ getTypeName(item.type) }}</el-tag>
                  <span>{{ item.location }}</span>
                </div>
              </div>
              <el-icon class="related-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>

        <!-- 学习笔记 -->
        <NotePanel
          v-if="caseData.id"
          class="section-card"
          target-type="case"
          :target-id="caseData.id"
        />

        <!-- 相关资料 -->
        <el-card class="section-card">
          <template #header>
            <span class="section-title sm"><el-icon><Link /></el-icon> 相关资料</span>
          </template>
          <div class="reference-list">
            <div v-for="(ref, index) in caseData.references" :key="index" class="reference-item">
              <el-icon><Document /></el-icon>
              <span>{{ ref }}</span>
            </div>
            <div v-if="!caseData.references.length" class="text-gray-400 text-sm">暂无相关资料</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部学习闭环 -->
    <div class="learning-footer">
      <el-button
        class="footer-nav"
        :disabled="!prevCaseId"
        @click="prevCaseId && goToCase(prevCaseId)"
      >
        <el-icon><ArrowLeft /></el-icon> 上一案例
      </el-button>
      <div class="footer-center">
        <el-button class="footer-review" type="primary" @click="openGuide">
          <el-icon><Reading /></el-icon> 复盘引导
        </el-button>
      </div>
      <el-button
        class="footer-nav"
        :disabled="!nextCaseId"
        @click="nextCaseId && goToCase(nextCaseId)"
      >
        下一案例 <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <CaseReviewGuide
      v-model="guideVisible"
      :case-id="caseData.id"
      :case-title="caseData.title"
      :case-data="caseData"
      @completed="onGuideCompleted"
      @progress="onGuideProgress"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Location, Calendar, Link, ArrowLeft, ArrowRight, Clock, Money, User, Timer,
  Histogram, Right, Connection, MagicStick, Document, Warning, CircleClose,
  Files, Reading,
} from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import FavoriteButton from '@/components/learning/FavoriteButton.vue'
import NotePanel from '@/components/learning/NotePanel.vue'
import CaseReviewGuide from '@/components/cases/CaseReviewGuide.vue'

const route = useRoute()
const router = useRouter()
const { p } = useAppBase()

interface TimelineEvent { id: string; time: string; title: string; description: string; type: string }
interface LossItem { label: string; amount: number }
interface RelatedCase { id: string; title: string; location: string; date: string; type: string; severity: string }

const caseData = ref({
  id: '', title: '', location: '', date: '', type: 'fire', severity: 'major',
  description: '', timeline: [] as TimelineEvent[], causeAnalysis: '', lossEstimate: '',
  lessonsLearned: '', references: [] as string[],
  directCause: '', indirectCause: '', rootCause: '', responsibleParty: '',
  casualties: '', lossAmount: 0, lossBreakdown: [] as LossItem[],
  lessons: [] as string[], difficulty: '', studyMinutes: 0,
})

const relatedCases = ref<RelatedCase[]>([])
const allCaseIds = ref<string[]>([])
const guideVisible = ref(false)
const caseProgress = ref({ completed: false, currentStep: 0, totalSteps: 0 })

const hasStructuredCause = computed(() =>
  !!(caseData.value.directCause || caseData.value.indirectCause || caseData.value.rootCause))

const currentIndex = computed(() => allCaseIds.value.indexOf(caseData.value.id))
const prevCaseId = computed(() => currentIndex.value > 0 ? allCaseIds.value[currentIndex.value - 1] : '')
const nextCaseId = computed(() =>
  currentIndex.value >= 0 && currentIndex.value < allCaseIds.value.length - 1
    ? allCaseIds.value[currentIndex.value + 1] : '')

const totalLoss = computed(() =>
  (caseData.value.lossBreakdown || []).reduce((sum, i) => sum + (i.amount || 0), 0))

const timelineSpan = computed(() => {
  const tl = caseData.value.timeline
  if (!tl?.length) return '—'
  const toMin = (t: string) => {
    const [h, m] = t.split(':').map(Number)
    return (h || 0) * 60 + (m || 0)
  }
  const diff = toMin(tl[tl.length - 1].time) - toMin(tl[0].time)
  if (diff <= 0) return '—'
  const h = Math.floor(diff / 60), m = diff % 60
  return h > 0 ? `${h}时${m}分` : `${m}分`
})

async function loadCase() {
  const caseId = route.params.id as string
  const [detailRes, relatedRes, listRes, progressRes] = await Promise.all([
    request.get(`/cases/${caseId}`),
    request.get(`/cases/${caseId}/related`).catch(() => ({ data: [] })),
    request.get('/cases').catch(() => ({ data: [] })),
    request.get(`/cases/${caseId}/progress`).catch(() => ({ data: {} })),
  ])
  caseData.value = { ...caseData.value, ...detailRes.data }
  relatedCases.value = relatedRes.data || []
  allCaseIds.value = (listRes.data || []).map((c: RelatedCase) => c.id)
  const p = progressRes.data || {}
  caseProgress.value = {
    completed: !!p.completed,
    currentStep: p.currentStep || 0,
    totalSteps: p.totalSteps || 0,
  }
  if (route.query.review === '1') {
    guideVisible.value = true
  }
}

onMounted(loadCase)
watch(() => route.params.id, loadCase)

function openGuide() {
  guideVisible.value = true
}

function onGuideCompleted() {
  caseProgress.value.completed = true
  ElMessage.success('案例复盘已完成')
}

function onGuideProgress(payload: { completed: boolean; currentStep: number; totalSteps: number }) {
  caseProgress.value = { ...payload }
}

function goToCase(id: string) {
  router.push(p(`/cases/${id}`))
}

// ===== 映射 =====
function getSeverityName(s: string) {
  const map: Record<string, string> = { minor: '轻微', moderate: '中等', major: '严重', critical: '特别严重', severe: '特别严重' }
  return map[s] || s
}

function severityColor(s: string) {
  const map: Record<string, string> = {
    critical: '#ef4444', severe: '#ef4444', major: '#f59e0b', moderate: '#eab308', minor: '#3b82f6',
  }
  return map[s] || '#6b7280'
}

function getTypeName(type: string) {
  const map: Record<string, string> = { fire: '火灾', explosion: '爆炸', thermal_runaway: '热失控', gas_leak: '气体泄漏' }
  return map[type] || type
}

function typeIcon(type: string) {
  const map: Record<string, any> = {
    fire: Warning, explosion: CircleClose, thermal_runaway: Histogram, gas_leak: Connection,
  }
  return map[type] || Warning
}

function difficultyLabel(d: string) {
  const map: Record<string, string> = { basic: '基础案例', intermediate: '进阶案例', advanced: '高难案例' }
  return map[d] || d
}

function difficultyType(d: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    basic: 'success', intermediate: 'warning', advanced: 'danger',
  }
  return map[d] || 'info'
}

// 时间线阶段
function phaseColor(type: string) {
  const map: Record<string, string> = {
    warning: '#f59e0b', danger: '#ef4444', action: '#3b82f6', success: '#10b981', info: '#10b981',
  }
  return map[type] || '#9ca3af'
}

function phaseBg(type: string) {
  const map: Record<string, string> = {
    warning: '#fff7ed', danger: '#fef2f2', action: '#eff6ff', success: '#f0fdf4', info: '#f0fdf4',
  }
  return map[type] || '#f3f4f6'
}

function phaseLabel(type: string) {
  const map: Record<string, string> = {
    warning: '预警', danger: '发生 / 扩大', action: '处置', success: '控制', info: '控制',
  }
  return map[type] || '事件'
}

function isKeyEvent(event: TimelineEvent, idx: number) {
  return idx === 0 || idx === caseData.value.timeline.length - 1 || event.type === 'danger'
}

function lossPercent(amount: number) {
  return totalLoss.value > 0 ? Math.round((amount / totalLoss.value) * 100) : 0
}

function lossColor(idx: number) {
  return ['#ef4444', '#f59e0b', '#3b82f6', '#8b5cf6'][idx % 4]
}
</script>

<style scoped>
.case-detail {
  padding-bottom: 88px;
}

/* 顶部返回栏 */
.detail-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 12px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  color: #4b5563;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #f5f3ff;
  border-color: #c7d2fe;
  color: #4f46e5;
}

/* 事故概览卡 */
.overview-card {
  display: flex;
  gap: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.scene-visual {
  position: relative;
  width: 220px;
  flex-shrink: 0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}

.scene-fire { background: linear-gradient(135deg, #ef4444, #f97316); }
.scene-explosion { background: linear-gradient(135deg, #f59e0b, #ef4444); }
.scene-thermal_runaway { background: linear-gradient(135deg, #f97316, #ef4444); }
.scene-gas_leak { background: linear-gradient(135deg, #14b8a6, #3b82f6); }

.scene-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.overview-main {
  flex: 1;
  min-width: 0;
}

.overview-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.severity-pill {
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.overview-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px;
}

.overview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 14px;
}

.overview-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.overview-desc {
  font-size: 15px;
  line-height: 1.8;
  color: #374151;
  margin: 0 0 20px;
}

/* 关键数据 */
.key-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.key-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 160px;
  padding: 14px 18px;
  background: #f9fafb;
  border-radius: 12px;
}

.key-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.key-icon.loss { background: linear-gradient(135deg, #ef4444, #f97316); }
.key-icon.people { background: linear-gradient(135deg, #3b82f6, #6366f1); }
.key-icon.time { background: linear-gradient(135deg, #8b5cf6, #a855f7); }

.key-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  line-height: 1.1;
}

.key-value.sm {
  font-size: 16px;
}

.key-unit {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  margin-left: 2px;
}

.key-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

/* 区块卡片 */
.section-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.section-title.sm {
  font-size: 15px;
}

.section-title .el-icon {
  color: #6366f1;
}

/* 阶段图例 */
.phase-legend {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: #f9fafb;
  border-radius: 8px;
  font-size: 13px;
  color: #4b5563;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.legend-item .dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}

.legend-arrow {
  color: #d1d5db;
}

/* 时间线 */
.timeline-event {
  padding: 4px 0;
}

.timeline-event.key {
  padding: 12px 16px;
  background: #f9fafb;
  border-left: 3px solid;
  border-radius: 0 8px 8px 0;
}

.event-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.event-phase {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}

.event-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.event-desc {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0;
}

/* 原因分析分层 */
.cause-layers {
  display: flex;
  flex-direction: column;
}

.cause-card {
  position: relative;
  padding: 16px 18px;
  border-radius: 10px;
  border-left: 4px solid;
}

.cause-card p {
  margin: 0;
  line-height: 1.7;
  color: #374151;
}

.cause-tag {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
}

.cause-card.direct {
  background: #fffbeb;
  border-color: #f59e0b;
}
.cause-card.direct .cause-tag { color: #b45309; }

.cause-card.indirect {
  background: #fff7ed;
  border-color: #f97316;
}
.cause-card.indirect .cause-tag { color: #c2410c; }

.cause-card.root {
  background: #fef2f2;
  border-color: #ef4444;
}
.cause-card.root .cause-tag { color: #b91c1c; }
.cause-card.root p { font-weight: 500; color: #1f2937; }

.cause-connector {
  width: 2px;
  height: 16px;
  background: #e5e7eb;
  margin-left: 24px;
}

.fallback-text {
  line-height: 1.8;
  color: #374151;
  margin: 0;
}

/* 经验教训 */
.lessons-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.lesson-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f3ff;
  border-radius: 10px;
}

.lesson-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.lesson-text {
  line-height: 1.6;
  color: #374151;
  padding-top: 2px;
}

/* 事故概况卡片组 */
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.profile-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-label {
  font-size: 12px;
  color: #9ca3af;
}

.profile-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

/* 损失明细 */
.loss-breakdown {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e5e7eb;
}

.loss-title {
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
  margin-bottom: 12px;
}

.loss-row {
  margin-bottom: 12px;
}

.loss-row-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 4px;
}

.loss-amount {
  font-weight: 600;
  color: #111827;
}

/* 相关案例 */
.related-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.related-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.related-item:hover {
  background: #f5f3ff;
}

.related-sev {
  width: 4px;
  align-self: stretch;
  border-radius: 4px;
  flex-shrink: 0;
}

.related-body {
  flex: 1;
  min-width: 0;
}

.related-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 6px;
  line-height: 1.4;
}

.related-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #9ca3af;
}

.related-arrow {
  color: #c7d2fe;
  flex-shrink: 0;
}

/* 相关资料 */
.reference-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reference-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #4b5563;
}

.reference-item .el-icon {
  color: #8b5cf6;
}

/* 底部学习闭环 */
.learning-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 32px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-top: 1px solid #eef0f3;
  box-shadow: 0 -4px 16px rgba(15, 23, 42, 0.05);
  z-index: 10;
}

.footer-center {
  display: flex;
  gap: 12px;
}

.footer-nav {
  height: 42px;
}

.footer-review {
  height: 42px;
  padding: 0 22px;
}

.footer-complete {
  height: 42px;
  padding: 0 28px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.footer-complete:hover {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
}

@media (max-width: 992px) {
  .overview-card {
    flex-direction: column;
  }
  .scene-visual {
    width: 100%;
    height: 140px;
  }
  .learning-footer {
    padding: 12px 16px;
    flex-wrap: wrap;
  }
}
</style>
