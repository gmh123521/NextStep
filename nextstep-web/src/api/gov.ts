import { request } from './http'

export const govApi = {
  posts: (params: { year?: number; examType?: string; province?: string; keyword?: string }) =>
    request<any[]>({ url: '/data/gov/posts', params }),
  detail: (postId: number) =>
    request<any>({ url: `/data/gov/posts/${postId}` })
}
