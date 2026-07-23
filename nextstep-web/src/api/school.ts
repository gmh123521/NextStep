import { request } from './http'

export interface School {
  id: number
  name: string
  code?: string
  province?: string
  city?: string
  level: string
  type?: string
  isSelfMarking?: number
}

export interface SchoolMajor {
  id: number
  schoolId: number
  majorCode: string
  majorName: string
  category?: string
  degreeType: string
}

export interface SchoolEnroll {
  year: number
  enrollPlan?: number
  enrollActual?: number
  applyCount?: number
  cutoffScore?: number
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

export const schoolApi = {
  page: (params: { pageNum?: number; pageSize?: number; keyword?: string; level?: string; province?: string }) =>
    request<PageResult<School>>({ url: '/data/school', params }),
  majors: (schoolId: number) =>
    request<SchoolMajor[]>({ url: `/data/school/${schoolId}/majors` }),
  enrolls: (majorId: number) =>
    request<SchoolEnroll[]>({ url: `/data/school/majors/${majorId}/enrolls` }),
  admitStats: (majorId: number) =>
    request<any[]>({ url: `/data/school/majors/${majorId}/admit-stats` })
}
