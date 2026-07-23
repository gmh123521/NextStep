import { defineStore } from 'pinia'
import { authApi, type LoginReq, type RegisterReq, type MeResp } from '@/api/auth'
import { clearToken, setToken } from '@/api/http'

export const useUserStore = defineStore('user', () => {
  const me = ref<MeResp | null>(null)

  async function login(req: LoginReq) {
    const resp = await authApi.login(req)
    setToken(resp.token)
    me.value = { userId: 0, username: resp.username }
    await refreshMe()
  }

  async function register(req: RegisterReq) {
    return authApi.register(req)
  }

  async function refreshMe() {
    me.value = await authApi.me()
  }

  function logout() {
    clearToken()
    me.value = null
  }

  return { me, login, register, refreshMe, logout }
})
