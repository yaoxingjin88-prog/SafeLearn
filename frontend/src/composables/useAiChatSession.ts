import type { QARecord } from '@/types'

export interface AiChatMessage {
  id: string
  role: 'user' | 'ai'
  content: string
  sources?: { id: string; title: string; relevance: number }[]
  time: string
}

export const AI_CHAT_WELCOME: AiChatMessage = {
  id: 'welcome',
  role: 'ai',
  content: '您好！我是储能安全AI助手，可以回答关于储能电站安全、热失控、消防安全等方面的问题。请问有什么可以帮助您的？',
  time: '',
}

const SESSION_PREFIX = 'safelearn:ai-chat:'

function sessionKey(userId?: string) {
  return `${SESSION_PREFIX}${userId || 'guest'}`
}

function formatTime(iso?: string) {
  if (!iso) return new Date().toLocaleTimeString()
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso.replace('T', ' ').slice(0, 16)
  return d.toLocaleTimeString()
}

export function loadAiChatSession(userId?: string): AiChatMessage[] | null {
  try {
    const raw = sessionStorage.getItem(sessionKey(userId))
    if (!raw) return null
    const parsed = JSON.parse(raw) as AiChatMessage[]
    return Array.isArray(parsed) && parsed.length ? parsed : null
  } catch {
    return null
  }
}

export function saveAiChatSession(messages: AiChatMessage[], userId?: string) {
  try {
    sessionStorage.setItem(sessionKey(userId), JSON.stringify(messages))
  } catch {
    // sessionStorage 满或不可用时忽略
  }
}

export function clearAiChatSession(userId?: string) {
  sessionStorage.removeItem(sessionKey(userId))
}

export function hasUserMessages(messages: AiChatMessage[]) {
  return messages.some(m => m.role === 'user' && m.content.trim())
}

/** 将后端历史记录转为聊天消息（时间正序） */
export function historyRecordsToMessages(records: QARecord[]): AiChatMessage[] {
  if (!records.length) return [AI_CHAT_WELCOME]

  const sorted = [...records].sort((a, b) => {
    const ta = new Date(a.createdAt).getTime()
    const tb = new Date(b.createdAt).getTime()
    return (Number.isNaN(ta) ? 0 : ta) - (Number.isNaN(tb) ? 0 : tb)
  })

  const messages: AiChatMessage[] = [AI_CHAT_WELCOME]
  for (const record of sorted) {
    messages.push({
      id: `hist-q-${record.id}`,
      role: 'user',
      content: record.question,
      time: formatTime(record.createdAt),
    })
    messages.push({
      id: `hist-a-${record.id}`,
      role: 'ai',
      content: record.answer,
      time: formatTime(record.createdAt),
    })
  }
  return messages
}

export function truncateQuestion(text: string, max = 28) {
  const t = text.trim()
  if (t.length <= max) return t
  return `${t.slice(0, max)}…`
}
