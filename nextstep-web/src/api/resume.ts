import { request, getToken } from './http'

export interface ResumeExperienceItem {
  type?: string
  title?: string
  role?: string
  startDate?: string
  endDate?: string
  description?: string
}

export interface ResumeExtractResult {
  currentSchool?: string
  schoolLevel?: string
  currentMajor?: string
  degreeType?: string
  gradeYear?: number
  gpa?: number
  gpaScale?: number
  englishLevel?: string
  englishScore?: number
  experiences?: ResumeExperienceItem[]
  notes?: string[]
}

export interface ResumeApplyResult {
  inserted: number
  skipped: number
}

export const resumeApi = {
  /** 上传 PDF，返回抽取结果（不入库） */
  parse: async (file: File): Promise<ResumeExtractResult> => {
    const form = new FormData()
    form.append('file', file)
    const token = getToken()
    const resp = await fetch('/api/ai/resume/parse', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form
    })
    const body = await resp.json()
    if (body.code !== 200) throw new Error(body.msg || '解析失败')
    return body.data
  },

  /** 用户确认后入库，返回新增/跳过数量 */
  apply: (data: ResumeExtractResult) =>
    request<ResumeApplyResult>({ url: '/ai/resume/apply', method: 'POST', data })
}
