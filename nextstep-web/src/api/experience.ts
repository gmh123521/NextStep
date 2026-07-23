import { request } from './http'

export interface UserExperience {
  id: number
  userId: number
  type: string         // INTERNSHIP / PROJECT / AWARD / RESEARCH / PAPER / COMPETITION
  title: string
  role?: string
  startDate?: string
  endDate?: string
  description?: string
  source: string       // RESUME / MANUAL / CHAT
  createdAt?: string
}

export const experienceApi = {
  list: () => request<UserExperience[]>({ url: '/user/experiences' }),
  remove: (id: number) => request<void>({ url: `/user/experiences/${id}`, method: 'DELETE' })
}
