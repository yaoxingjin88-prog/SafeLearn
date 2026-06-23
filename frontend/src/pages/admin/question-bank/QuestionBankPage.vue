<template>
  <div class="question-bank-page" v-loading="loading">
    <div class="page-layout">
      <aside class="category-panel">
        <div class="panel-title">题库分类</div>
        <el-input
          v-model="categoryKeyword"
          placeholder="搜索分类名称"
          clearable
          class="category-search"
          :prefix-icon="Search"
          @input="loadCategories"
        />

        <button
          type="button"
          class="all-category-item"
          :class="{ active: selectedCategoryId === 'all' }"
          @click="selectCategory('all')"
        >
          <el-icon class="all-icon"><FolderOpened /></el-icon>
          <span class="tree-label">全部题库</span>
          <span class="tree-count">({{ allCategoryCount }})</span>
        </button>

        <el-tree
          :key="categoryTreeKey"
          class="category-tree"
          :data="treeData"
          :props="treeProps"
          node-key="id"
          highlight-current
          show-line
          :expand-on-click-node="false"
          :current-node-key="treeCurrentKey"
          :default-expanded-keys="defaultExpandedKeys"
          @node-click="onTreeNodeClick"
        >
          <template #default="{ node, data }">
            <div
              class="tree-node"
              :class="{
                'is-parent': !node.isLeaf || node.level === 1,
                'is-leaf': node.isLeaf && node.level > 1,
                active: selectedCategoryId === data.id,
              }"
            >
              <span v-if="node.isLeaf && node.level > 1" class="leaf-dot" />
              <span class="tree-label">{{ data.name }}</span>
              <span class="tree-count">({{ data.questionCount }})</span>
            </div>
          </template>
        </el-tree>
      </aside>

      <section class="main-panel">
        <div class="filter-toolbar">
          <div class="filter-row">
            <div class="filter-field">
              <span class="filter-label">题目类型</span>
              <el-select v-model="filters.type" class="filter-item">
                <el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div class="filter-field">
              <span class="filter-label">难度</span>
              <el-select v-model="filters.difficulty" class="filter-item">
                <el-option v-for="item in QUESTION_DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div class="filter-field">
              <span class="filter-label">状态</span>
              <el-select v-model="filters.status" class="filter-item">
                <el-option v-for="item in QUESTION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div class="filter-field">
              <span class="filter-label">标签</span>
              <el-select v-model="selectedTags" multiple collapse-tags collapse-tags-tooltip placeholder="请选择标签" class="filter-item filter-tags">
                <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
              </el-select>
            </div>
            <div class="filter-field filter-field-search">
              <span class="filter-label">搜索</span>
              <el-input v-model="filters.keyword" placeholder="搜索题干、标签" clearable class="filter-search" @keyup.enter="search" />
            </div>
            <el-button type="primary" class="create-btn" @click="openCreate">+ 新建题目</el-button>
          </div>

          <div class="tabs-row">
            <div class="type-tabs">
              <button
                v-for="tab in QUESTION_TYPE_TABS"
                :key="tab.key"
                type="button"
                class="type-tab"
                :class="{ active: activeTypeTab === tab.value }"
                @click="switchTypeTab(tab.value)"
              >
                {{ tab.label }} ({{ typeCounts[tab.key] ?? 0 }})
              </button>
            </div>
            <el-dropdown trigger="click" @command="handleBatch">
              <el-button>批量操作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="publish">批量发布</el-dropdown-item>
                  <el-dropdown-item command="disable">批量停用</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>批量删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="table-card">
          <table class="question-table">
            <thead>
              <tr>
                <th class="col-check">
                  <el-checkbox v-model="checkAll" :indeterminate="indeterminate" @change="toggleCheckAll" />
                </th>
                <th>题目内容</th>
                <th>题目类型</th>
                <th>难度</th>
                <th>标签</th>
                <th>使用次数</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in questions" :key="item.id">
                <td class="col-check">
                  <el-checkbox :model-value="selectedIds.includes(item.id)" @change="(v: boolean) => toggleRow(item.id, v)" />
                </td>
                <td class="content-cell">
                  <p class="content-text">{{ item.content }}</p>
                </td>
                <td>{{ questionTypeText(item.type) }}</td>
                <td>
                  <span class="difficulty-pill" :class="difficultyClass(item.difficulty)">
                    {{ difficultyLabel(item.difficulty) }}
                  </span>
                </td>
                <td class="tags-cell">
                  <span
                    v-for="(tag, idx) in item.tags"
                    :key="tag"
                    class="tag-pill"
                    :class="tagColorClass(idx)"
                  >{{ tag }}</span>
                </td>
                <td>{{ item.usageCount }}</td>
                <td>{{ item.updatedAt || '-' }}</td>
                <td class="action-cell">
                  <div class="action-links">
                    <button type="button" class="link-btn" @click="openEdit(item)">编辑</button>
                    <button type="button" class="link-btn" @click="openPreview(item)">预览</button>
                    <el-dropdown trigger="click" @command="(cmd: string) => handleMore(cmd, item)">
                      <button type="button" class="link-btn">更多</button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-if="item.status !== 'published'" command="publish">发布</el-dropdown-item>
                          <el-dropdown-item v-if="item.status === 'published'" command="disable">停用</el-dropdown-item>
                          <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <el-empty v-if="!loading && !questions.length" description="暂无题目数据" />
        </div>

        <div class="pagination-bar">
          <span>共 {{ total }} 条</span>
          <el-pagination
            v-model:current-page="filters.page"
            v-model:page-size="filters.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="sizes, prev, pager, next, jumper"
            @current-change="loadQuestions"
            @size-change="onPageSizeChange"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="previewVisible" title="题目预览" width="560px">
      <div v-if="previewing" class="preview-body">
        <p class="preview-content">{{ previewing.content }}</p>
        <p class="preview-meta">
          <span>{{ questionTypeText(previewing.type) }}</span>
          <span>{{ difficultyLabel(previewing.difficulty) }}</span>
          <span>{{ statusLabel(previewing.status) }}</span>
        </p>
        <div v-if="previewOptions.length" class="preview-options">
          <div v-for="opt in previewOptions" :key="opt.id" class="preview-option">
            {{ opt.text }}
          </div>
        </div>
        <p v-if="previewing.explanation" class="preview-explanation">解析：{{ previewing.explanation }}</p>
      </div>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="editingId ? '编辑题目' : '新建题目'" width="640px">
      <el-form label-width="88px">
        <el-form-item label="所属分类" required>
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in leafCategories" :key="cat.id" :label="cat.label" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目类型" required>
          <el-select v-model="editForm.type" style="width: 100%">
            <el-option v-for="item in QUESTION_TYPE_OPTIONS.filter(i => i.value !== 'all')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="editForm.difficulty" style="width: 100%">
            <el-option v-for="item in QUESTION_DIFFICULTY_OPTIONS.filter(i => i.value !== 'all')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option v-for="item in QUESTION_STATUS_OPTIONS.filter(i => i.value !== 'all')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="editForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="editForm.tags" multiple filterable allow-create default-first-option placeholder="输入或选择标签" style="width: 100%">
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="editForm.explanation" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, Search } from '@element-plus/icons-vue'
import {
  adminApi,
  type AdminQuestionCategoryNode,
  type AdminQuestionDetail,
  type AdminQuestionListItem,
} from '@/api/admin'
import {
  QUESTION_DIFFICULTY_OPTIONS,
  QUESTION_STATUS_OPTIONS,
  QUESTION_TYPE_OPTIONS,
  QUESTION_TYPE_TABS,
  difficultyClass,
  difficultyLabel,
  questionTypeText,
  statusLabel,
  tagColorClass,
} from './questionBankShared'

const loading = ref(false)
const categories = ref<AdminQuestionCategoryNode[]>([])
const categoryKeyword = ref('')
const selectedCategoryId = ref('all')
const questions = ref<AdminQuestionListItem[]>([])
const total = ref(0)
const allTags = ref<string[]>([])
const typeCounts = ref<Record<string, number>>({ all: 0 })
const activeTypeTab = ref('all')
const selectedTags = ref<string[]>([])
const selectedIds = ref<string[]>([])
const checkAll = ref(false)
const indeterminate = ref(false)

const previewVisible = ref(false)
const previewing = ref<AdminQuestionDetail | null>(null)
const editVisible = ref(false)
const editingId = ref<string | null>(null)

const editForm = reactive({
  categoryId: '',
  type: 'single',
  difficulty: 'medium',
  status: 'published',
  content: '',
  tags: [] as string[],
  explanation: '',
})

const filters = reactive({
  type: 'all',
  difficulty: 'all',
  status: 'all',
  keyword: '',
  page: 1,
  pageSize: 10,
})

const treeProps = { label: 'name', children: 'children' }

const allCategoryCount = computed(() => {
  const allNode = categories.value.find(c => c.id === 'all')
  return allNode?.questionCount ?? 0
})

const treeData = computed(() => categories.value.filter(c => c.id !== 'all'))

const treeCurrentKey = computed(() => (selectedCategoryId.value === 'all' ? undefined : selectedCategoryId.value))

const categoryTreeKey = computed(() => `${categoryKeyword.value}:${categories.value.map(c => c.id).join(',')}`)

const defaultExpandedKeys = computed(() => {
  const keys: string[] = []
  const selected = selectedCategoryId.value

  for (const node of treeData.value) {
    if (!node.children?.length) continue
    const containsSelected = node.children.some(child =>
      child.id === selected || child.children?.some(sub => sub.id === selected),
    )
    if (containsSelected || !selected || selected === 'all') {
      keys.push(node.id)
    }
  }
  return keys
})

function onTreeNodeClick(data: AdminQuestionCategoryNode) {
  selectCategory(data.id)
}

const leafCategories = computed(() => {
  const result: Array<{ id: string; label: string }> = []
  for (const node of categories.value) {
    if (node.id === 'all') continue
    if (node.children?.length) {
      for (const child of node.children) {
        if (child.children?.length) {
          for (const sub of child.children) {
            result.push({ id: sub.id, label: `${node.name} / ${child.name} / ${sub.name}` })
          }
        } else {
          result.push({ id: child.id, label: `${node.name} / ${child.name}` })
        }
      }
    } else {
      result.push({ id: node.id, label: node.name })
    }
  }
  return result
})

const previewOptions = computed(() => {
  if (!previewing.value?.options) return []
  try {
    const parsed = typeof previewing.value.options === 'string'
      ? JSON.parse(previewing.value.options)
      : previewing.value.options
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

function buildQueryParams() {
  return {
    categoryId: selectedCategoryId.value === 'all' ? undefined : selectedCategoryId.value,
    type: activeTypeTab.value === 'all' ? (filters.type === 'all' ? undefined : filters.type) : activeTypeTab.value,
    difficulty: filters.difficulty === 'all' ? undefined : filters.difficulty,
    status: filters.status === 'all' ? undefined : filters.status,
    tags: selectedTags.value.length ? selectedTags.value.join(',') : undefined,
    keyword: filters.keyword || undefined,
    page: filters.page,
    pageSize: filters.pageSize,
  }
}

async function loadCategories() {
  const res = await adminApi.getQuestionCategories(categoryKeyword.value || undefined)
  categories.value = res.data || []
}

async function loadTags() {
  const res = await adminApi.getQuestionTags()
  allTags.value = res.data || []
}

async function loadTypeCounts() {
  const { page, pageSize, ...rest } = buildQueryParams()
  void page
  void pageSize
  const res = await adminApi.getQuestionTypeCounts(rest)
  typeCounts.value = res.data || { all: 0 }
}

async function loadQuestions() {
  loading.value = true
  try {
    const res = await adminApi.getQuestions(buildQueryParams())
    questions.value = res.data.items || []
    total.value = res.data.total || 0
    selectedIds.value = []
    checkAll.value = false
    indeterminate.value = false
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadCategories(), loadTags(), loadTypeCounts(), loadQuestions()])
}

function selectCategory(id: string) {
  selectedCategoryId.value = id
  filters.page = 1
  loadTypeCounts()
  loadQuestions()
}

function switchTypeTab(value: string) {
  activeTypeTab.value = value
  filters.page = 1
  loadQuestions()
}

function search() {
  filters.page = 1
  loadTypeCounts()
  loadQuestions()
}

function onPageSizeChange() {
  filters.page = 1
  loadQuestions()
}

function toggleRow(id: string, checked: boolean) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
  syncCheckAllState()
}

function toggleCheckAll(checked: boolean) {
  selectedIds.value = checked ? questions.value.map(q => q.id) : []
  indeterminate.value = false
  checkAll.value = checked
}

function syncCheckAllState() {
  const count = selectedIds.value.length
  checkAll.value = count > 0 && count === questions.value.length
  indeterminate.value = count > 0 && count < questions.value.length
}

watch(selectedIds, syncCheckAllState)

function openCreate() {
  editingId.value = null
  editForm.categoryId = leafCategories.value[0]?.id || ''
  editForm.type = 'single'
  editForm.difficulty = 'medium'
  editForm.status = 'published'
  editForm.content = ''
  editForm.tags = []
  editForm.explanation = ''
  editVisible.value = true
}

async function openEdit(item: AdminQuestionListItem) {
  const res = await adminApi.getQuestionById(item.id)
  const detail = res.data
  editingId.value = detail.id
  editForm.categoryId = detail.categoryId
  editForm.type = detail.type
  editForm.difficulty = detail.difficulty
  editForm.status = detail.status
  editForm.content = detail.content
  editForm.tags = [...(detail.tags || [])]
  editForm.explanation = detail.explanation || ''
  editVisible.value = true
}

async function openPreview(item: AdminQuestionListItem) {
  const res = await adminApi.getQuestionById(item.id)
  previewing.value = res.data
  previewVisible.value = true
}

async function saveQuestion() {
  const payload = {
    categoryId: editForm.categoryId,
    type: editForm.type,
    difficulty: editForm.difficulty,
    status: editForm.status,
    content: editForm.content,
    tags: editForm.tags,
    explanation: editForm.explanation,
  }
  if (editingId.value) {
    await adminApi.updateQuestion(editingId.value, payload)
    ElMessage.success('保存成功')
  } else {
    await adminApi.createQuestion(payload)
    ElMessage.success('创建成功')
  }
  editVisible.value = false
  await refreshAll()
}

async function handleMore(command: string, item: AdminQuestionListItem) {
  if (command === 'publish') {
    await adminApi.updateQuestion(item.id, { status: 'published' })
    ElMessage.success('已发布')
  } else if (command === 'disable') {
    await adminApi.updateQuestion(item.id, { status: 'disabled' })
    ElMessage.success('已停用')
  } else if (command === 'delete') {
    await ElMessageBox.confirm('确定删除该题目吗？', '提示', { type: 'warning' })
    await adminApi.deleteQuestion(item.id)
    ElMessage.success('删除成功')
  }
  await refreshAll()
}

async function handleBatch(command: string) {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择题目')
    return
  }
  if (command === 'delete') {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 道题目吗？`, '提示', { type: 'warning' })
  }
  await adminApi.batchOperateQuestions({ ids: selectedIds.value, action: command })
  ElMessage.success('操作成功')
  await refreshAll()
}

watch([() => filters.type, () => filters.difficulty, () => filters.status, selectedTags], () => {
  filters.page = 1
  loadTypeCounts()
  loadQuestions()
})

onMounted(refreshAll)
</script>

<style scoped>
.question-bank-page {
  padding: 20px 24px 24px;
  min-height: 100%;
  box-sizing: border-box;
}

.page-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.category-panel {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  padding: 16px;
  position: sticky;
  top: 16px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.category-search {
  margin-bottom: 12px;
}

.all-category-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 0;
  background: none;
  padding: 8px 10px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  text-align: left;
  color: #374151;
  font-size: 13px;
}

.all-category-item:hover,
.all-category-item.active {
  background: #eff6ff;
  color: #2563eb;
}

.all-icon {
  font-size: 16px;
  color: #2563eb;
  flex-shrink: 0;
}

.category-tree {
  max-height: calc(100vh - 260px);
  overflow: auto;
  --el-tree-node-hover-bg-color: #f8fafc;
  --el-tree-text-color: #374151;
  --el-tree-expand-icon-color: #2563eb;
}

.category-tree :deep(.el-tree) {
  background: transparent;
}

.category-tree :deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 6px;
  padding-right: 8px;
}

.category-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #eff6ff;
}

.category-tree :deep(.el-tree-node__expand-icon) {
  color: #2563eb;
  font-size: 14px;
}

.category-tree :deep(.el-tree-node__expand-icon.is-leaf) {
  color: transparent;
}

.category-tree :deep(.el-tree-node__children .el-tree-node__content) {
  padding-left: 6px;
}

.tree-node {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-node.is-parent .tree-label {
  font-weight: 600;
  color: #2563eb;
}

.tree-node.is-leaf .tree-label {
  font-weight: 400;
  color: #374151;
}

.tree-node.active .tree-label {
  color: #2563eb;
}

.leaf-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
  margin-left: 2px;
}

.tree-node.active .leaf-dot {
  background: #60a5fa;
}

.tree-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.tree-count {
  color: #9ca3af;
  font-size: 12px;
  flex-shrink: 0;
}

.tree-node.active .tree-count,
.all-category-item.active .tree-count {
  color: #60a5fa;
}

.main-panel {
  min-width: 0;
}

.filter-toolbar {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 12px;
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

.filter-item {
  width: 140px;
}

.filter-tags {
  width: 180px;
}

.filter-field-search .filter-search {
  width: 200px;
}

.create-btn {
  margin-left: auto;
}

.tabs-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.type-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-tab {
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #374151;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
}

.type-tab.active {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

.table-card {
  background: #fff;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  overflow: hidden;
}

.question-table {
  width: 100%;
  border-collapse: collapse;
}

.question-table th,
.question-table td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
  vertical-align: middle;
}

.question-table th {
  background: #fafbfc;
  color: #667085;
  font-weight: 600;
}

.col-check {
  width: 44px;
}

.content-cell {
  max-width: 360px;
}

.content-text {
  margin: 0;
  color: #1f2937;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.difficulty-pill {
  display: inline-block;
  min-width: 28px;
  text-align: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.difficulty-easy { color: #16a34a; background: #ecfdf3; }
.difficulty-medium { color: #ca8a04; background: #fefce8; }
.difficulty-hard { color: #dc2626; background: #fef2f2; }

.tags-cell {
  max-width: 220px;
}

.tag-pill {
  display: inline-block;
  margin: 0 4px 4px 0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.tag-blue { color: #2563eb; background: #eff6ff; }
.tag-green { color: #16a34a; background: #ecfdf3; }
.tag-orange { color: #ea580c; background: #fff7ed; }
.tag-purple { color: #7c3aed; background: #f5f3ff; }
.tag-cyan { color: #0891b2; background: #ecfeff; }

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

.preview-body {
  color: #374151;
}

.preview-content {
  font-size: 15px;
  line-height: 1.6;
  margin: 0 0 12px;
}

.preview-meta {
  display: flex;
  gap: 12px;
  color: #9ca3af;
  font-size: 12px;
  margin: 0 0 12px;
}

.preview-options {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.preview-option {
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}

.preview-explanation {
  margin: 0;
  padding: 10px 12px;
  background: #f0fdf4;
  border-radius: 6px;
  font-size: 13px;
  color: #166534;
}
</style>
