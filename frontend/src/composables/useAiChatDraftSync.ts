import type { AiChatMessage } from './useAiChatSession'
import { aiApi } from '@/api/ai'

let saveTimer: ReturnType<typeof setTimeout> | null = null

export async function loadAiChatDraftFromServer(userId?: string): Promise<AiChatMessage[] | null> {
  if (!userId) return null
  try {
    const res = await aiApi.getDraft()
    const raw = res.data?.messages
    if (!raw || raw === '[]') return null
    const parsed = JSON.parse(raw) as AiChatMessage[]
    return Array.isArray(parsed) && parsed.length ? parsed : null
  } catch {
    return null
  }
}

export function scheduleAiChatDraftSync(messages: AiChatMessage[], userId?: string) {
  if (!userId) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(async () => {
    try {
      await aiApi.saveDraft(JSON.stringify(messages))
    } catch {
      // 离线或网络异常时忽略
    }
  }, 800)
}
