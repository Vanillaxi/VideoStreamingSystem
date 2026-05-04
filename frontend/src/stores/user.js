import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo } from '../api/user'
import { clearAuth, getStoredUser, getToken, getTokenRole, getTokenUserId, setStoredUser, setToken } from '../utils/auth'
import { isAdminRole, isSuperAdminRole, roleText } from '../utils/role'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    user: getStoredUser(),
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    userId: (state) => state.user?.targetUserId || state.user?.id || getTokenUserId(state.token),
    role: (state) => Number(state.user?.role ?? getTokenRole(state.token) ?? 0),
    roleLabel: (state) => roleText(state.user?.role ?? getTokenRole(state.token) ?? 0),
    isAdmin: (state) => isAdminRole(state.user?.role ?? getTokenRole(state.token) ?? 0),
    isSuperAdmin: (state) => isSuperAdminRole(state.user?.role ?? getTokenRole(state.token) ?? 0)
  },
  actions: {
    async login(credentials) {
      this.loading = true
      try {
        const payload = {
          username: credentials.username,
          password: credentials.password
        }
        const result = await loginApi(payload)
        const token = result?.data?.token || result?.data
        if (!token) {
          throw new Error(result?.msg || '登录失败')
        }
        this.token = token
        setToken(token)

        const userId = getTokenUserId(token)
        if (userId) {
          await this.fetchUser(userId)
        }
        return result
      } finally {
        this.loading = false
      }
    },
    async fetchUser(id = this.userId) {
      if (!id) return null
      const result = await getUserInfo(id)
      this.user = result.data
      setStoredUser(result.data)
      return result.data
    },
    async logout() {
      try {
        if (this.token) {
          await logoutApi()
        }
      } finally {
        this.token = null
        this.user = null
        clearAuth()
      }
    },
    clearSession() {
      this.token = null
      this.user = null
      clearAuth()
    }
  }
})
