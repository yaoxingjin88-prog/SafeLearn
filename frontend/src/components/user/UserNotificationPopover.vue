<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom-end"
    :width="360"
    trigger="click"
    popper-class="user-inbox-popover"
    @show="loadSummary"
  >
    <template #reference>
      <button type="button" class="notify-btn" title="通知">
        <el-icon><Bell /></el-icon>
        <em v-if="unreadCount > 0">{{ badgeText(unreadCount) }}</em>
      </button>
    </template>

    <div class="inbox-panel" v-loading="loading">
      <header class="inbox-header">
        <div>
          <strong>学习通知</strong>
          <span>管理员提醒与系统消息</span>
        </div>
        <button type="button" class="inbox-action" :disabled="!unreadCount" @click="markAllRead">
          全部已读
        </button>
      </header>

      <ul v-if="items.length" class="inbox-list">
        <li
          v-for="item in items"
          :key="item.id"
          class="inbox-item"
          :class="{ unread: !item.read }"
          @click="openItem(item)"
        >
          <span class="item-icon">
            <el-icon><Bell /></el-icon>
          </span>
          <div class="item-body">
            <strong>{{ item.title }}</strong>
            <p>{{ item.content }}</p>
            <time>{{ item.time || '—' }}</time>
          </div>
        </li>
      </ul>
      <el-empty v-else description="暂无通知" :image-size="56" />
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { dashboardApi, type UserNotificationItem } from '@/api/dashboard'

const router = useRouter()
const visible = ref(false)
const loading = ref(false)
const items = ref<UserNotificationItem[]>([])
const unreadCount = ref(0)

function badgeText(count: number) {
  return count > 99 ? '99+' : String(count)
}

async function loadSummary() {
  loading.value = true
  try {
    const res = await dashboardApi.getNotificationSummary()
    items.value = res.data.items
    unreadCount.value = res.data.unreadCount
  } catch {
    items.value = []
    unreadCount.value = 0
  } finally {
    loading.value = false
  }
}

async function markAllRead() {
  try {
    await dashboardApi.markAllNotificationsRead()
    items.value = items.value.map(item => ({ ...item, read: true }))
    unreadCount.value = 0
  } catch {
    // ignore
  }
}

async function openItem(item: UserNotificationItem) {
  if (!item.read) {
    try {
      await dashboardApi.markNotificationRead(item.id)
      item.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // ignore
    }
  }
  visible.value = false
  if (item.actionPath) {
    router.push(item.actionPath)
  }
}

defineExpose({ refresh: loadSummary })

loadSummary()
</script>

<style scoped>
.notify-btn {
  position: relative;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #f3f5f9;
  color: #5b6678;
  cursor: pointer;
  display: grid;
  place-items: center;
  font-size: 18px;
}

.notify-btn em {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 99px;
  background: #ef4c55;
  color: #fff;
  font-style: normal;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

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
  grid-template-columns: 32px minmax(0, 1fr);
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
  color: #3d80ed;
  background: #eef4ff;
}

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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-body time {
  font-size: 11px;
  color: #a1a9b4;
}
</style>
