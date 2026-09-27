import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api/auth'

// 로그인 사용자(세션 쿠키 기반). user = { id, loginId, name, role } | null
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const loaded = ref(false)
  const currentUserName = computed(() => user.value?.name ?? '')

  /** 새로고침 시 세션이 살아있는지 확인 (401 이면 비로그인) */
  async function loadMe() {
    try {
      user.value = await authApi.me()
    } catch {
      user.value = null
    } finally {
      loaded.value = true
    }
  }

  async function login(loginId, password) {
    user.value = await authApi.login(loginId, password)
    loaded.value = true
  }

  async function logout() {
    await authApi.logout().catch(() => {})
    user.value = null
  }

  return { user, loaded, currentUserName, loadMe, login, logout }
})
