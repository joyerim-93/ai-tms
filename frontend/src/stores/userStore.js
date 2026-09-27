import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { USER_NAME_KEY, readStoredUserName } from '@/utils/userName'

// 현재 사용자 이름(표시용, 인증 아님) — localStorage와 동기화되어 새로고침해도 유지.
// 모든 API 요청에 X-User-Name 헤더로 실려 백엔드 CurrentUser(작성자/실행자/보고자 등)가 됨.
export const useUserStore = defineStore('user', () => {
  const currentUserName = ref(readStoredUserName())

  const setName = (name) => (currentUserName.value = (name ?? '').trim())

  watch(currentUserName, (name) => {
    try {
      name ? localStorage.setItem(USER_NAME_KEY, name) : localStorage.removeItem(USER_NAME_KEY)
    } catch {
      /* 저장 불가 환경이면 메모리에만 유지 */
    }
  })

  return { currentUserName, setName }
})
