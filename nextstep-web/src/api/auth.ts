import { request } from './http'

export interface LoginReq { username: string; password: string }
export interface RegisterReq extends LoginReq { nickname?: string; email?: string; phone?: string }
export interface LoginResp { token: string; expireMillis: number; username: string }
export interface MeResp { userId: number; username: string }

export const authApi = {
  login: (data: LoginReq) => request<LoginResp>({ url: '/auth/login', method: 'POST', data }),
  register: (data: RegisterReq) => request<number>({ url: '/auth/register', method: 'POST', data }),
  me: () => request<MeResp>({ url: '/auth/me' })
}
