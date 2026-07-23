import { request } from './http'

export interface Industry { id: number; code: string; name: string }
export interface JobPosition { id: number; name: string; industryId?: number; category?: string; description?: string }

export const jobApi = {
  industries: () => request<Industry[]>({ url: '/data/job/industries' }),
  positions: (params: { industryId?: number; keyword?: string }) =>
    request<JobPosition[]>({ url: '/data/job/positions', params }),
  salary: (positionId: number) =>
    request<any[]>({ url: `/data/job/positions/${positionId}/salary` })
}
