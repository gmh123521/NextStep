export interface RequestErrorLike {
  code?: unknown
  msg?: unknown
  message?: unknown
  name?: unknown
  response?: { data?: { msg?: unknown; message?: unknown } }
}

function text(value: unknown): string | null {
  if (typeof value !== 'string') return null
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

/** 将后端业务错误、Axios 错误和浏览器网络错误统一转换为用户可读提示。 */
export function formatRequestError(error: unknown, fallback = '请求失败'): string {
  if (typeof error === 'string') return text(error) || fallback
  if (!error || typeof error !== 'object') return fallback

  const value = error as RequestErrorLike
  const responseData = value.response?.data
  const serverMessage = text(value.msg) || text(responseData?.msg) || text(responseData?.message)
  if (serverMessage) return serverMessage

  const code = text(value.code)
  const message = text(value.message)
  if (code === 'ECONNABORTED' || message?.toLowerCase().includes('timeout')) {
    return '请求超时，请稍后重试'
  }
  if (message?.toLowerCase() === 'network error') {
    return '网络连接失败，请检查网络后重试'
  }
  if (message) return message
  return fallback
}

/** 读取 fetch 非 2xx 响应中的业务错误；响应不是 JSON 时保留 HTTP 状态。 */
export async function formatResponseError(response: Response, fallback = '请求失败'): Promise<string> {
  try {
    const body = await response.clone().json() as unknown
    const message = formatRequestError(body, '')
    if (message) return message
  } catch {
    // 二进制响应或非 JSON 响应走状态码兜底
  }
  return response.status ? `${fallback}（HTTP ${response.status}）` : fallback
}
