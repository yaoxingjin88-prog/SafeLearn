<template>
  <article class="panel notice-panel" v-loading="loading">
    <header class="panel-header panel-header--small">
      <h2>公告通知</h2>
      <span class="more-link" @click="goMessageCenter">
        更多 <el-icon><ArrowRight /></el-icon>
      </span>
    </header>
    <ul v-if="displayItems.length" class="notice-list">
      <li
        v-for="item in displayItems"
        :key="item.id"
        :class="{ unread: !isMessageRead(item.id) }"
        @click="openItem(item)"
      >
        <span class="notice-title">{{ item.title }}</span>
        <time>{{ formatDate(item.date) }}</time>
        <em v-if="item.pinned">置顶</em>
      </li>
    </ul>
    <el-empty v-else-if="!loading" class="notice-empty" description="暂无公告" :image-size="48" />
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import type { AdminMessageItem } from '@/api/admin'
import { useAdminInbox } from '@/composables/useAdminInbox'
import { navigateInboxAction } from '@/pages/dashboard/inboxCenterShared'

const DISPLAY_LIMIT = 5

const router = useRouter()
const { loading, messages, isMessageRead, markMessageRead } = useAdminInbox()

const displayItems = computed(() =>
  [...messages.value]
    .sort((a, b) => {
      if (Boolean(a.pinned) !== Boolean(b.pinned)) return a.pinned ? -1 : 1
      return (b.date || '').localeCompare(a.date || '')
    })
    .slice(0, DISPLAY_LIMIT),
)

function formatDate(date?: string) {
  if (!date) return '—'
  return date.length >= 10 ? date.slice(5) : date
}

function goMessageCenter() {
  router.push('/dashboard/messages')
}

async function openItem(item: AdminMessageItem) {
  await markMessageRead(item.id)
  if (item.actionPath) {
    navigateInboxAction(router, item.actionPath)
  }
}
</script>

<style scoped>
.notice-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  min-height: 44px;
  padding: 14px 16px 6px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-header h2 {
  margin: 0;
  color: #263246;
  font-size: 15px;
  font-weight: 700;
}

.more-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #8d97a6;
  font-size: 11px;
  white-space: nowrap;
  cursor: pointer;
}

.notice-list {
  margin: 0;
  padding: 0 14px 10px;
  list-style: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.notice-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  min-height: 24px;
  font-size: 11px;
  padding: 4px 0;
  cursor: pointer;
  border-radius: 4px;
}

.notice-list li:hover {
  background: #f8fafc;
}

.notice-list li.unread .notice-title {
  color: #1c2738;
  font-weight: 600;
}

.notice-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #485568;
}

.notice-list time {
  color: #a1a9b4;
  white-space: nowrap;
}

.notice-list em {
  padding: 1px 6px;
  border-radius: 3px;
  background: #ffe9e9;
  color: #ef5d62;
  font-style: normal;
  font-size: 10px;
}

.notice-empty {
  padding: 8px 0 12px;
}
</style>
