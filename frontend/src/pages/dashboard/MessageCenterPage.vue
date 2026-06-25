<template>
  <div class="inbox-center-page" v-loading="loading">
    <header class="page-top">
      <button type="button" class="back-link" @click="router.push('/dashboard')">
        <el-icon><ArrowLeft /></el-icon>
        返回首页
      </button>
      <div class="page-title-row">
        <div>
          <h1>消息中心</h1>
          <p>平台公告与业务告知 · 数据更新于 {{ data?.generatedAt || '—' }}</p>
        </div>
        <div class="title-actions">
          <el-button type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon>
            发布公告
          </el-button>
          <el-button @click="markAllRead" :disabled="!data?.unreadCount">全部已读</el-button>
          <el-button @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
    </header>

    <section v-if="data" class="stats-grid">
      <article class="stat-card" :class="{ active: filters.unreadOnly }" @click="toggleUnreadOnly">
        <span class="stat-icon tone-blue"><el-icon><Message /></el-icon></span>
        <div class="stat-content">
          <span class="stat-label">未读消息</span>
          <strong>{{ data.unreadCount ?? 0 }}<small>条</small></strong>
        </div>
      </article>
      <article class="stat-card">
        <span class="stat-icon tone-purple"><el-icon><ChatDotRound /></el-icon></span>
        <div class="stat-content">
          <span class="stat-label">全部消息</span>
          <strong>{{ data.total ?? 0 }}<small>条</small></strong>
        </div>
      </article>
    </section>

    <div class="page-toolbar">
      <div class="filter-row">
        <div class="filter-field filter-field-search">
          <span class="filter-label">关键词</span>
          <el-input v-model="filters.keyword" placeholder="搜索标题" clearable @keyup.enter="applyFilters" />
        </div>
        <div class="filter-field">
          <span class="filter-label">消息类型</span>
          <el-select v-model="filters.type" class="filter-item">
            <el-option v-for="item in MESSAGE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="applyFilters">查询</el-button>
        </div>
      </div>
    </div>

    <article class="panel table-panel">
      <header class="panel-header">
        <div>
          <h2>消息列表</h2>
          <p>共 {{ filteredItems.length }} 条</p>
        </div>
      </header>

      <div class="table-card">
        <table class="inbox-table">
          <thead>
            <tr>
              <th>类型</th>
              <th>标题</th>
              <th>日期</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in pagedItems"
              :key="item.id"
              :class="{ unread: !isRead(item) }"
              @click="openItem(item)"
            >
              <td>{{ messageTypeLabel(item.type) }}</td>
              <td class="content-cell">
                <strong>{{ item.title }}</strong>
                <p v-if="item.body">{{ item.body }}</p>
              </td>
              <td>{{ item.date || '—' }}</td>
              <td>
                <span v-if="item.pinned" class="pin-tag">置顶</span>
                <span v-else class="muted-text">{{ isRead(item) ? '已读' : '未读' }}</span>
              </td>
              <td class="action-cell" @click.stop>
                <button v-if="item.actionPath" type="button" class="link-btn" @click="openItem(item)">
                  {{ inboxActionLabel(item.type) }}
                </button>
                <template v-if="item.persisted">
                  <button type="button" class="link-btn" @click="openEdit(item)">编辑</button>
                  <button type="button" class="link-btn danger" @click="removeItem(item)">删除</button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
        <el-empty v-if="!loading && !filteredItems.length" description="暂无消息" :image-size="64" />
      </div>

      <div v-if="filteredItems.length" class="pagination-bar">
        <span>共 {{ filteredItems.length }} 条</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="filteredItems.length"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          @size-change="onPageSizeChange"
        />
      </div>
    </article>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑公告' : '发布公告'"
      width="560px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form label-width="88px" @submit.prevent>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.body" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="可选，补充说明" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" class="w-full">
            <el-option
              v-for="item in MESSAGE_TYPE_OPTIONS.filter(opt => opt.value !== 'all')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.actionPath" placeholder="可选，如 /admin/learning/courses" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMessage">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh, Message, ChatDotRound, Plus } from '@element-plus/icons-vue'
import { adminApi, type AdminInboxPage, type AdminMessageItem } from '@/api/admin'
import { onAdminInboxUpdate, useAdminInbox } from '@/composables/useAdminInbox'
import {
  MESSAGE_TYPE_OPTIONS,
  inboxActionLabel,
  messageTypeLabel,
  navigateInboxAction,
} from './inboxCenterShared'

type TypeFilter = typeof MESSAGE_TYPE_OPTIONS[number]['value']

const router = useRouter()
const loading = ref(false)
const data = ref<AdminInboxPage<AdminMessageItem> | null>(null)
const page = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)

const form = reactive({
  title: '',
  body: '',
  type: 'announcement',
  actionPath: '',
  pinned: false,
})

const { isMessageRead, markMessageRead, markAllMessagesRead, refresh: refreshInbox } = useAdminInbox()

const filters = reactive({
  keyword: '',
  type: 'all' as TypeFilter,
  unreadOnly: false,
})

const applied = reactive({ ...filters })

const filteredItems = computed(() => {
  const items = data.value?.items || []
  return items.filter(item => {
    if (applied.type !== 'all' && item.type !== applied.type) return false
    if (applied.keyword.trim()) {
      const q = applied.keyword.trim().toLowerCase()
      const text = `${item.title} ${item.body || ''}`.toLowerCase()
      if (!text.includes(q)) return false
    }
    return true
  })
})

const pagedItems = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

function isRead(item: AdminMessageItem) {
  return isMessageRead(item.id) || Boolean(item.read)
}

function toggleUnreadOnly() {
  filters.unreadOnly = !filters.unreadOnly
  page.value = 1
  loadData()
}

function applyFilters() {
  Object.assign(applied, filters)
  page.value = 1
}

function resetFilters() {
  filters.keyword = ''
  filters.type = 'all'
  filters.unreadOnly = false
  applyFilters()
  loadData()
}

function onPageSizeChange() {
  page.value = 1
  loadData()
}

async function openItem(item: AdminMessageItem) {
  await markMessageRead(item.id)
  item.read = true
  if (item.actionPath) {
    navigateInboxAction(router, item.actionPath)
  }
}

async function markAllRead() {
  await markAllMessagesRead()
  data.value?.items.forEach(item => { item.read = true })
}

function resetForm() {
  editingId.value = null
  form.title = ''
  form.body = ''
  form.type = 'announcement'
  form.actionPath = ''
  form.pinned = false
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item: AdminMessageItem) {
  editingId.value = item.id
  form.title = item.title
  form.body = item.body || ''
  form.type = item.type || 'announcement'
  form.actionPath = item.actionPath || ''
  form.pinned = Boolean(item.pinned)
  dialogVisible.value = true
}

async function saveMessage() {
  const title = form.title.trim()
  if (!title) {
    ElMessage.warning('请输入公告标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      title,
      body: form.body.trim(),
      type: form.type,
      actionPath: form.actionPath.trim() || undefined,
      pinned: form.pinned,
    }
    if (editingId.value) {
      await adminApi.updateMessage(editingId.value, payload)
      ElMessage.success('公告已更新')
    } else {
      await adminApi.createMessage(payload)
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    await loadData()
    await refreshInbox()
  } catch {
    ElMessage.error(editingId.value ? '更新失败' : '发布失败')
  } finally {
    saving.value = false
  }
}

async function removeItem(item: AdminMessageItem) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${item.title}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await adminApi.deleteMessage(item.id)
    ElMessage.success('已删除')
    await loadData()
    await refreshInbox()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.listMessages({
      page: 1,
      pageSize: 100,
      unreadOnly: filters.unreadOnly,
    })
    data.value = res.data
    Object.assign(applied, filters)
    page.value = 1
  } finally {
    loading.value = false
  }
}

let stopInboxListen: (() => void) | undefined

onMounted(() => {
  loadData()
  stopInboxListen = onAdminInboxUpdate(loadData)
})

onUnmounted(() => stopInboxListen?.())
</script>

<style scoped>
.inbox-center-page {
  min-height: 100%;
  padding: 20px 24px 16px;
  box-sizing: border-box;
  color: #243044;
  background: #f5f7fc;
}

.page-top { margin-bottom: 14px; }

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  color: #7c8695;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
  margin-bottom: 10px;
}

.page-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-title-row h1 {
  margin: 0;
  font-size: 22px;
}

.page-title-row p {
  margin: 6px 0 0;
  color: #7c8695;
  font-size: 13px;
}

.title-actions {
  display: flex;
  gap: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  cursor: pointer;
}

.stat-card.active {
  border-color: #3478ef;
  box-shadow: 0 0 0 1px #3478ef inset;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 18px;
}

.tone-blue { color: #3478ef; background: #edf3ff; }
.tone-purple { color: #7b61ff; background: #f3efff; }

.stat-content strong {
  font-size: 22px;
}

.stat-content small {
  font-size: 12px;
  margin-left: 2px;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #8b95a4;
}

.page-toolbar,
.panel {
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 10px;
}

.page-toolbar {
  padding: 14px 16px;
  margin-bottom: 14px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-field-search {
  flex: 1;
  min-width: 220px;
}

.filter-label {
  font-size: 12px;
  color: #8b95a4;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.panel-header {
  padding: 16px 16px 0;
}

.panel-header h2 {
  margin: 0;
  font-size: 16px;
}

.panel-header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8b95a4;
}

.table-card {
  padding: 0 16px 16px;
}

.inbox-table {
  width: 100%;
  border-collapse: collapse;
}

.inbox-table th,
.inbox-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #f0f3f8;
  text-align: left;
  font-size: 13px;
}

.inbox-table th {
  color: #8b95a4;
  font-weight: 500;
}

.inbox-table tr.unread {
  background: #f8fbff;
}

.content-cell strong {
  display: block;
  color: #243044;
}

.content-cell p {
  margin: 4px 0 0;
  color: #778293;
  font-size: 12px;
}

.pin-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #ef5d62;
  background: #ffe9e9;
}

.muted-text {
  color: #a1a9b4;
  font-size: 12px;
}

.link-btn {
  border: none;
  background: none;
  color: #3478ef;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
  margin-right: 8px;
}

.link-btn.danger {
  color: #ef4c55;
}

.action-cell {
  white-space: nowrap;
}

.w-full {
  width: 100%;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px 16px;
  font-size: 12px;
  color: #8b95a4;
}
</style>
