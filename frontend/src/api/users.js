import { http } from './http'

// 가입된 사용자 목록 (담당자/실행자 선택용) — { id, displayName }[]
export const userApi = {
  list: () => http('/users'),
}
