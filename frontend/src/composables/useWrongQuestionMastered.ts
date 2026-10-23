import { ref, watch } from 'vue'
import { useUserStore } from '@/stores'
import {
  getWrongQuestionMastered,
  markWrongQuestionMastered,
  resetWrongQuestionMastered,
} from '@/api/quiz'

const HIDE_MASTERED_KEY = 'wrongQuestions_hideMastered'

type MasteredMap = Record<string, string[]>

const masteredMap = ref<MasteredMap>({})
const hideMastered = ref(localStorage.getItem(HIDE_MASTERED_KEY) === '1')
const synced = ref(false)
let currentUserId = ''
let syncPromise: Promise<void> | null = null

function readLocalMastered(userId: string): MasteredMap {
  try {
    const raw = localStorage.getItem(`wrongQuestions_mastered_${userId}`)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function writeLocalMastered(userId: string, data: MasteredMap) {
  localStorage.setItem(`wrongQuestions_mastered_${userId}`, JSON.stringify(data))
}

async function syncFromServer(userId: string) {
  if (!userId || userId === 'guest') {
    masteredMap.value = readLocalMastered(userId)
    synced.value = true
    return
  }
  try {
    const res = await getWrongQuestionMastered()
    masteredMap.value = res.data?.chapters || {}
    writeLocalMastered(userId, masteredMap.value)
  } catch {
    masteredMap.value = readLocalMastered(userId)
  } finally {
    synced.value = true
  }
}

function ensureLoaded(userId: string) {
  if (userId !== currentUserId) {
    currentUserId = userId
    synced.value = false
    syncPromise = syncFromServer(userId)
  } else if (!synced.value && !syncPromise) {
    syncPromise = syncFromServer(userId)
  }
}

export function useWrongQuestionMastered() {
  const userStore = useUserStore()
  const userId = () => userStore.userInfo?.id || 'guest'

  ensureLoaded(userId())

  watch(
    () => userStore.userInfo?.id,
    (id) => {
      if (id) ensureLoaded(id)
    },
  )

  watch(hideMastered, (val) => {
    localStorage.setItem(HIDE_MASTERED_KEY, val ? '1' : '0')
  })

  function isMastered(chapterId: string, questionId: string) {
    return masteredMap.value[chapterId]?.includes(questionId) ?? false
  }

  async function markMastered(chapterId: string, questionId: string) {
    const uid = userId()
    const next = { ...masteredMap.value }
    const list = new Set(next[chapterId] || [])
    list.add(questionId)
    next[chapterId] = [...list]
    masteredMap.value = next
    writeLocalMastered(uid, next)
    try {
      await markWrongQuestionMastered({ chapterId, questionId })
    } catch {
      /* 本地已更新，服务端失败时下次同步可重试 */
    }
  }

  async function clearChapter(chapterId: string, questionIds: string[]) {
    const uid = userId()
    const next = { ...masteredMap.value }
    next[chapterId] = [...new Set([...(next[chapterId] || []), ...questionIds])]
    masteredMap.value = next
    writeLocalMastered(uid, next)
    try {
      await markWrongQuestionMastered({ chapterId, questionIds })
    } catch {
      /* ignore */
    }
  }

  function activeCount(chapterId: string, total: number) {
    const mastered = masteredMap.value[chapterId]?.length ?? 0
    return Math.max(0, total - mastered)
  }

  function totalMasteredCount() {
    return Object.values(masteredMap.value).reduce((sum, ids) => sum + ids.length, 0)
  }

  function hasMasteredRecords() {
    return totalMasteredCount() > 0
  }

  async function resetAllMastered() {
    const uid = userId()
    masteredMap.value = {}
    writeLocalMastered(uid, {})
    try {
      await resetWrongQuestionMastered()
    } catch {
      /* ignore */
    }
  }

  async function refreshMastered() {
    await syncFromServer(userId())
  }

  return {
    masteredMap,
    hideMastered,
    synced,
    isMastered,
    markMastered,
    clearChapter,
    activeCount,
    totalMasteredCount,
    hasMasteredRecords,
    resetAllMastered,
    refreshMastered,
  }
}

export function isAnswerSelected(optionId: string, answer?: string) {
  if (!answer) return false
  return answer.split(',').map(s => s.trim()).includes(optionId)
}
