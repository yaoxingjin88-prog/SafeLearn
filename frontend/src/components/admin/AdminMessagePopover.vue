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
      <button type="button" class="header-icon-btn" title="消息">
        <el-icon><Message /></el-icon>
        <em v-if="unreadMessageCount > 0">{{ badgeText(unreadMessageCount) }}</em>
      </button>
    </template>

    <div class="inbox-panel" v-loading="loading">
      <header class="inbox-header">
        <div>
          <strong>消息</strong>
          <span>平台公告与业务告知</span>
        </div>
        <button type="button" class="inbox-action" :disabled="!unreadMessageCount" @click="markAllRead">
          全部已读
        </button>
      </header>

      <ul v-if="messages.length" class="inbox-list">
        <li
          v-for="item in messages"
          :key="item.id"
          class="inbox-item"
          :class="{ unread: !isMessageRead(item.id) }"
          @click="openItem(item)"
        >
          <span class="item-icon tone-message">
            <el-icon><ChatDotRound /></el-icon>
          </span>
          <div class="item-body">
            <strong>{{ item.title }}</strong>
            <time>{{ formatDate(item.date) }}</time>
          </div>
          <em v-if="item.pinned" class="item-pin">置顶</em>
        </li>
      </ul>
      <el-empty v-else description="暂无消息" :image-size="56" />

      <footer class="inbox-footer">
        <button type="button" class="inbox-link" @click="goMessageCenter">查看全部消息</button>
      </footer>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message, ChatDotRound } from '@element-plus/icons-vue'
import type { AdminMessageItem } from '@/api/admin'
import { useAdminInbox } from '@/composables/useAdminInbox'

const router = useRouter()
const visible = ref(false)

const {
  loading,
  messages,
  unreadMessageCount,
  isMessageRead,
  markMessageRead,
  markAllMessagesRead,
  refresh,
} = useAdminInbox()

function badgeText(count: number) {
  return count > 99 ? '99+' : String(count)
}

function formatDate(date?: string) {
  if (!date) return '—'
  return date.length >= 10 ? date.slice(5) : date
}

function onOpen() {
  refresh()
}

function markAllRead() {
  markAllMessagesRead()
}

function openItem(item: AdminMessageItem) {
  markMessageRead(item.id)
  visible.value = false
  if (item.actionPath) {
    router.push(item.actionPath)
  }
}

function goMessageCenter() {
  visible.value = false
  router.push('/dashboard/messages')
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

.tone-message {
  color: #3478ef;
  background: #edf3ff;
}

.item-body strong {
  display: block;
  font-size: 13px;
  color: #364153;
  line-height: 1.4;
}

.item-body time {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: #a1a9b4;
}

.item-pin {
  font-style: normal;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #ffe9e9;
  color: #ef5d62;
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
