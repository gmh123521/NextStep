import { request } from './http'

export interface ChatTurnResult {
  reply: string
  profileUpdated: boolean
  experienceAdded: boolean
  updatedFields: string[]
}

export interface ChatHistoryMessage {
  role: string
  content?: any
}

export const chatApi = {
  send: (message: string) =>
    request<ChatTurnResult>({ url: '/ai/chat', method: 'POST', data: { message } }),

  kickoff: () =>
    request<ChatTurnResult>({ url: '/ai/chat/kickoff', method: 'POST' }),

  history: () =>
    request<ChatHistoryMessage[]>({ url: '/ai/chat/history' }),

  reset: () =>
    request<void>({ url: '/ai/chat/history', method: 'DELETE' })
}
