import { ref, watch, onMounted, onUnmounted, type Ref } from 'vue'

const HEARTBEAT_INTERVAL_MS = 30_000
const MIN_FLUSH_SECONDS = 5

export interface HeartbeatPayload {
  courseId: string
  chapterId: string
  elapsedSeconds: number
  progress?: number
}

export interface HeartbeatResult {
  success: boolean
  studySeconds?: number
  progress?: number
}

async function postHeartbeat(
  payload: HeartbeatPayload,
  keepalive = false,
): Promise<HeartbeatResult | null> {
  const token = localStorage.getItem('token')
  if (!token) return null

  try {
    const res = await fetch('/api/progress/heartbeat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(payload),
      keepalive,
    })
    if (!res.ok) return null
    const json = await res.json()
    if (json.code !== 200) return null
    return json.data as HeartbeatResult
  } catch {
    return null
  }
}

export interface LearningHeartbeatOptions {
  courseId: Ref<string>
  chapterId: Ref<string>
  /** 当前阅读进度 0-100 */
  progress: Ref<number>
  /** 是否启用心跳，默认 true */
  enabled?: Ref<boolean>
  /** 心跳成功后的回调，可用于同步服务端进度 */
  onSynced?: (result: HeartbeatResult) => void
}

/**
 * 章节学习页自动心跳：每 30s 上报学习时长与阅读进度。
 * 页面隐藏时暂停计时，离开页面时 flush 剩余时长。
 */
export function useLearningHeartbeat(options: LearningHeartbeatOptions) {
  const { courseId, chapterId, progress, enabled, onSynced } = options
  const active = enabled ?? ref(true)

  let timer: ReturnType<typeof setInterval> | null = null
  let segmentStart = 0
  let sending = false

  function isContextReady() {
    return active.value && !!courseId.value && !!chapterId.value && !document.hidden
  }

  function resetSegment() {
    segmentStart = Date.now()
  }

  function elapsedSinceSegment(): number {
    if (!segmentStart) return 0
    return Math.floor((Date.now() - segmentStart) / 1000)
  }

  async function sendHeartbeat(elapsedSeconds: number, keepalive = false) {
    if (!courseId.value || !chapterId.value || elapsedSeconds < 1 || sending) return
    sending = true
    try {
      const result = await postHeartbeat(
        {
          courseId: courseId.value,
          chapterId: chapterId.value,
          elapsedSeconds: Math.min(elapsedSeconds, 120),
          progress: progress.value,
        },
        keepalive,
      )
      if (result?.success) {
        onSynced?.(result)
      }
    } finally {
      sending = false
    }
  }

  function tick() {
    if (!isContextReady()) return
    const elapsed = elapsedSinceSegment()
    if (elapsed >= 30) {
      void sendHeartbeat(30)
      resetSegment()
    }
  }

  function flush(keepalive = false) {
    const elapsed = elapsedSinceSegment()
    if (elapsed >= MIN_FLUSH_SECONDS && courseId.value && chapterId.value) {
      void sendHeartbeat(elapsed, keepalive)
    }
    resetSegment()
  }

  function start() {
    stop()
    resetSegment()
    timer = setInterval(tick, HEARTBEAT_INTERVAL_MS)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function handleVisibilityChange() {
    if (document.hidden) {
      flush()
    } else {
      resetSegment()
    }
  }

  function handlePageHide() {
    const elapsed = elapsedSinceSegment()
    if (elapsed >= MIN_FLUSH_SECONDS) {
      void sendHeartbeat(elapsed, true)
    }
    resetSegment()
  }

  onMounted(() => {
    start()
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('pagehide', handlePageHide)
  })

  onUnmounted(() => {
    flush()
    stop()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener('pagehide', handlePageHide)
  })

  watch([courseId, chapterId], () => {
    flush()
    resetSegment()
  })

  watch(active, (on) => {
    if (on) {
      resetSegment()
      if (!timer) start()
    } else {
      flush()
      stop()
    }
  })

  return { flush }
}
