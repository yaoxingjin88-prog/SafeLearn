<template>
  <div class="inbox-center-page" v-loading="loading">
    <header class="page-top">
      <button type="button" class="back-link" @click="router.push('/dashboard')">
        <el-icon><ArrowLeft /></el-icon>
        返回首页
      </button>
      <div class="page-title-row">
        <div>
          <h1>通知中心</h1>
          <p>业务事件与动态预警 · 数据更新于 {{ data?.generatedAt || '—' }}</p>
        </div>
        <div class="title-actions">
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
        <span class="stat-icon tone-red"><el-icon><BellFilled /></el-icon></span>
        <div class="stat-content">
          <span class="stat-label">未读通知</span>
          <strong>{{ data.unreadCount ?? 0 }}<small>条</small></strong>
        </div>
      </article>
      <article class="stat-card">
        <span class="stat-icon tone-blue"><el-icon><Document /></el-icon></span>
        <div class="stat-content">
          <span class="stat-label">全部通知</span>
          <strong>{{ data.total ?? 0 }}<small>条</small></strong>
        </div>
      </article>
    </section>

    <div class="page-toolbar">
      <div class="filter-row">
        <div class="filter-field filter-field-search">
          <span class="filter-label">关键词</span>
          <el-input v-model="filters.keyword" placeholder="搜索标题或内容" clearable @keyup.enter="applyFilters" />
        </div>
        <div class="filter-field">
          <span class="filter-label">风险等级</span>
          <el-select v-model="filters.level" class="filter-item">
            <el-option v-for="item in NOTIFICATION_LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="filter-field">
          <span class="filter-label">通知类型</span>
          <el-select v-model="filters.type" class="filter-item">
            <el-option v-for="item in NOTIFICATION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
          <h2>通知列表</h2>
          <p>共 {{ filteredItems.length }} 条，点击条目可标记已读并跳转</p>
        </div>
      </header>

      <div class="table-card">
        <table class="inbox-table">
          <thead>
            <tr>
              <th>类型</th>
              <th>通知内容</th>
              <th>等级</th>
              <th>时间</th>
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
              <td>{{ notificationTypeLabel(item.type) }}</td>
              <td class="content-cell">
                <strong>{{ item.title }}</strong>
                <p>{{ item.description }}</p>
              </td>
              <td>
                <span class="level-tag" :class="`level-${item.level}`">{{ notificationLevelLabel(item.level) }}</span>
              </td>
              <td>{{ item.time || '—' }}</td>
              <td class="action-cell" @click.stop>
                <button v-if="item.actionPath" type="button" class="link-btn" @click="openItem(item)">
                  {{ inboxActionLabel(item.type) }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <el-empty v-if="!loading && !filteredItems.length" description="暂无通知" :image-size="64" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Refresh, BellFilled, Document } from '@element-plus/icons-vue'
import { adminApi, type AdminInboxPage, type AdminNotificationItem } from '@/api/admin'
import { onAdminInboxUpdate, useAdminInbox } from '@/composables/useAdminInbox'
import {
  NOTIFICATION_LEVEL_OPTIONS,
  NOTIFICATION_TYPE_OPTIONS,
  inboxActionLabel,
  navigateInboxAction,
  notificationLevelLabel,
  notificationTypeLabel,
} from './inboxCenterShared'

type LevelFilter = typeof NOTIFICATION_LEVEL_OPTIONS[number]['value']
type TypeFilter = typeof NOTIFICATION_TYPE_OPTIONS[number]['value']

const router = useRouter()
const loading = ref(false)
const data = ref<AdminInboxPage<AdminNotificationItem> | null>(null)
const page = ref(1)
const pageSize = ref(20)

const { isNotificationRead, markNotificationRead, markAllNotificationsRead } = useAdminInbox()

const filters = reactive({
  keyword: '',
  level: 'all' as LevelFilter,
  type: 'all' as TypeFilter,
  unreadOnly: false,
})

const applied = reactive({ ...filters })

const filteredItems = computed(() => {
  const items = data.value?.items || []
  return items.filter(item => {
    if (applied.level !== 'all' && item.level !== applied.level) return false
    if (applied.type !== 'all' && item.type !== applied.type) return false
    if (applied.keyword.trim()) {
      const q = applied.keyword.trim().toLowerCase()
      const text = `${item.title} ${item.description}`.toLowerCase()
      if (!text.includes(q)) return false
    }
    return true
  })
})

const pagedItems = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

function isRead(item: AdminNotificationItem) {
  return isNotificationRead(item.id) || Boolean(item.read)
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
  filters.level = 'all'
  filters.type = 'all'
  filters.unreadOnly = false
  applyFilters()
  loadData()
}

function onPageSizeChange() {
  page.value = 1
}

async function openItem(item: AdminNotificationItem) {
  await markNotificationRead(item.id)
  item.read = true
  if (item.actionPath) {
    navigateInboxAction(router, item.actionPath)
  }
}

async function markAllRead() {
  await markAllNotificationsRead()
  data.value?.items.forEach(item => { item.read = true })
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.listNotifications({
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

.tone-red { color: #ef4c55; background: #fff0f1; }
.tone-blue { color: #3478ef; background: #edf3ff; }

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

.level-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.level-danger { color: #ef4c55; background: #fff0f1; }
.level-warning { color: #f38b3b; background: #fff5ea; }
.level-info { color: #3d80ed; background: #eef4ff; }

.link-btn {
  border: none;
  background: none;
  color: #3478ef;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
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
