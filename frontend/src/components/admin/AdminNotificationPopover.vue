<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom-end"
    :width="360"
    trigger="click"
    popper-class="admin-inbox-popover"
    @show="onOpen"
  >
    <template #reference>
      <button type="button" class="header-icon-btn" title="通知">
        <el-icon><Bell /></el-icon>
        <em v-if="unreadNotificationCount > 0">{{ badgeText(unreadNotificationCount) }}</em>
      </button>
    </template>

    <div class="inbox-panel" v-loading="loading">
      <header class="inbox-header">
        <div>
          <strong>通知</strong>
          <span>需处理的预警与待办</span>
        </div>
        <button type="button" class="inbox-action" :disabled="!unreadNotificationCount" @click="markAllRead">
          全部已读
        </button>
      </header>

      <ul v-if="notifications.length" class="inbox-list">
        <li
          v-for="item in notifications"
          :key="item.id"
          class="inbox-item"
          :class="{ unread: !isNotificationRead(item.id) }"
          @click="openItem(item)"
        >
          <span class="item-icon" :class="`level-${item.level}`">
            <el-icon><WarningFilled /></el-icon>
          </span>
          <div class="item-body">
            <strong>{{ item.title }}</strong>
            <p>{{ item.description }}</p>
            <time>{{ item.time || '—' }}</time>
          </div>
          <em class="item-tag" :class="`level-${item.level}`">{{ levelLabel(item.level) }}</em>
        </li>
      </ul>
      <el-empty v-else description="暂无通知" :image-size="56" />

      <footer class="inbox-footer">
        <button type="button" class="inbox-link" @click="goNotificationCenter">查看全部通知</button>
      </footer>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, WarningFilled } from '@element-plus/icons-vue'
import type { AdminNotificationItem } from '@/api/admin'
import { useAdminInbox } from '@/composables/useAdminInbox'

const router = useRouter()
const visible = ref(false)

const {
  loading,
  notifications,
  unreadNotificationCount,
  isNotificationRead,
  markNotificationRead,
  markAllNotificationsRead,
  refresh,
} = useAdminInbox()

function badgeText(count: number) {
  return count > 99 ? '99+' : String(count)
}

function levelLabel(level: AdminNotificationItem['level']) {
  return { danger: '高危', warning: '中危', info: '提醒' }[level]
}

function onOpen() {
  refresh()
}

function markAllRead() {
  markAllNotificationsRead()
}

function openItem(item: AdminNotificationItem) {
  markNotificationRead(item.id)
  visible.value = false
  if (item.actionPath) {
    const [pathname, search] = item.actionPath.split('?')
    router.push(search ? { path: pathname, query: Object.fromEntries(new URLSearchParams(search)) } : pathname)
  }
}

function goNotificationCenter() {
  visible.value = false
  router.push('/dashboard/notifications')
}
</script>

<style scoped>
.inbox-panel {
  min-height: 120px;
}

.inbox-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf0f4;
  margin-bottom: 8px;
}

.inbox-header strong {
  display: block;
  font-size: 14px;
  color: #1c2738;
}

.inbox-header span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: #8b95a4;
}

.inbox-action {
  border: none;
  background: none;
  color: #3478ef;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  white-space: nowrap;
}

.inbox-action:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.inbox-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 360px;
  overflow-y: auto;
}

.inbox-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid #f2f4f7;
  cursor: pointer;
  border-radius: 6px;
}

.inbox-item:hover {
  background: #f8fafc;
}

.inbox-item.unread {
  background: #f8fbff;
}

.item-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 14px;
}

.level-danger { color: #ef4c55; background: #fff0f1; }
.level-warning { color: #f38b3b; background: #fff5ea; }
.level-info { color: #3d80ed; background: #eef4ff; }

.item-body strong {
  display: block;
  font-size: 13px;
  color: #364153;
}

.item-body p {
  margin: 3px 0;
  font-size: 12px;
  color: #778293;
  line-height: 1.4;
}

.item-body time {
  font-size: 11px;
  color: #a1a9b4;
}

.item-tag {
  font-style: normal;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  align-self: start;
  white-space: nowrap;
}

.inbox-footer {
  padding-top: 10px;
  border-top: 1px solid #edf0f4;
  margin-top: 4px;
}

.inbox-link {
  border: none;
  background: none;
  color: #3478ef;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}
</style>
