<template>
  <div class="sl-page case-list">
    <div class="page-hero">
      <div class="hero-text">
        <h2 class="sl-page-title">储能事故案例库</h2>
        <p class="hero-desc">基于真实事故档案 · 支持案例复盘与事故推演训练</p>
      </div>
      <el-input
        v-model="searchText"
        placeholder="搜索案例名称、地点..."
        prefix-icon="Search"
        class="search-input"
        clearable
      />
    </div>

    <!-- 筛选栏 -->
    <div class="filter-panel">
      <div class="filter-group">
        <span class="filter-label">事故类型</span>
        <div class="filter-pills">
          <button
            v-for="type in caseTypes"
            :key="type.value"
            type="button"
            class="filter-pill"
            :class="{ active: activeType === type.value }"
            @click="activeType = type.value"
          >
            {{ type.label }}
          </button>
        </div>
      </div>
      <div class="filter-group">
        <span class="filter-label">严重等级</span>
        <div class="filter-pills">
          <button
            v-for="sev in severityFilters"
            :key="sev.value"
            type="button"
            class="filter-pill"
            :class="{ active: activeSeverity === sev.value, [`pill-${sev.value}`]: sev.value !== 'all' }"
            @click="activeSeverity = sev.value"
          >
            {{ sev.label }}
          </button>
        </div>
      </div>
      <div class="filter-group">
        <span class="filter-label">发生时间</span>
        <div class="filter-pills">
          <button
            v-for="period in timeFilters"
            :key="period.value"
            type="button"
            class="filter-pill"
            :class="{ active: activeTime === period.value }"
            @click="activeTime = period.value"
          >
            {{ period.label }}
          </button>
        </div>
      </div>
      <div class="filter-footer">
        <span class="result-count">共 {{ filteredCases.length }} 条案例</span>
        <div class="sort-control">
          <span class="sort-label">排序</span>
          <el-select v-model="sortBy" class="sort-select" size="small">
            <el-option label="时间最新" value="latest" />
            <el-option label="严重等级" value="severity" />
            <el-option label="未复盘优先" value="popular" />
          </el-select>
        </div>
      </div>
    </div>

    <el-skeleton v-if="loading" animated :rows="8" />
    <template v-else>
      <el-empty v-if="!filteredCases.length" description="暂无匹配案例" />
      <div v-else class="case-grid">
        <article
          v-for="caseItem in filteredCases"
          :key="caseItem.id"
          class="case-card"
          :class="severityCardClass(caseItem.severity)"
          @click="openCase(caseItem.id)"
        >
          <div class="card-accent" :class="severityCardClass(caseItem.severity)" />

          <header class="card-top">
            <div class="severity-badge" :class="severityCardClass(caseItem.severity)">
              {{ getSeverityName(caseItem.severity) }}
            </div>
            <div class="card-top-meta">
              <span class="meta-date">{{ caseItem.date }}</span>
              <span class="meta-loc">{{ caseItem.location }}</span>
            </div>
          </header>

          <h3 class="case-title">{{ caseItem.title }}</h3>

          <div v-if="metaTags(caseItem).length" class="meta-tags">
            <span v-for="tag in metaTags(caseItem)" :key="tag" class="meta-tag">{{ tag }}</span>
          </div>

          <p class="case-desc">{{ briefDescription(caseItem) }}</p>

          <div class="case-timeline">
            <template v-if="caseItem.timeline?.length">
              <div
                v-for="(evt, idx) in caseItem.timeline.slice(0, 3)"
                :key="evt.id || idx"
                class="timeline-node"
                :class="{ 'is-last': idx === Math.min(2, caseItem.timeline.length - 1) }"
              >
                <div class="timeline-track">
                  <span class="timeline-dot" :class="evtTypeClass(evt.type)" />
                  <span v-if="idx < Math.min(2, caseItem.timeline.length - 1)" class="timeline-line" />
                </div>
                <div class="timeline-body">
                  <span v-if="evt.time && evt.time !== '—'" class="timeline-time">{{ evt.time }}</span>
                  <span class="timeline-title">{{ evt.title }}</span>
                </div>
              </div>
            </template>
            <div v-else class="timeline-empty">暂无时间线摘要</div>
          </div>

          <footer class="card-footer">
            <span class="type-chip" :class="`type-${caseItem.type}`">
              {{ getTypeName(caseItem.type) }}
            </span>
            <el-tag
              v-if="progressMap[caseItem.id]?.completed"
              size="small"
              type="success"
              effect="plain"
              class="reviewed-tag"
            >
              已复盘
            </el-tag>
            <button
              v-if="hasCaseSimulation(caseItem.id)"
              type="button"
              class="enter-btn"
              @click.stop="enterDeduction(caseItem.id)"
            >
              进入推演
            </button>
          </footer>
        </article>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAppBase } from '@/composables/useAppBase'
import {
  getCaseSimulationPath,
  hasCaseSimulation,
  parseCaseMetaTags,
  stripMetaFromDescription,
} from '@/cases/caseSimulationLinks'
import type { AccidentCase } from '@/types'

const router = useRouter()
const { p } = useAppBase()

const searchText = ref('')
const activeType = ref('all')
const activeSeverity = ref('all')
const activeTime = ref('all')
const sortBy = ref<'latest' | 'severity' | 'popular'>('latest')

const caseTypes = [
  { label: '全部', value: 'all' },
  { label: '火灾', value: 'fire' },
  { label: '爆炸', value: 'explosion' },
  { label: '热失控', value: 'thermal_runaway' },
  { label: '气体泄漏', value: 'gas_leak' },
]

const severityFilters = [
  { label: '全部', value: 'all' },
  { label: '特别严重', value: 'critical' },
  { label: '严重', value: 'major' },
  { label: '中等', value: 'moderate' },
  { label: '轻微', value: 'minor' },
]

const timeFilters = [
  { label: '全部', value: 'all' },
  { label: '2025年', value: '2025' },
  { label: '2024年', value: '2024' },
  { label: '2023及以前', value: 'older' },
]

const severityWeight: Record<string, number> = {
  critical: 5,
  severe: 5,
  major: 4,
  moderate: 3,
  minor: 2,
}

const cases = ref<AccidentCase[]>([])
const loading = ref(true)
const progressMap = ref<Record<string, { completed: boolean }>>({})

function openCase(id: string) {
  router.push(p(`/cases/${id}`))
}

function enterDeduction(id: string) {
  const simPath = getCaseSimulationPath(id)
  if (simPath) {
    router.push(p(simPath))
  }
}

function metaTags(caseItem: AccidentCase) {
  const { battery, station } = parseCaseMetaTags(caseItem.description)
  const tags: string[] = []
  if (battery) tags.push(battery)
  if (station) tags.push(station)
  return tags
}

function briefDescription(caseItem: AccidentCase) {
  const stripped = stripMetaFromDescription(caseItem.description)
  return stripped || caseItem.description
}

function severityCardClass(severity: string) {
  if (severity === 'critical' || severity === 'severe') return 'sev-critical'
  if (severity === 'major') return 'sev-major'
  if (severity === 'moderate') return 'sev-moderate'
  return 'sev-minor'
}

function evtTypeClass(type?: string) {
  if (type === 'danger' || type === 'escalation') return 'dot-danger'
  if (type === 'action' || type === 'response') return 'dot-action'
  if (type === 'warning' || type === 'alarm' || type === 'detection') return 'dot-warning'
  return 'dot-info'
}

function matchTimePeriod(date: string, period: string) {
  if (period === 'all') return true
  const year = parseInt(date.slice(0, 4), 10)
  if (period === '2025') return year === 2025
  if (period === '2024') return year === 2024
  if (period === 'older') return year <= 2023
  return true
}

function matchSeverity(severity: string, filter: string) {
  if (filter === 'all') return true
  if (filter === 'critical') return severity === 'critical' || severity === 'severe'
  return severity === filter
}

onMounted(async () => {
  loading.value = true
  try {
    const [casesRes, progressRes] = await Promise.all([
      request.get('/cases'),
      request.get('/cases/progress/summary').catch(() => ({ data: [] })),
    ])
    cases.value = casesRes.data
    const map: Record<string, { completed: boolean }> = {}
    for (const item of progressRes.data || []) {
      map[item.caseId] = { completed: !!item.completed }
    }
    progressMap.value = map
  } catch (error) {
    console.error('加载案例失败', error)
    ElMessage.error('加载案例失败，请稍后重试')
  } finally {
    loading.value = false
  }
})

const filteredCases = computed(() => {
  let result = [...cases.value]
  if (activeType.value !== 'all') {
    result = result.filter(c => c.type === activeType.value)
  }
  if (activeSeverity.value !== 'all') {
    result = result.filter(c => matchSeverity(c.severity, activeSeverity.value))
  }
  if (activeTime.value !== 'all') {
    result = result.filter(c => matchTimePeriod(c.date, activeTime.value))
  }
  if (searchText.value) {
    const search = searchText.value.toLowerCase()
    result = result.filter(c =>
      c.title.toLowerCase().includes(search) ||
      c.location.toLowerCase().includes(search) ||
      c.description.toLowerCase().includes(search),
    )
  }
  if (sortBy.value === 'popular') {
    result.sort((a, b) => {
      const aDone = progressMap.value[a.id]?.completed ? 1 : 0
      const bDone = progressMap.value[b.id]?.completed ? 1 : 0
      if (aDone !== bDone) return aDone - bDone
      return (severityWeight[b.severity] || 0) - (severityWeight[a.severity] || 0)
    })
  } else if (sortBy.value === 'severity') {
    result.sort((a, b) => (severityWeight[b.severity] || 0) - (severityWeight[a.severity] || 0))
  } else {
    result.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
  }
  return result
})

function getSeverityName(severity: string) {
  const map: Record<string, string> = {
    minor: '轻微',
    moderate: '中等',
    major: '严重',
    critical: '特别严重',
    severe: '特别严重',
  }
  return map[severity] || severity
}

function getTypeName(type: string) {
  const map: Record<string, string> = {
    fire: '火灾',
    explosion: '爆炸',
    thermal_runaway: '热失控',
    gas_leak: '气体泄漏',
  }
  return map[type] || type
}
</script>

<style scoped>
.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.hero-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: #64748b;
}

.search-input {
  width: min(320px, 100%);
}

/* ── 筛选面板 ── */
.filter-panel {
  background: #fff;
  border: 1px solid #e8edf4;
  border-radius: 14px;
  padding: 18px 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.filter-group {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 14px;
}

.filter-group:last-of-type {
  margin-bottom: 0;
}

.filter-label {
  flex-shrink: 0;
  width: 64px;
  padding-top: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.filter-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.filter-pill {
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  color: #475569;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.4;
}

.filter-pill:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
}

.filter-pill.active {
  background: #1e3a5f;
  border-color: #1e3a5f;
  color: #fff;
  font-weight: 500;
}

.filter-pill.pill-critical.active { background: #991b1b; border-color: #991b1b; }
.filter-pill.pill-major.active { background: #c2410c; border-color: #c2410c; }
.filter-pill.pill-moderate.active { background: #b45309; border-color: #b45309; }
.filter-pill.pill-minor.active { background: #475569; border-color: #475569; }

.filter-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.result-count {
  font-size: 13px;
  color: #94a3b8;
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 13px;
  color: #64748b;
}

.sort-select {
  width: 120px;
}

/* ── 卡片网格 ── */
.case-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1200px) {
  .case-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
  .case-grid { grid-template-columns: minmax(0, 1fr); }
  .filter-group { flex-direction: column; gap: 8px; }
  .filter-label { padding-top: 0; }
}

/* ── 案例卡片 ── */
.case-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 18px 20px 16px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8edf4;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.28s cubic-bezier(0.34, 1.2, 0.64, 1), box-shadow 0.28s ease;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.05);
}

.case-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
}

.case-card.sev-critical {
  background: linear-gradient(135deg, #fff 0%, #fff5f5 100%);
  border-color: rgba(220, 38, 38, 0.22);
}
.case-card.sev-major {
  background: linear-gradient(135deg, #fff 0%, #fff7ed 100%);
  border-color: rgba(234, 88, 12, 0.2);
}
.case-card.sev-moderate {
  background: linear-gradient(135deg, #fff 0%, #fffbeb 100%);
  border-color: rgba(217, 119, 6, 0.18);
}
.case-card.sev-minor {
  background: linear-gradient(135deg, #fff 0%, #f8fafc 100%);
  border-color: #e2e8f0;
}

.card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.card-accent.sev-critical { background: linear-gradient(90deg, #dc2626, #f87171); }
.card-accent.sev-major { background: linear-gradient(90deg, #ea580c, #fb923c); }
.card-accent.sev-moderate { background: linear-gradient(90deg, #d97706, #fbbf24); }
.card-accent.sev-minor { background: linear-gradient(90deg, #64748b, #94a3b8); }

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.severity-badge {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.severity-badge.sev-critical { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.severity-badge.sev-major { background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; }
.severity-badge.sev-moderate { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }
.severity-badge.sev-minor { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; }

.card-top-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  font-size: 12px;
  color: #94a3b8;
}

.meta-date,
.meta-loc {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-loc::before {
  content: '·';
  margin-right: 10px;
  color: #cbd5e1;
}

.case-title {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.meta-tag {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  color: #475569;
  background: rgba(30, 58, 95, 0.06);
  border: 1px solid rgba(30, 58, 95, 0.1);
}

.case-desc {
  margin: 0 0 14px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── 时间轴 ── */
.case-timeline {
  flex: 1;
  min-height: 88px;
  margin-bottom: 14px;
  padding: 4px 0;
}

.timeline-empty {
  font-size: 12px;
  color: #cbd5e1;
  padding: 8px 0;
}

.timeline-node {
  display: flex;
  gap: 10px;
  min-height: 28px;
}

.timeline-track {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 12px;
  flex-shrink: 0;
  padding-top: 5px;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #cbd5e1;
  flex-shrink: 0;
  z-index: 1;
}

.timeline-dot.dot-danger { background: #ef4444; box-shadow: 0 0 0 1px #fecaca; }
.timeline-dot.dot-warning { background: #f59e0b; box-shadow: 0 0 0 1px #fde68a; }
.timeline-dot.dot-action { background: #3b82f6; box-shadow: 0 0 0 1px #bfdbfe; }
.timeline-dot.dot-info { background: #64748b; box-shadow: 0 0 0 1px #e2e8f0; }

.timeline-line {
  flex: 1;
  width: 2px;
  min-height: 14px;
  margin: 2px 0;
  background: linear-gradient(180deg, #cbd5e1, #e2e8f0);
  border-radius: 1px;
}

.timeline-body {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  padding-bottom: 8px;
  min-width: 0;
}

.timeline-time {
  font-size: 11px;
  font-weight: 600;
  color: #1e3a5f;
  font-family: 'Consolas', 'SF Mono', monospace;
  flex-shrink: 0;
}

.timeline-title {
  font-size: 12px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 底部操作 ── */
.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.type-chip {
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}
.type-chip.type-fire { background: #fef2f2; color: #dc2626; }
.type-chip.type-explosion { background: #fff7ed; color: #ea580c; }
.type-chip.type-thermal_runaway { background: #fffbeb; color: #d97706; }
.type-chip.type-gas_leak { background: #eff6ff; color: #2563eb; }

.reviewed-tag {
  margin-right: auto;
}

.enter-btn {
  margin-left: auto;
  padding: 7px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #1e3a5f, #2b5aed);
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s, transform 0.15s;
  box-shadow: 0 2px 8px rgba(43, 90, 237, 0.25);
  white-space: nowrap;
}

.enter-btn:hover {
  background: linear-gradient(135deg, #152a47, #2348d4);
  box-shadow: 0 4px 14px rgba(43, 90, 237, 0.35);
  transform: translateY(-1px);
}

.case-card:hover .enter-btn {
  box-shadow: 0 4px 16px rgba(43, 90, 237, 0.4);
}
</style>
