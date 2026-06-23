<template>
  <div class="sl-page ai-chat">
    <el-card class="chat-card">
      <template #header>
        <div class="chat-header">
          <div class="flex items-center gap-2">
            <el-icon :size="24" color="#8b5cf6"><ChatDotRound /></el-icon>
            <span class="font-bold text-lg">AI安全问答</span>
          </div>
          <div class="chat-header-actions">
            <el-button
              v-if="hasHistory"
              text
              type="danger"
              size="small"
              @click="handleClearChat"
            >
              清空对话
            </el-button>
            <el-button text size="small" @click="goHistoryPage">
              全部历史
            </el-button>
          </div>
        </div>
      </template>

      <div class="chat-body">
        <!-- 聊天消息区域 -->
        <div ref="chatContainer" class="chat-messages">
          <div
            v-for="message in messages"
            :key="message.id"
            :id="'msg-' + message.id"
            class="message-item"
            :class="[
              message.role === 'user' ? 'message-user' : 'message-ai',
              { 'message-highlight': highlightId === message.id },
            ]"
          >
            <div class="message-avatar">
              <el-avatar v-if="message.role === 'user'" :size="36">
                <el-icon><User /></el-icon>
              </el-avatar>
              <el-avatar v-else :size="36" style="background: #8b5cf6">
                <el-icon><ChatDotRound /></el-icon>
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-text">
                {{ message.content }}
                <span
                  v-if="message.role === 'ai' && streamingId === message.id"
                  class="stream-cursor"
                />
              </div>
              <div v-if="message.sources?.length" class="message-sources">
                <div class="sources-title">参考来源:</div>
                <div v-for="source in message.sources" :key="source.id" class="source-item">
                  <el-icon><Document /></el-icon>
                  <span>{{ source.title }}</span>
                  <el-tag size="small">相关度: {{ formatRelevance(source.relevance) }}%</el-tag>
                </div>
              </div>
              <div class="message-time">{{ message.time }}</div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading && streamingId === null" class="message-item message-ai">
            <div class="message-avatar">
              <el-avatar :size="36" style="background: #8b5cf6">
                <el-icon><ChatDotRound /></el-icon>
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 最近提问 -->
        <aside v-if="recentQuestions.length" class="recent-panel">
          <div class="recent-title">最近提问</div>
          <div class="recent-list">
            <button
              v-for="item in recentQuestions"
              :key="item.id"
              type="button"
              class="recent-item"
              :class="{ active: highlightId === item.id }"
              @click="scrollToMessage(item.id)"
            >
              {{ truncateQuestion(item.content) }}
            </button>
          </div>
        </aside>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input">
        <div v-if="recommendQuestions.length" class="recommend-bar">
          <span class="recommend-label">推荐问题</span>
          <div class="recommend-questions">
            <el-button
              v-for="question in recommendQuestions"
              :key="question"
              text
              size="small"
              @click="inputText = question"
            >
              {{ question }}
            </el-button>
          </div>
        </div>
        <el-input
          v-model="inputText"
          placeholder="请输入您的安全问题..."
          :rows="2"
          type="textarea"
          resize="none"
          @keyup.enter.ctrl="sendMessage"
        />
        <div class="input-footer">
          <span class="text-gray-400 text-sm">按 Ctrl + Enter 发送</span>
          <el-button type="primary" :loading="loading" @click="sendMessage">
            发送
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onActivated, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, User, Document } from '@element-plus/icons-vue'
import { systemConfigApi } from '@/api/systemConfig'
import { aiApi } from '@/api/ai'
import { useAppBase } from '@/composables/useAppBase'
import { useUserStore } from '@/stores'
import {
  AI_CHAT_WELCOME,
  loadAiChatSession,
  saveAiChatSession,
  clearAiChatSession,
  hasUserMessages,
  historyRecordsToMessages,
  truncateQuestion,
  type AiChatMessage,
} from '@/composables/useAiChatSession'

const router = useRouter()
const { p } = useAppBase()
const userStore = useUserStore()

const chatContainer = ref<HTMLElement>()
const inputText = ref('')
const loading = ref(false)
const streamingId = ref<string | null>(null)
const highlightId = ref<string | null>(null)
const sessionReady = ref(false)

const messages = ref<AiChatMessage[]>([{ ...AI_CHAT_WELCOME, time: new Date().toLocaleTimeString() }])

const recommendQuestions = ref<string[]>([
  '储能柜冒烟如何处理？',
  '热失控前兆有哪些？',
  '锂电池储能消防系统有哪些类型？',
  'BMS系统的主要功能是什么？',
  '储能电站安全检查的重点是什么？',
])

const userId = computed(() => userStore.userInfo?.id)

const hasHistory = computed(() => hasUserMessages(messages.value))

const recentQuestions = computed(() =>
  messages.value
    .filter(m => m.role === 'user' && m.content.trim())
    .slice(-10)
    .reverse(),
)

function formatRelevance(relevance: number) {
  const ratio = relevance > 1 ? relevance / 100 : relevance
  return Math.min(100, Math.round(Math.max(0, ratio) * 100))
}

function persistSession() {
  if (!sessionReady.value) return
  saveAiChatSession(messages.value, userId.value)
}

async function initChatSession() {
  const cached = loadAiChatSession(userId.value)
  if (cached && hasUserMessages(cached)) {
    messages.value = cached
    sessionReady.value = true
    return
  }

  try {
    const res = await aiApi.getHistory({ page: 1, pageSize: 10 })
    const items = res.data?.items || []
    if (items.length) {
      messages.value = historyRecordsToMessages(items)
      saveAiChatSession(messages.value, userId.value)
    }
  } catch {
    // 保留欢迎语
  } finally {
    sessionReady.value = true
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({
    id: Date.now().toString(),
    role: 'user',
    content: text,
    time: new Date().toLocaleTimeString(),
  })
  persistSession()

  inputText.value = ''
  loading.value = true

  const aiMsgId = (Date.now() + 1).toString()
  const aiMsgIndex = messages.value.length
  messages.value.push({
    id: aiMsgId,
    role: 'ai',
    content: '',
    sources: [],
    time: new Date().toLocaleTimeString(),
  })
  streamingId.value = aiMsgId

  await nextTick()
  scrollToBottom()

  try {
    await streamAnswer(text, aiMsgIndex)
  } catch {
    if (!messages.value[aiMsgIndex]?.content) {
      messages.value[aiMsgIndex] = {
        ...messages.value[aiMsgIndex],
        content: '抱歉，暂时无法回答您的问题，请稍后重试。',
      }
    }
  } finally {
    finishSending(aiMsgIndex)
  }
}

function finishSending(aiMsgIndex: number) {
  loading.value = false
  streamingId.value = null
  const msg = messages.value[aiMsgIndex]
  if (msg && !msg.content.trim()) {
    messages.value[aiMsgIndex] = {
      ...msg,
      content: '抱歉，未能获取到有效回答，请稍后重试。',
    }
  }
  persistSession()
  nextTick(() => scrollToBottom())
}

let activeStreamAbort: AbortController | null = null

async function streamAnswer(question: string, aiMsgIndex: number) {
  activeStreamAbort?.abort()
  const controller = new AbortController()
  activeStreamAbort = controller
  const timeoutId = window.setTimeout(() => controller.abort(), 120_000)

  const token = localStorage.getItem('token')
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
  let streamCompleted = false

  try {
    const resp = await fetch('/api/ai/ask/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ question }),
      signal: controller.signal,
    })

    if (!resp.ok || !resp.body) {
      throw new Error('stream request failed')
    }

    reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (!streamCompleted) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')

      let boundary = buffer.indexOf('\n\n')
      while (boundary !== -1) {
        const rawEvent = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        if (parseSseEvent(rawEvent, aiMsgIndex)) {
          streamCompleted = true
          break
        }
        boundary = buffer.indexOf('\n\n')
      }
    }

    if (!streamCompleted && buffer.trim()) {
      streamCompleted = parseSseEvent(buffer, aiMsgIndex)
    }

    buffer += decoder.decode(undefined, { stream: false })
    if (!streamCompleted && buffer.trim()) {
      parseSseEvent(buffer, aiMsgIndex)
    }
  } finally {
    clearTimeout(timeoutId)
    if (activeStreamAbort === controller) {
      activeStreamAbort = null
    }
    try {
      await reader?.cancel()
    } catch {
      // ignore
    }
  }
}

/** @returns true 表示流式响应已结束（done / error） */
function parseSseEvent(rawEvent: string, aiMsgIndex: number): boolean {
  let eventName = 'message'
  const dataLines: string[] = []

  for (const rawLine of rawEvent.split('\n')) {
    const line = rawLine.replace(/\r$/, '')
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  if (!dataLines.length) return false
  return handleSseEvent(eventName, dataLines.join('\n'), aiMsgIndex)
}

function handleSseEvent(event: string, data: string, aiMsgIndex: number): boolean {
  const current = messages.value[aiMsgIndex]
  if (!current) return event === 'done' || event === 'error'

  try {
    if (event === 'sources') {
      messages.value[aiMsgIndex] = {
        ...current,
        sources: JSON.parse(data),
      }
    } else if (event === 'delta') {
      const payload = JSON.parse(data)
      const text = payload?.text ?? ''
      if (text) {
        const latest = messages.value[aiMsgIndex]
        messages.value[aiMsgIndex] = {
          ...latest,
          content: latest.content + text,
        }
      }
    } else if (event === 'replace') {
      const payload = JSON.parse(data)
      messages.value[aiMsgIndex] = {
        ...current,
        content: payload?.text ?? current.content,
      }
    } else if (event === 'error') {
      const payload = JSON.parse(data)
      messages.value[aiMsgIndex] = {
        ...current,
        content: payload?.message || 'AI 服务暂时不可用。',
      }
      scrollToBottom()
      return true
    } else if (event === 'done') {
      scrollToBottom()
      return true
    }
    scrollToBottom()
  } catch (e) {
    console.warn('SSE parse failed:', event, e)
  }
  return false
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

function scrollToMessage(messageId: string) {
  highlightId.value = messageId
  nextTick(() => {
    const el = document.getElementById(`msg-${messageId}`)
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    setTimeout(() => {
      if (highlightId.value === messageId) highlightId.value = null
    }, 2000)
  })
}

async function handleClearChat() {
  try {
    await ElMessageBox.confirm('确定清空当前对话吗？清空后仍可在「全部历史」查看已保存记录。', '清空对话', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    messages.value = [{ ...AI_CHAT_WELCOME, time: new Date().toLocaleTimeString() }]
    clearAiChatSession(userId.value)
    ElMessage.success('对话已清空')
  } catch {
    // 取消
  }
}

function goHistoryPage() {
  router.push(p('/ai/history'))
}

watch(messages, () => persistSession(), { deep: true })

onBeforeUnmount(() => {
  activeStreamAbort?.abort()
  loading.value = false
  streamingId.value = null
})

onActivated(() => {
  // 从其他页面返回时，避免 loading 状态残留
  if (loading.value && streamingId.value === null) {
    loading.value = false
  }
})

onMounted(async () => {
  if (!userStore.userInfo && userStore.isLoggedIn) {
    try {
      await userStore.getUserInfo()
    } catch {
      // 无用户信息时仍用 guest 会话键
    }
  }
  await initChatSession()
  await loadRecommendQuestions()
  nextTick(() => scrollToBottom())
})

async function loadRecommendQuestions() {
  try {
    const res = await systemConfigApi.getPublic()
    const list = res.data?.['ai.relatedQuestions']
    if (Array.isArray(list) && list.length) {
      recommendQuestions.value = list as string[]
    }
  } catch {
    // 接口失败保留兜底默认
  }
}
</script>

<style scoped>
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chat-card {
  height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  padding: 12px;
}

.chat-body {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  min-width: 0;
}

.recent-panel {
  width: 160px;
  flex-shrink: 0;
  border-left: 1px solid #e5e7eb;
  padding: 12px;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.recent-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
}

.recent-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.recent-item {
  text-align: left;
  border: none;
  background: #fff;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.4;
  color: #4b5563;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  border: 1px solid #e5e7eb;
}

.recent-item:hover,
.recent-item.active {
  background: #ede9fe;
  color: #6d28d9;
  border-color: #c4b5fd;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  scroll-margin-top: 16px;
  transition: background 0.3s;
  border-radius: 8px;
}

.message-highlight {
  background: rgba(139, 92, 246, 0.08);
}

.message-user {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 85%;
}

.message-user .message-content {
  max-width: 70%;
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

.stream-cursor {
  display: inline-block;
  width: 7px;
  height: 14px;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: #8b5cf6;
  animation: blink 1s steps(2) infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  50.01%, 100% { opacity: 0; }
}

.message-ai .message-text {
  background: #f3f4f6;
  color: #1f2937;
}

.message-user .message-text {
  background: #3b82f6;
  color: white;
}

.message-sources {
  margin-top: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  font-size: 13px;
}

.sources-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: #374151;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: #6b7280;
}

.message-time {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f3f4f6;
  border-radius: 12px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ca3af;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1); }
}

.chat-input {
  border-top: 1px solid #e5e7eb;
  padding-top: 12px;
  flex-shrink: 0;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.recommend-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  overflow: hidden;
}

.recommend-label {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  flex-shrink: 0;
}

.recommend-questions {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  flex: 1;
}

.recommend-questions::-webkit-scrollbar {
  display: none;
}

@media (max-width: 768px) {
  .recent-panel {
    display: none;
  }
}
</style>
