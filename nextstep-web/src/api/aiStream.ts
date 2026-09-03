import { getToken } from './http'
import { formatResponseError } from '@/utils/error'

export interface ExplainController {
  abort(): void
}

/**
 * SSE 流式 AI 解读
 * 浏览器原生 EventSource 不支持自定义 headers，所以用 fetch + ReadableStream
 */
export function streamExplain(
  onChunk: (chunk: string) => void,
  onDone: () => void,
  onError: (err: Error) => void
): ExplainController {
  const ctl = new AbortController()
  const token = getToken()

  // 兜底超时：90 秒还没完就主动断开（正常 250 字内 < 5 秒）
  const timer = setTimeout(() => {
    console.warn('[streamExplain] timeout 90s, aborting')
    ctl.abort()
  }, 90_000)

  let finished = false
  const finish = () => {
    if (finished) return
    finished = true
    clearTimeout(timer)
    onDone()
  }

  fetch('/api/ai/explain', {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    signal: ctl.signal
  })
    .then(async resp => {
      if (!resp.ok) throw new Error(await formatResponseError(resp, 'AI 解读请求失败'))
      if (!resp.body) throw new Error('No response body')

      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        let idx
        while ((idx = buffer.indexOf('\n\n')) >= 0) {
          const eventBlock = buffer.slice(0, idx)
          buffer = buffer.slice(idx + 2)
          const stop = parseAndDispatch(eventBlock, onChunk)
          if (stop) {
            try { await reader.cancel() } catch {}
            finish()
            return
          }
        }
      }
      finish()
    })
    .catch(err => {
      clearTimeout(timer)
      if (err.name === 'AbortError') {
        finish()
        return
      }
      finished = true
      onError(err)
    })

  return {
    abort: () => {
      clearTimeout(timer)
      ctl.abort()
    }
  }
}

/** 返回 true 表示遇到 done 事件，外层应停止读取 */
function parseAndDispatch(block: string, onChunk: (s: string) => void): boolean {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5))
  }
  if (!dataLines.length) return false
  const data = dataLines.join('\n')
  if (event === 'done' || data === '[DONE]') return true
  onChunk(data)
  return false
}
