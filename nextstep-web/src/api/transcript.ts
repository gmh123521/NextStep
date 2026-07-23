import { request, getToken } from './http'

export interface CourseItem {
  courseName?: string
  credit?: number
  score?: number
  gpa?: number
  semester?: string
  category?: string
}

export interface TranscriptExtractResult {
  studentName?: string
  studentId?: string
  schoolName?: string
  majorName?: string
  computedGpa?: number
  gpaScale?: number
  officialGpaText?: string
  totalCredit?: number
  courses?: CourseItem[]
  notes?: string[]
}

export interface TranscriptApplyResult {
  inserted: number
  skipped: number
  profileGpaUpdated: boolean
}

export const transcriptApi = {
  /** 上传图片/PDF，返回多模态识别结果（不入库） */
  parse: async (file: File): Promise<TranscriptExtractResult> => {
    const form = new FormData()
    form.append('file', file)
    const token = getToken()
    const resp = await fetch('/api/ai/transcript/parse', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form
    })
    const body = await resp.json()
    if (body.code !== 200) throw new Error(body.msg || '识别失败')
    return body.data
  },

  /** 用户确认后入库 */
  apply: (data: TranscriptExtractResult, syncProfileGpa = true) =>
    request<TranscriptApplyResult>({
      url: '/ai/transcript/apply',
      method: 'POST',
      params: { syncProfileGpa },
      data
    })
}
