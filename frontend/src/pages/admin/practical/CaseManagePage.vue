<template>
  <div class="practical-page" v-loading="loading">
    <div class="page-toolbar">
      <div class="filter-bar">
        <el-select v-model="filters.type" placeholder="案例类型" clearable class="filter-item">
          <el-option v-for="item in CASE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.severity" placeholder="事故等级" clearable class="filter-item">
          <el-option v-for="item in SEVERITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索标题、地点" clearable class="filter-search" @keyup.enter="search" />
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">搜索</el-button>
      </div>
      <div class="action-bar">
        <span class="total-text">共 {{ total }} 个案例</span>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 新建案例
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>案例标题</th>
            <th>地点</th>
            <th>日期</th>
            <th>类型</th>
            <th>等级</th>
            <th>建议时长</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td><strong>{{ item.title }}</strong></td>
            <td>{{ item.location || '—' }}</td>
            <td>{{ item.date || '—' }}</td>
            <td>{{ item.type }}</td>
            <td><el-tag size="small" type="danger">{{ severityLabel(item.severity) }}</el-tag></td>
            <td>{{ item.studyMinutes ? `${item.studyMinutes} 分钟` : '—' }}</td>
            <td class="action-cell">
              <button type="button" class="link-btn" @click="openEdit(item.id)">编辑</button>
              <button type="button" class="link-btn danger" @click="remove(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <el-empty v-if="!loading && !items.length" description="暂无案例" />
    </div>

    <div class="pagination-bar">
      <span>共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next"
        @current-change="loadItems"
        @size-change="onPageSizeChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑案例' : '新建案例'" width="640px">
      <el-form label-width="88px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="事故案例标题" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="事故发生地点" />
        </el-form-item>
        <el-form-item label="日期">
          <el-input v-model="form.date" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" class="w-full">
            <el-option v-for="item in CASE_TYPE_OPTIONS.slice(1)" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.severity" class="w-full">
            <el-option v-for="item in SEVERITY_OPTIONS.slice(1)" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="建议时长">
          <el-input-number v-model="form.studyMinutes" :min="5" :max="180" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="原因分析">
          <el-input v-model="form.causeAnalysis" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="教训总结">
          <el-input v-model="form.lessonsLearned" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type AdminPracticalCaseDetail, type AdminPracticalCaseItem } from '@/api/admin'
import { CASE_TYPE_OPTIONS, SEVERITY_OPTIONS, severityLabel } from './practicalAdminShared'

const loading = ref(false)
const saving = ref(false)
const items = ref<AdminPracticalCaseItem[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive<Partial<AdminPracticalCaseDetail>>({
  title: '',
  location: '',
  date: '',
  type: 'thermal_runaway',
  severity: 'major',
  studyMinutes: 30,
  description: '',
  causeAnalysis: '',
  lessonsLearned: '',
})

const filters = reactive({
  keyword: '',
  type: 'all',
  severity: 'all',
  page: 1,
  pageSize: 10,
})

async function loadItems() {
  loading.value = true
  try {
    const res = await adminApi.getPracticalCases({
      keyword: filters.keyword || undefined,
      type: filters.type === 'all' ? undefined : filters.type,
      severity: filters.severity === 'all' ? undefined : filters.severity,
      page: filters.page,
      pageSize: filters.pageSize,
    })
    items.value = res.data?.items || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadItems()
}

function resetFilters() {
  filters.keyword = ''
  filters.type = 'all'
  filters.severity = 'all'
  filters.page = 1
  loadItems()
}

function onPageSizeChange() {
  filters.page = 1
  loadItems()
}

function resetForm() {
  Object.assign(form, {
    title: '',
    location: '',
    date: '',
    type: 'thermal_runaway',
    severity: 'major',
    studyMinutes: 30,
    description: '',
    causeAnalysis: '',
    lessonsLearned: '',
  })
}

function openCreate() {
  editingId.value = ''
  resetForm()
  dialogVisible.value = true
}

async function openEdit(id: string) {
  editingId.value = id
  const res = await adminApi.getPracticalCase(id)
  Object.assign(form, res.data || {})
  dialogVisible.value = true
}

async function save() {
  if (!form.title?.trim()) {
    ElMessage.warning('请填写案例标题')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await adminApi.updatePracticalCase(editingId.value, form)
      ElMessage.success('案例已更新')
    } else {
      await adminApi.createPracticalCase(form)
      ElMessage.success('案例已创建')
    }
    dialogVisible.value = false
    await loadItems()
  } finally {
    saving.value = false
  }
}

async function remove(item: AdminPracticalCaseItem) {
  await ElMessageBox.confirm(`确定删除案例「${item.title}」吗？`, '提示', { type: 'warning' })
  await adminApi.deletePracticalCase(item.id)
  ElMessage.success('已删除')
  await loadItems()
}

onMounted(loadItems)
</script>

<style scoped>
.practical-page { padding: 20px 24px 24px; min-height: 100%; box-sizing: border-box; }
.page-toolbar { margin-bottom: 16px; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.filter-item { width: 140px; }
.filter-search { width: 220px; }
.action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.total-text { color: #667085; font-size: 13px; }
.table-card { background: #fff; border: 1px solid #e8edf3; border-radius: 8px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { padding: 12px 14px; background: #fafbfc; color: #8b95a4; text-align: left; font-weight: 500; }
.data-table td { padding: 14px; border-top: 1px solid #edf0f4; color: #4b5563; vertical-align: middle; }
.action-cell { white-space: nowrap; }
.link-btn { border: 0; background: none; color: #2563eb; font-size: 13px; cursor: pointer; padding: 0 4px; }
.link-btn.danger { color: #ef4444; }
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; color: #667085; font-size: 13px; }
.w-full { width: 100%; }
</style>
