<template>
  <div class="practical-page" v-loading="loading">
    <div class="page-toolbar">
      <div class="filter-bar">
        <el-select v-model="filters.kind" placeholder="场景类型" clearable class="filter-item">
          <el-option v-for="item in SCENARIO_KIND_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索场景名称" clearable class="filter-search" @keyup.enter="search" />
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">搜索</el-button>
      </div>
      <div class="action-bar">
        <span class="total-text">共 {{ total }} 个场景</span>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 新建场景
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>场景名称</th>
            <th>类型</th>
            <th>时长</th>
            <th>难度</th>
            <th>关联案例</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>
              <strong>{{ item.name }}</strong>
              <p class="desc">{{ item.description || '暂无描述' }}</p>
            </td>
            <td><el-tag size="small" :type="scenarioKindTag(item.kind)">{{ scenarioKindLabel(item.kind) }}</el-tag></td>
            <td>{{ item.duration ? `${item.duration} 分钟` : '—' }}</td>
            <td>{{ item.difficulty || '—' }}</td>
            <td>{{ item.caseId ? '已关联' : '—' }}</td>
            <td class="action-cell">
              <button type="button" class="link-btn" @click="openEdit(item.id)">编辑</button>
              <button type="button" class="link-btn" @click="preview(item)">预览</button>
              <button type="button" class="link-btn danger" @click="remove(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <el-empty v-if="!loading && !items.length" description="暂无场景" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑场景' : '新建场景'" width="680px">
      <el-form label-width="96px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="场景名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="form.duration" :min="5" :max="120" />
        </el-form-item>
        <el-form-item label="难度(1-5)">
          <el-input-number v-model="form.difficulty" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="初始条件 JSON">
          <el-input
            v-model="initialConditionsText"
            type="textarea"
            :rows="4"
            placeholder='{"batteryCount":4,"trainingKind":"emergency_case","caseId":"..."}'
          />
        </el-form-item>
        <el-form-item label="决策点 JSON">
          <el-input v-model="decisionPointsText" type="textarea" :rows="4" placeholder="应急训练场景需配置决策点" />
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
import { adminApi, type AdminPracticalScenarioDetail, type AdminPracticalScenarioItem } from '@/api/admin'
import { SCENARIO_KIND_OPTIONS, scenarioKindLabel, scenarioKindTag } from './practicalAdminShared'

const loading = ref(false)
const saving = ref(false)
const items = ref<AdminPracticalScenarioItem[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref('')
const initialConditionsText = ref('{}')
const decisionPointsText = ref('[]')
const form = reactive<Partial<AdminPracticalScenarioDetail>>({
  name: '',
  description: '',
  duration: 30,
  difficulty: 2,
})

const filters = reactive({
  keyword: '',
  kind: 'all',
  page: 1,
  pageSize: 10,
})

async function loadItems() {
  loading.value = true
  try {
    const res = await adminApi.getPracticalScenarios({
      keyword: filters.keyword || undefined,
      kind: filters.kind === 'all' ? undefined : filters.kind,
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
  filters.kind = 'all'
  filters.page = 1
  loadItems()
}

function onPageSizeChange() {
  filters.page = 1
  loadItems()
}

function resetForm() {
  Object.assign(form, { name: '', description: '', duration: 30, difficulty: 2 })
  initialConditionsText.value = '{"batteryCount":1,"initialTemperature":25}'
  decisionPointsText.value = '[]'
}

function openCreate() {
  editingId.value = ''
  resetForm()
  dialogVisible.value = true
}

async function openEdit(id: string) {
  editingId.value = id
  const res = await adminApi.getPracticalScenario(id)
  Object.assign(form, res.data || {})
  initialConditionsText.value = JSON.stringify(res.data?.initialConditions ?? {}, null, 2)
  decisionPointsText.value = JSON.stringify(res.data?.decisionPoints ?? [], null, 2)
  dialogVisible.value = true
}

function preview(item: AdminPracticalScenarioItem) {
  if (item.kind === 'simulation') {
    window.open(`/user/simulation/${item.id}`, '_blank')
  } else if (item.kind === 'emergency') {
    window.open(`/user/simulation/timeline/beijing416`, '_blank')
  } else {
    window.open(`/user/training/${item.id}`, '_blank')
  }
}

async function save() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写场景名称')
    return
  }
  let initialConditions: unknown
  let decisionPoints: unknown
  try {
    initialConditions = JSON.parse(initialConditionsText.value || '{}')
    decisionPoints = JSON.parse(decisionPointsText.value || '[]')
  } catch {
    ElMessage.error('JSON 格式不正确')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, initialConditions, decisionPoints }
    if (editingId.value) {
      await adminApi.updatePracticalScenario(editingId.value, payload)
      ElMessage.success('场景已更新')
    } else {
      await adminApi.createPracticalScenario(payload)
      ElMessage.success('场景已创建')
    }
    dialogVisible.value = false
    await loadItems()
  } finally {
    saving.value = false
  }
}

async function remove(item: AdminPracticalScenarioItem) {
  await ElMessageBox.confirm(`确定删除场景「${item.name}」吗？`, '提示', { type: 'warning' })
  await adminApi.deletePracticalScenario(item.id)
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
.data-table .desc { margin: 4px 0 0; color: #9ca3af; font-size: 12px; }
.action-cell { white-space: nowrap; }
.link-btn { border: 0; background: none; color: #2563eb; font-size: 13px; cursor: pointer; padding: 0 4px; }
.link-btn.danger { color: #ef4444; }
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; color: #667085; font-size: 13px; }
</style>
