<template>
  <div class="alert-center-page" v-loading="loading">
    <nav class="page-breadcrumb" aria-label="面包屑">
      <button type="button" @click="router.push('/dashboard')">首页</button>
      <span>/</span>
      <span>安全预警</span>
      <span>/</span>
      <strong>最新安全预警</strong>
    </nav>

    <header class="page-header">
      <div>
        <h1>最新安全预警</h1>
        <p>查看、筛选与跟踪培训过程中的异常预警信息</p>
      </div>
    </header>

    <section v-if="stats" class="stats-grid">
      <article v-for="card in summaryCards" :key="card.key" class="stat-card">
        <span class="stat-icon" :class="`tone-${card.tone}`">
          <el-icon><component :is="card.icon" /></el-icon>
        </span>
        <div class="stat-content">
          <span class="stat-label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <em :class="card.subTone">{{ card.subText }}</em>
        </div>
      </article>
    </section>

    <section class="filter-panel">
      <div class="filter-grid">
        <div class="filter-field span-2">
          <span class="filter-label">关键词</span>
          <el-input v-model="filters.keyword" placeholder="搜索预警标题/学员/课程" clearable @keyup.enter="search" />
        </div>
        <div class="filter-field">
          <span class="filter-label">预警级别</span>
          <el-select v-model="filters.level" class="w-full">
            <el-option v-for="item in ALERT_LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">预警类型</span>
          <el-select v-model="filters.type" class="w-full">
            <el-option v-for="item in ALERT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">所属部门</span>
          <el-select v-model="filters.department" class="w-full">
            <el-option v-for="dept in departmentOptions" :key="dept" :label="dept === 'all' ? '全部部门' : dept" :value="dept" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">处理状态</span>
          <el-select v-model="filters.status" class="w-full">
            <el-option v-for="item in ALERT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field span-2">
          <span class="filter-label">发生时间</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="w-full"
          />
        </div>
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <el-button @click="exportData">
          <el-icon><Upload /></el-icon>
          导出
        </el-button>
      </div>
    </section>

    <div class="content-grid">
      <article class="panel main-panel">
        <div class="table-toolbar">
          <div class="level-tabs">
            <button
              v-for="tab in ALERT_LEVEL_TAB_OPTIONS"
              :key="tab.value"
              type="button"
              :class="{ active: activeTab === tab.value }"
              @click="switchTab(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
          <el-button type="primary" @click="openCreate">+ 新增预警</el-button>
        </div>

        <div class="table-wrap">
          <table class="alert-table">
            <thead>
              <tr>
                <th>预警编号</th>
                <th>预警标题</th>
                <th>预警类型</th>
                <th>预警级别</th>
                <th>所属部门</th>
                <th>责任人</th>
                <th>发生时间</th>
                <th>处理状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in data?.items || []" :key="item.id">
                <td class="mono">{{ item.alertNo || '—' }}</td>
                <td class="title-cell">
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.description }}</p>
                </td>
                <td>{{ alertTypeLabel(item.type) }}</td>
                <td><span class="level-tag" :class="`level-${item.level}`">{{ alertLevelLabel(item.level) }}</span></td>
                <td>{{ item.department || '—' }}</td>
                <td>{{ item.responsiblePerson || '—' }}</td>
                <td>{{ item.time || '—' }}</td>
                <td><span class="status-tag" :class="`status-${item.status}`">{{ alertStatusLabel(item.status) }}</span></td>
                <td class="action-cell">
                  <button type="button" @click="openView(item)">查看</button>
                  <button v-if="item.manual" type="button" @click="openEdit(item)">编辑</button>
                  <button v-if="item.status !== 'closed'" type="button" @click="closeAlert(item)">关闭</button>
                  <button v-if="item.actionPath" type="button" @click="goAction(item.actionPath!)">去处理</button>
                </td>
              </tr>
            </tbody>
          </table>
          <el-empty v-if="!loading && !(data?.items?.length)" description="暂无符合条件的预警" :image-size="72" />
        </div>

        <div v-if="data && data.total > 0" class="pagination-bar">
          <span>共 {{ data.total }} 条</span>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="data.total"
            :page-sizes="[10, 20, 50]"
            layout="sizes, prev, pager, next, jumper"
            @current-change="loadData"
            @size-change="onPageSizeChange"
          />
        </div>
      </article>

      <aside class="guide-panel">
        <h2>培训预警说明</h2>
        <ul class="guide-list">
          <li v-for="guide in guideItems" :key="guide.title">
            <span class="guide-icon" :class="guide.tone"><el-icon><component :is="guide.icon" /></el-icon></span>
            <div>
              <strong>{{ guide.title }}</strong>
              <p>{{ guide.desc }}</p>
            </div>
          </li>
        </ul>
        <div class="guide-tip">
          <el-icon><InfoFilled /></el-icon>
          <p>请及时跟进培训预警信息，督促学员完成学习与考核，确保培训达标。</p>
        </div>
      </aside>
    </div>

    <el-dialog v-model="detailVisible" :title="dialogMode === 'view' ? '预警详情' : dialogMode === 'edit' ? '编辑预警' : '新增预警'" width="560px">
      <el-form v-if="dialogMode !== 'view'" :model="form" label-width="88px">
        <el-form-item label="预警类型" required>
          <el-select v-model="form.type" class="w-full">
            <el-option v-for="item in ALERT_TYPE_OPTIONS.filter(i => i.value !== 'all')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警级别" required>
          <el-select v-model="form.level" class="w-full">
            <el-option label="高危" value="danger" />
            <el-option label="中危" value="warning" />
            <el-option label="低危" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="预警内容">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-input v-model="form.department" />
        </el-form-item>
        <el-form-item label="责任人">
          <el-input v-model="form.responsiblePerson" />
        </el-form-item>
        <el-form-item label="学员姓名">
          <el-input v-model="form.traineeName" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="form.courseName" />
        </el-form-item>
      </el-form>
      <div v-else class="detail-view">
        <p><span>编号</span>{{ currentItem?.alertNo }}</p>
        <p><span>标题</span>{{ currentItem?.title }}</p>
        <p><span>内容</span>{{ currentItem?.description }}</p>
        <p><span>类型</span>{{ alertTypeLabel(currentItem?.type) }}</p>
        <p><span>级别</span>{{ alertLevelLabel(currentItem!.level) }}</p>
        <p><span>部门</span>{{ currentItem?.department }}</p>
        <p><span>责任人</span>{{ currentItem?.responsiblePerson }}</p>
        <p><span>学员</span>{{ currentItem?.traineeName || '—' }}</p>
        <p><span>课程</span>{{ currentItem?.courseName || '—' }}</p>
        <p><span>状态</span>{{ alertStatusLabel(currentItem?.status) }}</p>
        <p><span>时间</span>{{ currentItem?.time }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button v-if="dialogMode !== 'view'" type="primary" :loading="saving" @click="saveAlert">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  BellFilled, Document, Timer, List, Upload, InfoFilled,
  Reading, Medal, WarningFilled, Notebook,
} from '@element-plus/icons-vue'
import {
  adminApi,
  type AdminAlertCenterPage,
  type AdminAlertStats,
  type AdminTrainingAlertItem,
} from '@/api/admin'
import {
  ALERT_LEVEL_OPTIONS,
  ALERT_LEVEL_TAB_OPTIONS,
  ALERT_TYPE_OPTIONS,
  ALERT_STATUS_OPTIONS,
  alertLevelLabel,
  alertTypeLabel,
  alertStatusLabel,
} from './alertCenterShared'

type LevelTab = typeof ALERT_LEVEL_TAB_OPTIONS[number]['value']

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const data = ref<AdminAlertCenterPage | null>(null)
const stats = ref<AdminAlertStats | null>(null)
const page = ref(1)
const pageSize = ref(10)
const activeTab = ref<LevelTab>('all')
const dateRange = ref<[string, string] | null>(null)
const detailVisible = ref(false)
const dialogMode = ref<'view' | 'create' | 'edit'>('view')
const currentItem = ref<AdminTrainingAlertItem | null>(null)

const filters = reactive({
  keyword: '',
  level: 'all',
  type: 'all',
  department: 'all',
  status: 'all',
})

const form = reactive({
  type: 'exam_fail',
  level: 'warning' as AdminTrainingAlertItem['level'],
  title: '',
  description: '',
  department: '',
  responsiblePerson: '',
  traineeName: '',
  courseName: '',
})

const departmentOptions = computed(() => data.value?.departments || ['all'])

const summaryCards = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    {
      key: 'total',
      label: '预警总数',
      value: s.total,
      subText: `较昨日 ${s.totalDelta >= 0 ? '+' : ''}${s.totalDelta}`,
      subTone: s.totalDelta > 0 ? 'negative' : 'positive',
      tone: 'blue',
      icon: BellFilled,
    },
    {
      key: 'exam',
      label: '考试不合格',
      value: s.examFailCount,
      subText: `占比 ${s.examFailRate}%`,
      subTone: 'negative',
      tone: 'red',
      icon: Document,
    },
    {
      key: 'progress',
      label: '进度滞后',
      value: s.progressLagCount,
      subText: `占比 ${s.progressLagRate}%`,
      subTone: 'warning',
      tone: 'orange',
      icon: Timer,
    },
    {
      key: 'pending',
      label: '待处理',
      value: s.pendingCount,
      subText: `占比 ${s.pendingRate}%`,
      subTone: 'warning',
      tone: 'green',
      icon: List,
    },
  ]
})

const guideItems = [
  { title: '考试不合格', desc: '学员考试成绩未达到合格分数线，需安排补考或辅导。', icon: Document, tone: 'blue' },
  { title: '培训进度滞后', desc: '学员学习进度落后于计划安排，需督促加快学习。', icon: Reading, tone: 'green' },
  { title: '课程未完成', desc: '必修课程或学习资料未按时完成阅读与学习。', icon: Notebook, tone: 'purple' },
  { title: '复训/证书到期', desc: '学员证书即将到期或需进行周期性复训。', icon: Medal, tone: 'orange' },
]

function buildParams() {
  return {
    keyword: filters.keyword || undefined,
    level: activeTab.value !== 'all' ? activeTab.value : (filters.level !== 'all' ? filters.level : undefined),
    type: filters.type !== 'all' ? filters.type : undefined,
    department: filters.department !== 'all' ? filters.department : undefined,
    status: filters.status !== 'all' ? filters.status : undefined,
    dateFrom: dateRange.value?.[0],
    dateTo: dateRange.value?.[1],
    page: page.value,
    pageSize: pageSize.value,
  }
}

function switchTab(tab: LevelTab) {
  activeTab.value = tab
  page.value = 1
  loadData()
}

function search() {
  page.value = 1
  loadData()
}

function resetFilters() {
  filters.keyword = ''
  filters.level = 'all'
  filters.type = 'all'
  filters.department = 'all'
  filters.status = 'all'
  dateRange.value = null
  activeTab.value = 'all'
  search()
}

function onPageSizeChange() {
  page.value = 1
  loadData()
}

function goAction(path: string) {
  const [pathname, searchStr] = path.split('?')
  router.push(searchStr ? { path: pathname, query: Object.fromEntries(new URLSearchParams(searchStr)) } : path)
}

function openView(item: AdminTrainingAlertItem) {
  currentItem.value = item
  dialogMode.value = 'view'
  detailVisible.value = true
}

function openCreate() {
  currentItem.value = null
  Object.assign(form, {
    type: 'exam_fail',
    level: 'warning',
    title: '',
    description: '',
    department: '',
    responsiblePerson: '',
    traineeName: '',
    courseName: '',
  })
  dialogMode.value = 'create'
  detailVisible.value = true
}

function openEdit(item: AdminTrainingAlertItem) {
  currentItem.value = item
  Object.assign(form, {
    type: item.type,
    level: item.level,
    title: item.title,
    description: item.description,
    department: item.department || '',
    responsiblePerson: item.responsiblePerson || '',
    traineeName: item.traineeName || '',
    courseName: item.courseName || '',
  })
  dialogMode.value = 'edit'
  detailVisible.value = true
}

async function saveAlert() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写预警标题')
    return
  }
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await adminApi.createAlert({ ...form })
      ElMessage.success('预警已创建')
    } else if (currentItem.value) {
      await adminApi.updateAlert(currentItem.value.id, { ...form })
      ElMessage.success('预警已更新')
    }
    detailVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function closeAlert(item: AdminTrainingAlertItem) {
  await ElMessageBox.confirm('确认将该预警标记为已闭环？', '关闭预警', { type: 'warning' })
  await adminApi.updateAlertStatus(item.id, { status: 'closed' })
  ElMessage.success('预警已关闭')
  loadData()
}

async function exportData() {
  const res = await adminApi.exportAlerts({
    keyword: filters.keyword || undefined,
    level: filters.level !== 'all' ? filters.level : undefined,
    type: filters.type !== 'all' ? filters.type : undefined,
    department: filters.department !== 'all' ? filters.department : undefined,
    status: filters.status !== 'all' ? filters.status : undefined,
    dateFrom: dateRange.value?.[0],
    dateTo: dateRange.value?.[1],
  })
  const rows = res.data || []
  const header = ['预警编号', '预警标题', '预警类型', '预警级别', '所属部门', '责任人', '发生时间', '处理状态']
  const csv = [
    header.join(','),
    ...rows.map(item => [
      item.alertNo,
      `"${item.title}"`,
      alertTypeLabel(item.type),
      alertLevelLabel(item.level),
      item.department,
      item.responsiblePerson,
      item.time,
      alertStatusLabel(item.status),
    ].join(',')),
  ].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `安全预警_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.getAlertCenter(buildParams())
    data.value = res.data
    stats.value = res.data?.stats || null
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.alert-center-page {
  min-height: 100%;
  padding: 16px 24px 24px;
  background: #f5f7fc;
  color: #243044;
  box-sizing: border-box;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 13px;
  color: #8b95a4;
}

.page-breadcrumb button {
  border: none;
  background: none;
  color: #8b95a4;
  cursor: pointer;
  padding: 0;
}

.page-breadcrumb button:hover,
.page-breadcrumb strong {
  color: #364153;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
}

.page-header p {
  margin: 0 0 16px;
  color: #7c8695;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e7ebf1;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(34, 54, 86, 0.04);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 22px;
}

.tone-blue { color: #3478ef; background: #edf3ff; }
.tone-red { color: #ef4c55; background: #fff0f1; }
.tone-orange { color: #f38b3b; background: #fff5ea; }
.tone-green { color: #22a06b; background: #eaf8f1; }

.stat-label {
  display: block;
  font-size: 12px;
  color: #8b95a4;
  margin-bottom: 4px;
}

.stat-content strong {
  display: block;
  font-size: 28px;
  line-height: 1.1;
}

.stat-content em {
  display: block;
  margin-top: 4px;
  font-style: normal;
  font-size: 12px;
}

.stat-content em.negative { color: #ef4c55; }
.stat-content em.warning { color: #f38b3b; }
.stat-content em.positive { color: #22a06b; }

.filter-panel {
  background: #fff;
  border: 1px solid #e7ebf1;
  border-radius: 10px;
  padding: 16px 18px;
  margin-bottom: 14px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 14px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-field.span-2 {
  grid-column: span 2;
}

.filter-label {
  font-size: 12px;
  color: #8b95a4;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: start;
}

.panel {
  background: #fff;
  border: 1px solid #e7ebf1;
  border-radius: 10px;
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #edf0f4;
}

.level-tabs {
  display: flex;
  gap: 18px;
}

.level-tabs button {
  border: none;
  background: none;
  color: #667085;
  font-size: 14px;
  cursor: pointer;
  padding: 0 0 6px;
  border-bottom: 2px solid transparent;
}

.level-tabs button.active {
  color: #3478ef;
  border-bottom-color: #3478ef;
  font-weight: 600;
}

.table-wrap {
  overflow-x: auto;
}

.alert-table {
  width: 100%;
  border-collapse: collapse;
}

.alert-table th,
.alert-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #edf0f4;
  text-align: left;
  font-size: 13px;
  vertical-align: top;
}

.alert-table th {
  background: #fafbfc;
  color: #667085;
  font-weight: 600;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: #667085;
  white-space: nowrap;
}

.title-cell strong {
  display: block;
  color: #364153;
  line-height: 1.4;
}

.title-cell p {
  margin: 4px 0 0;
  color: #8b95a4;
  font-size: 12px;
  line-height: 1.4;
}

.level-tag,
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
}

.level-danger { color: #ef4c55; background: #fff0f1; }
.level-warning { color: #f38b3b; background: #fff5ea; }
.level-info { color: #3d80ed; background: #eef4ff; }

.status-pending { color: #f38b3b; }
.status-processing { color: #3478ef; }
.status-closed { color: #22a06b; }

.action-cell {
  white-space: nowrap;
}

.action-cell button {
  border: none;
  background: none;
  color: #3478ef;
  cursor: pointer;
  font-size: 12px;
  padding: 0 4px;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  color: #667085;
  font-size: 13px;
  border-top: 1px solid #edf0f4;
}

.guide-panel {
  padding: 16px;
}

.guide-panel h2 {
  margin: 0 0 14px;
  font-size: 15px;
}

.guide-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.guide-list li {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #edf0f4;
}

.guide-list li:last-child {
  border-bottom: none;
}

.guide-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.guide-icon.blue { color: #3478ef; background: #edf3ff; }
.guide-icon.green { color: #22a06b; background: #eaf8f1; }
.guide-icon.purple { color: #8255ea; background: #f2edff; }
.guide-icon.orange { color: #f38b3b; background: #fff5ea; }

.guide-list strong {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
}

.guide-list p {
  margin: 0;
  font-size: 12px;
  color: #778293;
  line-height: 1.5;
}

.guide-tip {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  padding: 12px;
  border-radius: 8px;
  background: #edf5ff;
  color: #4b6b9b;
  font-size: 12px;
  line-height: 1.5;
}

.guide-tip p {
  margin: 0;
}

.detail-view p {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 13px;
}

.detail-view span {
  color: #8b95a4;
}

.w-full {
  width: 100%;
}

@media (max-width: 1280px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid,
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stats-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-field.span-2 {
    grid-column: span 1;
  }
}
</style>
