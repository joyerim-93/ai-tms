import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api/auth'

// 로그인 사용자(세션 쿠키 기반). user = { id, username, displayName, role } | null
// currentUserName = 서버가 준 displayName — 작성자/실행자/보고자 표시에 그대로 쓰임
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const loaded = ref(false)
  const currentUserName = computed(() => user.value?.displayName ?? '')

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

  async function login(username, password) {
    user.value = await authApi.login(username, password)
    loaded.value = true
  }

  /** 회원가입 후 바로 로그인 */
  async function register(username, password, displayName) {
    await authApi.register(username, password, displayName)
    await login(username, password)
  }

  async function logout() {
    await authApi.logout().catch(() => {})
    user.value = null
  }

  return { user, loaded, currentUserName, loadMe, login, register, logout }
})
