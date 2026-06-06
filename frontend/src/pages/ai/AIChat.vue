<template>
  <div class="ai-chat">
    <el-card class="chat-card">
      <template #header>
        <div class="flex items-center gap-2">
          <el-icon :size="24" color="#8b5cf6"><ChatDotRound /></el-icon>
          <span class="font-bold text-lg">AI安全问答</span>
        </div>
      </template>

      <!-- 聊天消息区域 -->
      <div ref="chatContainer" class="chat-messages">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-item"
          :class="message.role === 'user' ? 'message-user' : 'message-ai'"
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
            <div class="message-text" v-html="message.content" />
            <div v-if="message.sources?.length" class="message-sources">
              <div class="sources-title">参考来源:</div>
              <div v-for="source in message.sources" :key="source.id" class="source-item">
                <el-icon><Document /></el-icon>
                <span>{{ source.title }}</span>
                <el-tag size="small">相关度: {{ Math.round(source.relevance * 100) }}%</el-tag>
              </div>
            </div>
            <div class="message-time">{{ message.time }}</div>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="message-item message-ai">
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

      <!-- 输入区域 -->
      <div class="chat-input">
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

    <!-- 推荐问题 -->
    <el-card class="mt-4">
      <template #header>
        <span class="font-bold">推荐问题</span>
      </template>
      <div class="recommend-questions">
        <el-button
          v-for="question in recommendQuestions"
          :key="question"
          text
          @click="inputText = question"
        >
          {{ question }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ChatDotRound, User, Document } from '@element-plus/icons-vue'
import request from '@/api/request'

interface Message {
  id: string
  role: 'user' | 'ai'
  content: string
  sources?: { id: string; title: string; relevance: number }[]
  time: string
}

const chatContainer = ref<HTMLElement>()
const inputText = ref('')
const loading = ref(false)

const messages = ref<Message[]>([
  {
    id: '1',
    role: 'ai',
    content: '您好！我是储能安全AI助手，可以回答关于储能电站安全、热失控、消防安全等方面的问题。请问有什么可以帮助您的？',
    time: '10:00',
  },
])

const recommendQuestions = [
  '储能柜冒烟如何处理？',
  '热失控前兆有哪些？',
  '锂电池储能消防系统有哪些类型？',
  'BMS系统的主要功能是什么？',
  '储能电站安全检查的重点是什么？',
]

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // 添加用户消息
  messages.value.push({
    id: Date.now().toString(),
    role: 'user',
    content: text,
    time: new Date().toLocaleTimeString(),
  })

  inputText.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  // 调用AI API
  try {
    const res = await request.post('/ai/ask', { question: text })
    messages.value.push({
      id: (Date.now() + 1).toString(),
      role: 'ai',
      content: res.data.answer,
      sources: res.data.sources,
      time: new Date().toLocaleTimeString(),
    })
  } catch {
    messages.value.push({
      id: (Date.now() + 1).toString(),
      role: 'ai',
      content: '抱歉，暂时无法回答您的问题，请稍后重试。',
      time: new Date().toLocaleTimeString(),
    })
  }

  loading.value = false
  nextTick(() => scrollToBottom())
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.ai-chat {
  width: 100%;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.chat-card {
  height: calc(100vh - 300px);
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-user {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 70%;
}

.message-user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
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
  padding-top: 16px;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.recommend-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
