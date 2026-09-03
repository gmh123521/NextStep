import { request, getToken } from './http'
import { formatResponseError } from '@/utils/error'

export interface PlanTask {
  id: number
  planId: number
  phase: string
  phaseOrder: number
  subject?: string
  title: string
  description?: string
  orderIdx: number
  completed: number
  completedAt?: string
}

export interface PhaseGroup {
  phase: string
  phaseOrder: number
  tasks: PlanTask[]
}

export interface PlanView {
  planId: number
  path: string
  pathName: string
  targetSummary?: string
  strategy?: string
  totalMonths: number
  riskAlerts?: string[]
  phases: PhaseGroup[]
  progressPct: number
  totalTasks: number
  doneTasks: number
  updatedAt?: string
}

export interface RecommendResult {
  months: number
  reason: string
}

export const plannerApi = {
  get: (path: string) =>
    request<PlanView | null>({ url: '/planner', params: { path } }),

  /** 推荐战线长度（基于年级/状态） */
  recommend: (path: string) =>
    request<RecommendResult>({ url: '/planner/recommend', params: { path } }),

  /** LLM 生成需要 30-60 秒，单独给 120s 超时；months 可选（不传走智能推断） */
  generate: (path: string, months?: number) =>
    request<PlanView>({
      url: '/planner/generate',
      method: 'POST',
      params: months != null ? { path, months } : { path },
      timeout: 120_000
    }),

  toggleTask: (taskId: number, completed: boolean) =>
    request<void>({ url: `/planner/tasks/${taskId}`, method: 'PUT', data: { completed } }),

  /** 导出当前路径规划为 PDF，触发浏览器下载 */
  exportPdf: async (path: string, pathName: string) => {
    const token = getToken()
    // 关键：PDF 二进制流不走 Vite 代理（dev 环境 vite proxy 对 PDF 字节流处理有 bug，
    //       会破坏字体子集中的 0x80+ 字节，导致浏览器内置 PDF 阅读器报"无法打开"）
    // 直接打到后端 8080（CORS 已配置允许跨域）；生产环境同源部署不受影响
    const isDev = import.meta.env.DEV
    const baseUrl = isDev ? 'http://localhost:8080' : ''
    const resp = await fetch(`${baseUrl}/api/planner/export?path=${path}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!resp.ok) throw new Error(await formatResponseError(resp, '规划导出失败'))
    const buf = await resp.arrayBuffer()
    const blob = new Blob([buf], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `NextStep-${pathName}规划.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }
}
