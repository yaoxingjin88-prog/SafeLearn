<template>
  <div class="sl-page exam-center-page" v-loading="loading">
    <section class="page-hero">
      <div class="hero-main">
        <div class="hero-icon">
          <el-icon :size="28"><Tickets /></el-icon>
        </div>
        <div>
          <h1 class="hero-title">考试中心</h1>
          <p class="hero-desc">参加储能安全相关考试，检验学习成果，提升安全能力。</p>
        </div>
      </div>
      <div v-if="streakDays > 0" class="streak-badge">
        <el-icon><Sunny /></el-icon>
        连续学习 {{ streakDays }} 天
      </div>
    </section>

    <section class="stats-row">
      <div v-for="item in statCards" :key="item.key" class="stat-card">
        <div class="stat-icon" :style="{ background: item.bg, color: item.color }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-sub">{{ item.sub }}</div>
        </div>
      </div>
    </section>

    <section class="list-card">
      <div class="list-toolbar">
        <el-tabs v-model="activeTab" class="exam-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="全部考试" name="all" />
          <el-tab-pane label="正式考试" name="formal" />
          <el-tab-pane label="综合考试" name="comprehensive" />
          <el-tab-pane label="章节考试" name="chapter" />
        </el-tabs>
        <button type="button" class="guide-link" @click="showGuide = true">
          <el-icon><InfoFilled /></el-icon>
          考试说明
        </button>
      </div>

      <el-table :data="exams" class="exam-table" empty-text="暂无可参加的考试">
        <el-table-column label="考试名称" min-width="320">
          <template #default="{ row }">
            <div class="exam-name-cell">
              <div class="exam-icon" :class="row.sourceType">
                <el-icon><component :is="sourceIcon(row.sourceType)" /></el-icon>
              </div>
              <div class="exam-name-body">
                <div class="exam-name-row">
                  <span class="exam-name">{{ row.title }}</span>
                  <el-tag size="small" :type="sourceTagType(row.sourceType)" effect="light">
                    {{ sourceLabel(row.sourceType) }}
                  </el-tag>
                </div>
                <p class="exam-desc">{{ row.description || defaultDescription(row) }}</p>
                <span class="exam-validity">{{ row.validityText || '长期有效' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="考试类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="examTypeTag(row)" effect="plain">
              {{ examTypeLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="时长" width="110">
          <template #default="{ row }">
            <span class="cell-with-icon">
              <el-icon><Timer /></el-icon>
              {{ row.timeLimit || '—' }} 分钟
            </span>
          </template>
        </el-table-column>

        <el-table-column label="及格分" width="90">
          <template #default="{ row }">{{ row.passScore ?? '—' }} 分</template>
        </el-table-column>

        <el-table-column label="已用次数" width="120">
          <template #default="{ row }">
            使用 {{ row.usedAttempts ?? row.attemptCount ?? 0 }} / {{ row.maxAttempts ?? 5 }} 次
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-text" :class="row.examStatus || (row.examPassed ? 'passed' : 'available')">
              {{ statusLabel(row) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.examPassed"
              type="success"
              link
              disabled
            >
              已通过
            </el-button>
            <el-button
              v-else-if="row.examStatus === 'exhausted'"
              type="info"
              link
              disabled
            >
              次数已满
            </el-button>
            <el-button v-else type="primary" link @click="goExam(row)">
              进入考试
              <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > 0" class="pagination-wrap">
        <span class="total-text">共 {{ total }} 项</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          @current-change="loadExams"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="showGuide" title="考试说明" width="520px" align-center>
      <ul class="guide-list">
        <li>请在网络稳定的环境下参加考试，考试过程中请勿刷新或关闭页面。</li>
        <li>每门考试有次数限制，请合理安排作答时间，超时将自动提交。</li>
        <li>正式考试通过后可在结果页查看解析，章节与综合考试请回到对应课程复习。</li>
        <li>标记「待检查」的题目可在答题卡中快速回看，提交前请确认全部作答。</li>
      </ul>
      <template #footer>
        <el-button type="primary" @click="showGuide = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Tickets, Sunny, Calendar, Clock, CircleCheck, Trophy,
  InfoFilled, Timer, ArrowRight, Document, Reading, Collection,
} from '@element-plus/icons-vue'
import { examApi, type UserExamItem, type ExamCenterStats } from '@/api/exam'
import { dashboardApi } from '@/api/dashboard'
import { useAppBase } from '@/composables/useAppBase'

const router = useRouter()
const { p } = useAppBase()

const loading = ref(true)
const exams = ref<UserExamItem[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('all')
const showGuide = ref(false)
const streakDays = ref(0)

const stats = ref<ExamCenterStats>({
  pendingExams: 0,
  inProgressExams: 0,
  completedExams: 0,
  averageScore: 0,
  percentileRank: 0,
})

const statCards = computed(() => [
  {
    key: 'pending',
    label: '待参加考试',
    value: stats.value.pendingExams,
    sub: '随时可参加的考试',
    icon: Calendar,
    bg: '#eff6ff',
    color: '#2563eb',
  },
  {
    key: 'progress',
    label: '进行中',
    value: stats.value.inProgressExams,
    sub: '考试未完成',
    icon: Clock,
    bg: '#fff7ed',
    color: '#ea580c',
  },
  {
    key: 'completed',
    label: '已完成',
    value: stats.value.completedExams,
    sub: '已通过考试',
    icon: CircleCheck,
    bg: '#ecfdf5',
    color: '#059669',
  },
  {
    key: 'average',
    label: '平均成绩',
    value: stats.value.averageScore > 0 ? stats.value.averageScore : '—',
    sub: stats.value.percentileRank > 0 ? `超过 ${stats.value.percentileRank}% 的学员` : '完成考试后显示',
    icon: Trophy,
    bg: '#f5f3ff',
    color: '#7c3aed',
  },
])

function sourceLabel(type: string) {
  const map: Record<string, string> = {
    paper: '独立试卷',
    chapter: '章节考试',
    comprehensive: '综合考试',
  }
  return map[type] || type
}

function sourceTagType(type: string) {
  if (type === 'comprehensive') return 'warning'
  if (type === 'paper') return 'success'
  return 'primary'
}

function sourceIcon(type: string) {
  if (type === 'comprehensive') return Collection
  if (type === 'chapter') return Reading
  return Document
}

function examTypeLabel(row: UserExamItem) {
  if (row.sourceType === 'chapter') return '章节考试'
  if (row.sourceType === 'comprehensive') return '正式考试'
  return row.examType === 'mock' ? '模拟考试' : '正式考试'
}

function examTypeTag(row: UserExamItem) {
  if (row.sourceType === 'chapter') return 'success'
  if (row.examType === 'mock') return 'info'
  return 'primary'
}

function statusLabel(row: UserExamItem) {
  if (row.examPassed || row.examStatus === 'passed') return '已通过'
  if (row.examStatus === 'in_progress') return '进行中'
  if (row.examStatus === 'exhausted') return '次数已满'
  return '可参加'
}

function defaultDescription(row: UserExamItem) {
  if (row.courseTitle && row.chapterTitle) {
    return `${row.courseTitle} · ${row.chapterTitle}`
  }
  if (row.courseTitle) return `关联课程：${row.courseTitle}`
  return '检验储能安全知识掌握情况。'
}

function goExam(row: UserExamItem) {
  if (!row.link) {
    ElMessage.info('暂不支持此类型考试')
    return
  }
  router.push(row.link.startsWith('/user') ? row.link : p(row.link.replace(/^\//, '')))
}

function handleTabChange() {
  page.value = 1
  loadExams()
}

function handleSizeChange() {
  page.value = 1
  loadExams()
}

async function loadExams() {
  loading.value = true
  try {
    const res = await examApi.getList({
      page: page.value,
      pageSize: pageSize.value,
      tab: activeTab.value === 'all' ? undefined : activeTab.value,
    })
    exams.value = res.data.items || []
    total.value = res.data.total || 0
    if (res.data.stats) stats.value = res.data.stats
  } finally {
    loading.value = false
  }
}

async function loadStreak() {
  try {
    const res = await dashboardApi.getOverview()
    streakDays.value = res.data.streakDays || 0
  } catch {
    streakDays.value = 0
  }
}

onMounted(async () => {
  await Promise.all([loadExams(), loadStreak()])
})
</script>

<style scoped>
.exam-center-page {
  background: #f5f7fa;
  min-height: 100%;
}

.page-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.25);
}

.hero-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.hero-desc {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

.streak-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #ea580c;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.1;
}

.stat-label {
  margin-top: 2px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.stat-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #9ca3af;
}

.list-card {
  background: #fff;
  border-radius: 16px;
  padding: 8px 20px 20px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 4px;
}

.exam-tabs {
  flex: 1;
}

.exam-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.exam-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.guide-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
  padding: 8px 0;
  white-space: nowrap;
}

.guide-link:hover {
  color: #2563eb;
}

.exam-table {
  width: 100%;
}

.exam-name-cell {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 6px 0;
}

.exam-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.exam-icon.paper {
  background: #ecfdf5;
  color: #059669;
}

.exam-icon.comprehensive {
  background: #fff7ed;
  color: #ea580c;
}

.exam-icon.chapter {
  background: #eff6ff;
  color: #2563eb;
}

.exam-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.exam-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.exam-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.45;
}

.exam-validity {
  display: inline-block;
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.cell-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #4b5563;
}

.status-text {
  font-size: 13px;
  font-weight: 600;
}

.status-text.available {
  color: #059669;
}

.status-text.in_progress {
  color: #ea580c;
}

.status-text.passed {
  color: #059669;
}

.status-text.exhausted {
  color: #9ca3af;
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.total-text {
  font-size: 13px;
  color: #6b7280;
}

.guide-list {
  margin: 0;
  padding-left: 18px;
  color: #4b5563;
  line-height: 1.7;
}

.guide-list li + li {
  margin-top: 8px;
}

.ml-1 {
  margin-left: 4px;
}

@media (max-width: 1100px) {
  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-hero {
    flex-direction: column;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .list-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-wrap {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
