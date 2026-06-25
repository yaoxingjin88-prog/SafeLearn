import { computed, onMounted, onUnmounted, ref } from 'vue'
import { adminApi, type AdminMessageItem, type AdminNotificationItem } from '@/api/admin'
import { useUserStore } from '@/stores'
import { connectSseStream } from '@/utils/sseStream'

const POLL_INTERVAL_MS = 60_000
const POLL_SSE_CONNECTED_MS = 300_000
const SSE_RECONNECT_MS = 5_000
const REFRESH_DEBOUNCE_MS = 300

const loading = ref(false)
const notifications = ref<AdminNotificationItem[]>([])
const messages = ref<AdminMessageItem[]>([])
const notificationReadIds = ref<Set<string>>(new Set())
const messageReadIds = ref<Set<string>>(new Set())
const sseConnected = ref(false)

let pollTimer: ReturnType<typeof setInterval> | undefined
let sseAbort: AbortController | undefined
let sseReconnectTimer: ReturnType<typeof setTimeout> | undefined
let refreshDebounceTimer: ReturnType<typeof setTimeout> | undefined
let mountedCount = 0
const inboxUpdateListeners = new Set<() => void>()

export function onAdminInboxUpdate(listener: () => void) {
  inboxUpdateListeners.add(listener)
  return () => inboxUpdateListeners.delete(listener)
}

function notifyInboxUpdate() {
  inboxUpdateListeners.forEach(listener => listener())
}

function readStorageKey(kind: 'notifications' | 'messages', userId?: string) {
  return `safelearn:admin:${kind}:read:${userId || 'guest'}`
}

function loadReadIds(kind: 'notifications' | 'messages', userId?: string): Set<string> {
  try {
    const raw = localStorage.getItem(readStorageKey(kind, userId))
    if (!raw) return new Set()
    const parsed = JSON.parse(raw)
    return new Set(Array.isArray(parsed) ? parsed : [])
  } catch {
    return new Set()
  }
}

function saveReadIds(kind: 'notifications' | 'messages', userId: string | undefined, ids: Set<string>) {
  localStorage.setItem(readStorageKey(kind, userId), JSON.stringify([...ids]))
}

function syncServerReadState() {
  const nextNotificationIds = new Set(notificationReadIds.value)
  notifications.value.forEach(item => {
    if (item.id && item.read) nextNotificationIds.add(item.id)
  })
  notificationReadIds.value = nextNotificationIds

  const nextMessageIds = new Set(messageReadIds.value)
  messages.value.forEach(item => {
    if (item.id && item.read) nextMessageIds.add(item.id)
  })
  messageReadIds.value = nextMessageIds
}

function isDbNotificationId(id: string) {
  return id.startsWith('db:notification:')
}

function isDbMessageId(id: string) {
  return id.startsWith('db:message:')
}

function scheduleDebouncedRefresh(refreshFn: () => Promise<void>) {
  if (refreshDebounceTimer) clearTimeout(refreshDebounceTimer)
  refreshDebounceTimer = setTimeout(() => {
    refreshDebounceTimer = undefined
    void refreshFn().then(() => notifyInboxUpdate())
  }, REFRESH_DEBOUNCE_MS)
}

function restartPolling(refreshFn: () => Promise<void>) {
  if (pollTimer) clearInterval(pollTimer)
  const interval = sseConnected.value ? POLL_SSE_CONNECTED_MS : POLL_INTERVAL_MS
  pollTimer = setInterval(refreshFn, interval)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
}

function stopSse() {
  if (sseReconnectTimer) {
    clearTimeout(sseReconnectTimer)
    sseReconnectTimer = undefined
  }
  if (sseAbort) {
    sseAbort.abort()
    sseAbort = undefined
  }
  sseConnected.value = false
}

async function connectSse(refreshFn: () => Promise<void>, isAdmin: boolean) {
  if (!isAdmin || mountedCount <= 0) return

  stopSse()
  const controller = new AbortController()
  sseAbort = controller

  try {
    await connectSseStream('/api/admin/inbox/stream', (event, _data) => {
      if (event === 'connected') {
        sseConnected.value = true
        restartPolling(refreshFn)
      } else if (event === 'inbox') {
        scheduleDebouncedRefresh(refreshFn)
      }
    }, controller.signal)
  } catch {
    // 断线后重连
  } finally {
    const shouldReconnect = mountedCount > 0 && sseAbort === controller
    sseConnected.value = false
    restartPolling(refreshFn)
    if (shouldReconnect) {
      sseReconnectTimer = setTimeout(() => connectSse(refreshFn, isAdmin), SSE_RECONNECT_MS)
    }
  }
}

export function useAdminInbox() {
  const userStore = useUserStore()
  const userId = computed(() => userStore.userInfo?.id)

  const unreadNotificationCount = computed(() =>
    notifications.value.filter(item => item.id && !isNotificationRead(item.id)).length,
  )

  const unreadMessageCount = computed(() =>
    messages.value.filter(item => item.id && !isMessageRead(item.id)).length,
  )

  function syncReadStorage() {
    notificationReadIds.value = loadReadIds('notifications', userId.value)
    messageReadIds.value = loadReadIds('messages', userId.value)
    syncServerReadState()
    saveReadIds('notifications', userId.value, notificationReadIds.value)
    saveReadIds('messages', userId.value, messageReadIds.value)
  }

  function isNotificationRead(id: string) {
    const item = notifications.value.find(row => row.id === id)
    if (item?.read) return true
    return notificationReadIds.value.has(id)
  }

  function isMessageRead(id: string) {
    const item = messages.value.find(row => row.id === id)
    if (item?.read) return true
    return messageReadIds.value.has(id)
  }

  async function markNotificationRead(id: string) {
    if (!id) return
    try {
      if (isDbNotificationId(id)) {
        await adminApi.markNotificationRead(id)
      }
    } catch {
      // 计算类通知仅本地已读
    }
    notificationReadIds.value = new Set([...notificationReadIds.value, id])
    saveReadIds('notifications', userId.value, notificationReadIds.value)
    const item = notifications.value.find(row => row.id === id)
    if (item) item.read = true
  }

  async function markMessageRead(id: string) {
    if (!id) return
    try {
      if (isDbMessageId(id)) {
        await adminApi.markMessageRead(id)
      }
    } catch {
      // 计算类消息仅本地已读
    }
    messageReadIds.value = new Set([...messageReadIds.value, id])
    saveReadIds('messages', userId.value, messageReadIds.value)
    const item = messages.value.find(row => row.id === id)
    if (item) item.read = true
  }

  async function markAllNotificationsRead() {
    try {
      await adminApi.markAllNotificationsRead()
    } catch {
      // ignore
    }
    notificationReadIds.value = new Set(
      notifications.value.map(item => item.id).filter(Boolean) as string[],
    )
    notifications.value.forEach(item => { item.read = true })
    saveReadIds('notifications', userId.value, notificationReadIds.value)
  }

  async function markAllMessagesRead() {
    try {
      await adminApi.markAllMessagesRead()
    } catch {
      // ignore
    }
    messageReadIds.value = new Set(
      messages.value.map(item => item.id).filter(Boolean) as string[],
    )
    messages.value.forEach(item => { item.read = true })
    saveReadIds('messages', userId.value, messageReadIds.value)
  }

  async function refresh() {
    if (userStore.role !== 'admin') return
    loading.value = true
    try {
      const [noticeRes, messageRes] = await Promise.all([
        adminApi.getNotificationSummary(),
        adminApi.getMessageSummary(),
      ])
      notifications.value = noticeRes.data?.items || []
      messages.value = messageRes.data?.items || []
      syncReadStorage()
    } catch {
      notifications.value = []
      messages.value = []
    } finally {
      loading.value = false
    }
  }

  function initInbox() {
    mountedCount += 1
    if (mountedCount === 1) {
      syncReadStorage()
      refresh()
      restartPolling(refresh)
      connectSse(refresh, userStore.role === 'admin')
    }
  }

  function destroyInbox() {
    mountedCount -= 1
    if (mountedCount <= 0) {
      mountedCount = 0
      stopPolling()
      stopSse()
      if (refreshDebounceTimer) {
        clearTimeout(refreshDebounceTimer)
        refreshDebounceTimer = undefined
      }
    }
  }

  onMounted(initInbox)
  onUnmounted(destroyInbox)

  return {
    loading,
    notifications,
    messages,
    unreadNotificationCount,
    unreadMessageCount,
    sseConnected,
    isNotificationRead,
    isMessageRead,
    markNotificationRead,
    markMessageRead,
    markAllNotificationsRead,
    markAllMessagesRead,
    refresh,
    syncReadStorage,
    isDbNotificationId,
    isDbMessageId,
  }
}
