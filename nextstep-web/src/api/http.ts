import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { formatRequestError } from '@/utils/error'

export interface R<T> {
  code: number
  msg: string
  data: T
  timestamp: number
}

const TOKEN_KEY = 'nextstep_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(t: string) {
  localStorage.setItem(TOKEN_KEY, t)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use(cfg => {
  const t = getToken()
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

http.interceptors.response.use(
  resp => {
    const body = resp.data as R<unknown>
    if (body.code === 200) return body as any
    if (body.code === 4010) {
      clearToken()
      ElMessage.warning('登录已过期，请重新登录')
      if (location.pathname !== '/login') location.href = '/login'
      return Promise.reject(body)
    }
    return Promise.reject(body)
  },
  err => {
    return Promise.reject(new Error(formatRequestError(err, '网络请求失败')))
  }
)

export async function request<T>(cfg: AxiosRequestConfig): Promise<T> {
  const body = await http.request<unknown, R<T>>(cfg)
  return body.data
}
