export type SseEventHandler = (event: string, data: unknown) => void

function parseSseChunk(raw: string, onEvent: SseEventHandler) {
  let eventName = 'message'
  const dataLines: string[] = []

  for (const rawLine of raw.split('\n')) {
    const line = rawLine.replace(/\r$/, '')
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }

  if (!dataLines.length) return

  const dataStr = dataLines.join('\n')
  let data: unknown = dataStr
  try {
    data = JSON.parse(dataStr)
  } catch {
    // keep raw string
  }
  onEvent(eventName, data)
}

/** 使用 fetch 读取 SSE（支持 Authorization 头）。连接断开时抛出或正常结束。 */
export async function connectSseStream(
  url: string,
  onEvent: SseEventHandler,
  signal: AbortSignal,
): Promise<void> {
  const token = localStorage.getItem('token')
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    signal,
  })

  if (!response.ok || !response.body) {
    throw new Error(`SSE connect failed: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')

      let boundary = buffer.indexOf('\n\n')
      while (boundary !== -1) {
        const rawEvent = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        parseSseChunk(rawEvent, onEvent)
        boundary = buffer.indexOf('\n\n')
      }
    }

    buffer += decoder.decode(undefined, { stream: false })
    if (buffer.trim()) {
      parseSseChunk(buffer, onEvent)
    }
  } finally {
    try {
      await reader.cancel()
    } catch {
      // ignore
    }
  }
}
