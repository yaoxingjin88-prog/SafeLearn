<template>
  <div class="monitoring-page" v-loading="loading">
    <div class="course-toolbar">
      <span class="toolbar-label">选择课程</span>
      <el-select
        v-model="selectedCourseId"
        filterable
        placeholder="请选择课程"
        class="course-select"
        @change="onCourseChange"
      >
        <el-option
          v-for="course in courseOptions"
          :key="course.id"
          :label="course.title"
          :value="course.id"
        />
      </el-select>
    </div>

    <template v-if="summary">
      <div class="summary-card">
        <img :src="coverUrl(summary.course.coverImage)" alt="" class="summary-cover" />
        <div class="summary-info">
          <h1>{{ summary.course.title }}</h1>
          <div class="summary-tags">
            <el-tag size="small">{{ summary.course.categoryName || summary.course.category }}</el-tag>
            <el-tag size="small" type="info">{{ formatDuration(summary.course.totalDuration) }}</el-tag>
            <el-tag size="small" type="success">{{ statusLabel(summary.course.status) }}</el-tag>
          </div>
          <p v-if="summary.course.publishedAt" class="publish-time">发布时间：{{ summary.course.publishedAt }}</p>
        </div>
        <div class="summary-stats">
          <div v-for="item in summaryStatItems" :key="item.label" class="summary-stat">
            <div class="stat-label-wrap">
              <span class="stat-label">{{ item.label }}</span>
              <el-tag v-if="item.warning" size="small" type="danger" effect="plain" class="warn-tag">预警</el-tag>
            </div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>

      <div class="filter-bar">
        <div class="filter-field">
          <span class="filter-label">学习状态</span>
          <el-select v-model="filters.learningStatus" placeholder="全部状态" class="filter-item">
            <el-option v-for="item in LEARNING_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">所属部门</span>
          <el-select v-model="filters.department" placeholder="全部部门" class="filter-item">
            <el-option v-for="dept in departmentOptions" :key="dept" :label="dept === 'all' ? '全部' : dept" :value="dept" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">预警状态</span>
          <el-select v-model="filters.warningStatus" placeholder="全部预警" class="filter-item">
            <el-option v-for="item in WARNING_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field filter-field-search">
          <span class="filter-label">关键词</span>
          <el-input
            v-model="filters.keyword"
            placeholder="学员姓名、工号"
            clearable
            class="filter-search"
            @keyup.enter="search"
          />
        </div>
        <div class="filter-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="exportData">导出数据</el-button>
        </div>
      </div>

      <div class="table-card">
        <table class="monitor-table">
          <thead>
            <tr>
              <th>学员信息</th>
              <th>所属部门</th>
              <th>学习进度</th>
              <th>学习时长</th>
              <th>考试得分</th>
              <th>证书状态</th>
              <th>预警状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in learners" :key="row.userId">
              <td class="user-cell">
                <el-avatar :size="36" :src="row.avatarUrl">{{ row.username?.charAt(0) }}</el-avatar>
                <div>
                  <strong>{{ row.username }}</strong>
                  <span>{{ row.employeeNo }}</span>
                </div>
              </td>
              <td>{{ row.department }}</td>
              <td class="progress-cell">
                <div class="progress-wrap">
                  <div class="progress-bar"><i :style="{ width: `${Math.min(100, row.progress)}%` }" /></div>
                  <span>{{ row.progress }}%</span>
                </div>
              </td>
              <td>{{ row.studyDuration || '00:00:00' }}</td>
              <td>{{ row.examScore || (row.learningStatus === 'not_started' ? '-' : 0) }}</td>
              <td><span class="status-pill" :class="certClass(row.certificateStatus)">{{ certLabel(row.certificateStatus) }}</span></td>
              <td>
                <span v-if="row.warningStatus === 'none'" class="muted">-</span>
                <span v-else class="status-pill" :class="warnClass(row.warningStatus)">{{ warnLabel(row.warningStatus) }}</span>
              </td>
              <td><button type="button" class="link-btn" @click="openDetail(row)">查看详情</button></td>
            </tr>
          </tbody>
        </table>
        <el-empty v-if="!loading && !learners.length" description="暂无学员数据" />
      </div>

      <div class="pagination-bar">
        <span>共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          @current-change="loadLearners"
          @size-change="onPageSizeChange"
        />
      </div>
    </template>

    <el-empty v-else-if="!loading && !courseOptions.length" description="暂无已发布课程" />

    <el-dialog v-model="detailVisible" :title="`${detail?.username || ''} · 学习详情`" width="720px">
      <template v-if="detail">
        <div class="detail-meta">
          <span>工号：{{ detail.employeeNo }}</span>
          <span>部门：{{ detail.department || '-' }}</span>
          <span>课程：{{ detail.courseTitle }}</span>
        </div>
        <table class="detail-table">
          <thead>
            <tr>
              <th>章节</th>
              <th>进度</th>
              <th>学习时长</th>
              <th>测验得分</th>
              <th>最近学习</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ch in detail.chapters" :key="ch.chapterId">
              <td>{{ String(ch.order || 0).padStart(2, '0') }} {{ ch.title }}</td>
              <td>{{ ch.completed ? '100%' : `${ch.progress || 0}%` }}</td>
              <td>{{ formatSeconds(ch.studySeconds) }}</td>
              <td>{{ ch.quizScore ?? '-' }}</td>
              <td>{{ ch.lastAccessAt || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  adminApi,
  type AdminCourseLearnerDetail,
  type AdminCourseLearnerItem,
  type AdminCourseListItem,
  type AdminCourseMonitoringSummary,
} from '@/api/admin'
import { coverUrl, formatDuration, statusLabel } from './courseAdminShared'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const courseOptions = ref<AdminCourseListItem[]>([])
const selectedCourseId = ref('')
const summary = ref<AdminCourseMonitoringSummary | null>(null)
const learners = ref<AdminCourseLearnerItem[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<AdminCourseLearnerDetail | null>(null)

const filters = reactive({
  keyword: '',
  department: 'all',
  learningStatus: 'all',
  warningStatus: 'all',
  page: 1,
  pageSize: 10,
})

const LEARNING_STATUS_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '未开始', value: 'not_started' },
  { label: '学习中', value: 'in_progress' },
  { label: '已完成', value: 'completed' },
]

const WARNING_STATUS_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '无预警', value: 'none' },
  { label: '未开始学习', value: 'not_started' },
  { label: '进度偏低', value: 'low_progress' },
]

const departmentOptions = computed(() => {
  const depts = summary.value?.departments || ['全部']
  return depts.map(d => (d === '全部' ? 'all' : d))
})

const summaryStatItems = computed(() => {
  const stats = summary.value?.stats
  if (!stats) return []
  return [
    { label: '应学人数', value: stats.expectedCount },
    { label: '已完成', value: stats.completedCount },
    { label: '完成率', value: `${stats.completionRate}%` },
    { label: '平均得分', value: stats.avgScore },
    { label: '未完成人数', value: stats.incompleteCount, warning: stats.incompleteCount > 0 },
  ]
})

function certLabel(status: AdminCourseLearnerItem['certificateStatus']) {
  const map = { obtained: '已获得', not_obtained: '未获得', not_started: '未开始' }
  return map[status] || '-'
}

function certClass(status: AdminCourseLearnerItem['certificateStatus']) {
  return `cert-${status}`
}

function warnLabel(status: AdminCourseLearnerItem['warningStatus']) {
  const map = { not_started: '未开始学习', low_progress: '进度偏低', none: '-' }
  return map[status] || '-'
}

function warnClass(status: AdminCourseLearnerItem['warningStatus']) {
  return `warn-${status}`
}

function formatSeconds(seconds?: number) {
  const total = seconds || 0
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

async function loadCourseOptions() {
  const res = await adminApi.getCourses({ status: 'published', pageSize: 200 })
  courseOptions.value = res.data.items || []
  if (!selectedCourseId.value && courseOptions.value.length) {
    const queryId = route.query.courseId as string | undefined
    selectedCourseId.value = queryId && courseOptions.value.some(c => c.id === queryId)
      ? queryId
      : courseOptions.value[0].id
  }
}

async function loadSummary() {
  if (!selectedCourseId.value) {
    summary.value = null
    return
  }
  const res = await adminApi.getCourseMonitoring(selectedCourseId.value)
  summary.value = res.data
}

async function loadLearners() {
  if (!selectedCourseId.value) {
    learners.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await adminApi.getCourseLearners(selectedCourseId.value, {
      keyword: filters.keyword || undefined,
      department: filters.department === 'all' ? undefined : filters.department,
      learningStatus: filters.learningStatus === 'all' ? undefined : filters.learningStatus,
      warningStatus: filters.warningStatus === 'all' ? undefined : filters.warningStatus,
      page: filters.page,
      pageSize: filters.pageSize,
    })
    learners.value = res.data.items || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function reloadAll() {
  if (!selectedCourseId.value) return
  loading.value = true
  try {
    await loadSummary()
    await loadLearners()
  } finally {
    loading.value = false
  }
}

function onCourseChange() {
  filters.page = 1
  router.replace({ query: { courseId: selectedCourseId.value } })
  reloadAll()
}

function search() {
  filters.page = 1
  loadLearners()
}

function resetFilters() {
  filters.keyword = ''
  filters.department = 'all'
  filters.learningStatus = 'all'
  filters.warningStatus = 'all'
  filters.page = 1
  loadLearners()
}

function onPageSizeChange() {
  filters.page = 1
  loadLearners()
}

async function openDetail(row: AdminCourseLearnerItem) {
  if (!selectedCourseId.value) return
  try {
    const res = await adminApi.getCourseLearnerDetail(selectedCourseId.value, row.userId)
    detail.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载学员详情失败，请稍后重试')
  }
}

async function exportData() {
  if (!selectedCourseId.value) return
  try {
    const res = await adminApi.getCourseLearners(selectedCourseId.value, {
      keyword: filters.keyword || undefined,
      department: filters.department === 'all' ? undefined : filters.department,
      learningStatus: filters.learningStatus === 'all' ? undefined : filters.learningStatus,
      warningStatus: filters.warningStatus === 'all' ? undefined : filters.warningStatus,
      page: 1,
      pageSize: 10000,
    })
    const rows = res.data.items || []
    const header = ['姓名', '工号', '部门', '学习进度', '学习时长', '考试得分', '证书状态', '预警状态']
    const lines = rows.map(r => [
      r.username,
      r.employeeNo,
      r.department,
      `${r.progress}%`,
      r.studyDuration,
      r.examScore,
      certLabel(r.certificateStatus),
      r.warningStatus === 'none' ? '-' : warnLabel(r.warningStatus),
    ].join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${summary.value?.course.title || '课程'}-学习监控.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

watch(() => route.query.courseId, (id) => {
  if (typeof id === 'string' && id !== selectedCourseId.value && courseOptions.value.some(c => c.id === id)) {
    selectedCourseId.value = id
    reloadAll()
  }
})

onMounted(async () => {
  loading.value = true
  try {
    await loadCourseOptions()
    await reloadAll()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.monitoring-page {
  padding: 20px 24px 24px;
  min-height: 100%;
  box-sizing: border-box;
}

.course-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-label {
  color: #667085;
  font-size: 14px;
}

.course-select {
  width: 320px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  margin-bottom: 16px;
}

.summary-cover {
  flex: 0 0 120px;
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.summary-info {
  flex: 0 0 240px;
  min-width: 0;
}

.summary-info h1 {
  margin: 0 0 8px;
  font-size: 18px;
  line-height: 1.3;
  color: #1f2937;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.publish-time {
  margin: 0;
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.4;
}

.summary-stats {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: center;
  padding: 12px 8px;
  background: #fafbfc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

.summary-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 0 8px;
  border-left: 1px solid #e8edf3;
}

.summary-stat:first-child {
  border-left: 0;
}

.stat-label-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 20px;
  margin-bottom: 8px;
  white-space: nowrap;
}

.stat-label {
  color: #9ca3af;
  font-size: 12px;
  line-height: 1;
}

.warn-tag {
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  flex-shrink: 0;
}

.summary-stat strong {
  font-size: 22px;
  line-height: 1.1;
  font-weight: 700;
  color: #1f2937;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 14px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-label {
  color: #667085;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.filter-item {
  width: 140px;
}

.filter-field-search .filter-search {
  width: 200px;
}

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 10px;
  padding-bottom: 1px;
}

.table-card {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  overflow: hidden;
}

.monitor-table {
  width: 100%;
  border-collapse: collapse;
}

.monitor-table th,
.monitor-table td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.monitor-table th {
  background: #fafbfc;
  color: #667085;
  font-weight: 600;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-cell strong {
  display: block;
  color: #1f2937;
}

.user-cell span {
  color: #9ca3af;
  font-size: 12px;
}

.progress-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 140px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #eef2ff;
  border-radius: 999px;
  overflow: hidden;
}

.progress-bar i {
  display: block;
  height: 100%;
  background: #2563eb;
  border-radius: 999px;
}

.progress-wrap span {
  width: 42px;
  color: #374151;
  font-size: 12px;
}

.status-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.cert-obtained { color: #16a34a; background: #ecfdf3; }
.cert-not_obtained { color: #dc2626; background: #fef2f2; }
.cert-not_started { color: #9ca3af; background: #f3f4f6; }
.warn-not_started { color: #dc2626; background: #fef2f2; }
.warn-low_progress { color: #ea580c; background: #fff7ed; }

.link-btn {
  border: 0;
  background: none;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
}

.muted { color: #9ca3af; }

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  color: #667085;
  font-size: 13px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
  color: #667085;
  font-size: 13px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th,
.detail-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
  font-size: 13px;
}

.detail-table th {
  background: #fafbfc;
  color: #667085;
}

@media (max-width: 1200px) {
  .summary-card {
    flex-wrap: wrap;
  }

  .summary-info {
    flex: 1;
    min-width: 200px;
  }

  .summary-stats {
    width: 100%;
    flex: none;
  }
}
</style>
