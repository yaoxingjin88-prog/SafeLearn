import { ref, watch } from 'vue'
import { useUserStore } from '@/stores'

const HIDE_MASTERED_KEY = 'wrongQuestions_hideMastered'

type MasteredMap = Record<string, string[]>

const masteredMap = ref<MasteredMap>({})
const hideMastered = ref(localStorage.getItem(HIDE_MASTERED_KEY) === '1')
let currentUserId = ''

function storageKey(userId: string) {
  return `wrongQuestions_mastered_${userId}`
}

function readMastered(userId: string): MasteredMap {
  try {
    const raw = localStorage.getItem(storageKey(userId))
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function writeMastered(userId: string, data: MasteredMap) {
  localStorage.setItem(storageKey(userId), JSON.stringify(data))
}

function ensureLoaded(userId: string) {
  if (userId !== currentUserId) {
    currentUserId = userId
    masteredMap.value = readMastered(userId)
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

  function markMastered(chapterId: string, questionId: string) {
    const uid = userId()
    const next = { ...masteredMap.value }
    const list = new Set(next[chapterId] || [])
    list.add(questionId)
    next[chapterId] = [...list]
    masteredMap.value = next
    writeMastered(uid, next)
  }

  function clearChapter(chapterId: string, questionIds: string[]) {
    const uid = userId()
    const next = { ...masteredMap.value }
    next[chapterId] = [...new Set([...(next[chapterId] || []), ...questionIds])]
    masteredMap.value = next
    writeMastered(uid, next)
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

  function resetAllMastered() {
    const uid = userId()
    masteredMap.value = {}
    writeMastered(uid, {})
  }

  return {
    masteredMap,
    hideMastered,
    isMastered,
    markMastered,
    clearChapter,
    activeCount,
    totalMasteredCount,
    hasMasteredRecords,
    resetAllMastered,
  }
}

export function isAnswerSelected(optionId: string, answer?: string) {
  if (!answer) return false
  return answer.split(',').map(s => s.trim()).includes(optionId)
}
