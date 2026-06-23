<template>
  <div class="exam-list-page" v-loading="loading">
    <div class="page-toolbar">
      <div class="filter-row">
        <div class="filter-field">
          <span class="filter-label">考试名称</span>
          <el-input v-model="filters.keyword" placeholder="请输入考试名称" clearable class="filter-input" @keyup.enter="search" />
        </div>
        <div class="filter-field">
          <span class="filter-label">考试类型</span>
          <el-select v-model="filters.examType" class="filter-item">
            <el-option v-for="item in EXAM_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">适用部门</span>
          <el-select v-model="filters.department" class="filter-item">
            <el-option v-for="dept in DEPARTMENT_FILTER_OPTIONS" :key="dept" :label="dept" :value="dept === '全部部门' ? 'all' : dept" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">状态</span>
          <el-select v-model="filters.status" class="filter-item">
            <el-option v-for="item in EXAM_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </div>

      <div class="filter-row filter-row-bottom">
        <div class="filter-field filter-field-date">
          <span class="filter-label">创建时间</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="filter-date"
          />
        </div>
        <div class="filter-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </div>
    </div>

    <div class="table-card">
      <table class="exam-table">
        <thead>
          <tr>
            <th>考试名称</th>
            <th>考试类型</th>
            <th>适用部门</th>
            <th>题目数量</th>
            <th>考试时长</th>
            <th>总分</th>
            <th>及格线</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="exam in exams" :key="exam.id">
            <td class="title-cell">
              <strong>{{ exam.title }}</strong>
              <span v-if="exam.courseTitle" class="sub-text">{{ exam.courseTitle }}</span>
            </td>
            <td>
              <el-tag size="small" :type="examTypeTagType(exam.examType) || undefined" :class="exam.examType === 'mock' ? 'tag-mock' : ''">
                {{ examTypeLabel(exam.examType) }}
              </el-tag>
            </td>
            <td>{{ exam.department }}</td>
            <td>{{ exam.questionCount }}</td>
            <td>{{ formatExamDuration(exam.timeLimit) }}</td>
            <td>{{ exam.totalScore }}</td>
            <td>{{ exam.passScore }}</td>
            <td><span class="status-pill" :class="examStatusClass(exam.status)">{{ examStatusLabel(exam.status) }}</span></td>
            <td>{{ exam.createdAt || '-' }}</td>
            <td class="action-cell">
              <div class="action-links">
                <button type="button" class="link-btn" title="预览学员端考试页面" @click="previewExam(exam)">预览</button>
                <button type="button" class="link-btn" title="修改考试状态与类型" @click="openEdit(exam)">编辑</button>
                <button type="button" class="link-btn" title="查看考试参与与通过情况" @click="viewStats(exam)">查看成绩</button>
                <el-dropdown trigger="click" @command="(cmd: string) => handleMore(cmd, exam)">
                  <button type="button" class="link-btn">更多</button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="exam.sourceType === 'chapter' && exam.status !== 'published'" command="publish">发布考试</el-dropdown-item>
                      <el-dropdown-item v-if="exam.sourceType === 'chapter' && exam.status === 'published'" command="end">结束考试</el-dropdown-item>
                      <el-dropdown-item v-if="exam.courseId" command="monitoring">学习监控</el-dropdown-item>
                      <el-dropdown-item v-if="exam.sourceType === 'chapter'" command="delete" divided>删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <el-empty v-if="!loading && !exams.length" description="暂无考试数据" />
    </div>

    <div class="pagination-bar">
      <span>共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next, jumper"
        @current-change="loadExams"
        @size-change="onPageSizeChange"
      />
    </div>

    <el-dialog v-model="statsVisible" :title="`${stats?.title || ''} · 成绩统计`" width="480px">
      <div v-if="stats" class="stats-grid">
        <div><span>参考人次</span><strong>{{ stats.attemptCount }}</strong></div>
        <div><span>通过人数</span><strong>{{ stats.passedCount }}</strong></div>
        <div><span>通过率</span><strong>{{ stats.passRate }}%</strong></div>
      </div>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑考试" width="480px">
      <el-form v-if="editing" label-width="88px">
        <el-form-item label="考试名称"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="考试类型">
          <el-select v-model="editForm.examType">
            <el-option label="正式考试" value="formal" />
            <el-option label="模拟考试" value="mock" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option v-for="item in EXAM_STATUS_OPTIONS.filter(i => i.value !== 'all')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试时长"><el-input-number v-model="editForm.timeLimit" :min="0" :max="300" /> 分钟</el-form-item>
        <el-form-item label="总分"><el-input-number v-model="editForm.totalScore" :min="1" :max="200" /></el-form-item>
        <el-form-item label="及格线"><el-input-number v-model="editForm.passScore" :min="0" :max="200" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type AdminExamListItem, type AdminExamStats } from '@/api/admin'
import {
  DEPARTMENT_FILTER_OPTIONS, EXAM_STATUS_OPTIONS, EXAM_TYPE_OPTIONS,
  examStatusClass, examStatusLabel, examTypeLabel, examTypeTagType, formatExamDuration,
} from './examAdminShared'

const router = useRouter()
const loading = ref(false)
const exams = ref<AdminExamListItem[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const statsVisible = ref(false)
const stats = ref<AdminExamStats | null>(null)
const editVisible = ref(false)
const editing = ref<AdminExamListItem | null>(null)
const editForm = reactive({
  title: '',
  examType: 'mock',
  status: 'published',
  timeLimit: 60,
  totalScore: 100,
  passScore: 60,
})

const filters = reactive({
  keyword: '',
  examType: 'all',
  department: 'all',
  status: 'all',
  page: 1,
  pageSize: 10,
})

async function loadExams() {
  loading.value = true
  try {
    const res = await adminApi.getExams({
      keyword: filters.keyword || undefined,
      examType: filters.examType === 'all' ? undefined : filters.examType,
      department: filters.department === 'all' ? undefined : filters.department,
      status: filters.status === 'all' ? undefined : filters.status,
      createdFrom: dateRange.value?.[0],
      createdTo: dateRange.value?.[1],
      page: filters.page,
      pageSize: filters.pageSize,
    })
    exams.value = res.data.items || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadExams()
}

function resetFilters() {
  filters.keyword = ''
  filters.examType = 'all'
  filters.department = 'all'
  filters.status = 'all'
  filters.page = 1
  dateRange.value = null
  loadExams()
}

function onPageSizeChange() {
  filters.page = 1
  loadExams()
}

function previewExam(exam: AdminExamListItem) {
  if (!exam.courseId) return
  if (exam.sourceType === 'comprehensive') {
    window.open(`/user/courses/${exam.courseId}/comprehensive-exam`, '_blank')
  } else if (exam.chapterId) {
    window.open(`/user/courses/${exam.courseId}/chapters/${exam.chapterId}/quiz`, '_blank')
  }
}

function openEdit(exam: AdminExamListItem) {
  if (exam.sourceType === 'comprehensive') {
    ElMessage.info('综合考试由课程章节题库自动生成，暂不支持编辑')
    return
  }
  editing.value = exam
  editForm.title = exam.title
  editForm.examType = exam.examType
  editForm.status = exam.status
  editForm.timeLimit = exam.timeLimit ?? 60
  editForm.totalScore = exam.totalScore
  editForm.passScore = exam.passScore
  editVisible.value = true
}

async function saveEdit() {
  if (!editing.value) return
  await adminApi.updateExam(editing.value.id, { ...editForm })
  ElMessage.success('保存成功')
  editVisible.value = false
  loadExams()
}

async function viewStats(exam: AdminExamListItem) {
  const res = await adminApi.getExamStats(exam.id)
  stats.value = res.data
  statsVisible.value = true
}

async function handleMore(command: string, exam: AdminExamListItem) {
  if (command === 'monitoring' && exam.courseId) {
    router.push({ path: '/admin/learning/monitoring', query: { courseId: exam.courseId } })
    return
  }
  if (command === 'publish') {
    await adminApi.updateExam(exam.id, { status: 'published' })
    ElMessage.success('考试已发布')
    loadExams()
    return
  }
  if (command === 'end') {
    await adminApi.updateExam(exam.id, { status: 'ended' })
    ElMessage.success('考试已结束')
    loadExams()
    return
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(`确定删除考试「${exam.title}」吗？`, '提示', { type: 'warning' })
    await adminApi.deleteExam(exam.id)
    ElMessage.success('删除成功')
    loadExams()
  }
}

onMounted(loadExams)
</script>

<style scoped>
.exam-list-page {
  padding: 20px 24px 24px;
  min-height: 100%;
  box-sizing: border-box;
}

.page-toolbar {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 12px;
}

.filter-row-bottom {
  margin-bottom: 0;
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
}

.filter-input,
.filter-item {
  width: 180px;
}

.filter-field-date .filter-date {
  width: 280px;
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

.exam-table {
  width: 100%;
  border-collapse: collapse;
}

.exam-table th,
.exam-table td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
  vertical-align: middle;
}

.exam-table th {
  background: #fafbfc;
  color: #667085;
  font-weight: 600;
}

.title-cell strong {
  display: block;
  color: #1f2937;
}

.sub-text {
  display: block;
  margin-top: 4px;
  color: #9ca3af;
  font-size: 12px;
}

.tag-mock {
  --el-tag-bg-color: #f5f3ff;
  --el-tag-border-color: #ddd6fe;
  --el-tag-text-color: #7c3aed;
}

.status-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.exam-status-published { color: #16a34a; background: #ecfdf3; }
.exam-status-pending { color: #ea580c; background: #fff7ed; }
.exam-status-draft { color: #9ca3af; background: #f3f4f6; }
.exam-status-ended { color: #374151; background: #e5e7eb; }

.action-cell {
  white-space: nowrap;
}

.action-links {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.action-links :deep(.el-dropdown) {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}

.link-btn {
  border: 0;
  background: none;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
  line-height: 20px;
  padding: 0;
  white-space: nowrap;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  color: #667085;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stats-grid span {
  display: block;
  color: #9ca3af;
  font-size: 12px;
  margin-bottom: 6px;
}

.stats-grid strong {
  font-size: 22px;
  color: #1f2937;
}
</style>
