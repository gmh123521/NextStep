import { request } from './http'

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

export interface AdminStats {
  totalUsers: number
  activeUsers: number
  disabledUsers: number
  adminUsers: number
  totalSchools: number
  totalGovPosts: number
  totalJobPositions: number
  totalSalaryStats: number
}

export interface AdminUser {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  status: number
  role: 'USER' | 'ADMIN'
  createdAt?: string
}

export interface SchoolRecord {
  id?: number
  name: string
  code?: string
  province?: string
  city?: string
  level?: string
  type?: string
  isSelfMarking?: number
}

export interface GovPostRecord {
  id?: number
  year: number
  examType?: string
  province?: string
  deptName: string
  postCode?: string
  postName: string
  region?: string
  degreeRequired?: string
  majorRequired?: string
}

export interface JobPositionRecord {
  id?: number
  name: string
  industryId?: number
  category?: string
  description?: string
}

export interface CrawlerJob {
  id: number
  source: string
  triggerBy: string
  status: string
  fetched: number
  inserted: number
  skipped: number
  message?: string
  startedAt?: string
  finishedAt?: string
}

const pageParams = (pageNum: number, pageSize: number, extra: Record<string, unknown> = {}) => ({
  pageNum,
  pageSize,
  ...extra
})

export const adminApi = {
  overview: () => request<AdminStats>({ url: '/admin/stats/overview' }),
  users: (params: { pageNum: number; pageSize: number; keyword?: string; status?: number; role?: string }) =>
    request<PageResult<AdminUser>>({ url: '/admin/users', params }),
  disableUser: (id: number) => request<void>({ url: `/admin/users/${id}/disable`, method: 'PUT' }),
  enableUser: (id: number) => request<void>({ url: `/admin/users/${id}/enable`, method: 'PUT' }),
  setUserRole: (id: number, role: 'USER' | 'ADMIN') =>
    request<void>({ url: `/admin/users/${id}/role`, method: 'PUT', params: { role } }),
  schools: (params: { pageNum: number; pageSize: number; keyword?: string; level?: string; province?: string }) =>
    request<PageResult<SchoolRecord>>({ url: '/admin/data/schools', params }),
  saveSchool: (data: SchoolRecord) => request<number>({ url: '/admin/data/schools', method: 'POST', data }),
  deleteSchool: (id: number) => request<void>({ url: `/admin/data/schools/${id}`, method: 'DELETE' }),
  govPosts: (params: { pageNum: number; pageSize: number; year?: number; province?: string; keyword?: string }) =>
    request<PageResult<GovPostRecord>>({ url: '/admin/data/gov-posts', params }),
  saveGovPost: (data: GovPostRecord) => request<number>({ url: '/admin/data/gov-posts', method: 'POST', data }),
  deleteGovPost: (id: number) => request<void>({ url: `/admin/data/gov-posts/${id}`, method: 'DELETE' }),
  jobPositions: (params: { pageNum: number; pageSize: number; keyword?: string; category?: string }) =>
    request<PageResult<JobPositionRecord>>({ url: '/admin/data/job-positions', params }),
  saveJobPosition: (data: JobPositionRecord) => request<number>({ url: '/admin/data/job-positions', method: 'POST', data }),
  deleteJobPosition: (id: number) => request<void>({ url: `/admin/data/job-positions/${id}`, method: 'DELETE' }),
  sources: () => request<string[]>({ url: '/admin/crawler/sources' }),
  runCrawler: (source: string) => request<CrawlerJob>({ url: `/admin/crawler/run/${source}`, method: 'POST' }),
  crawlerJobs: (params: { pageNum: number; pageSize: number; source?: string }) =>
    request<PageResult<CrawlerJob>>({ url: '/admin/crawler/jobs', params })
}
