// 표시용 담당자명 — 인증이 아니라 '누가 입력했는지' 기록용. Spring Security 도입 시 실제 로그인 사용자로 교체.
// http.js(헤더 X-User-Name)와 userStore가 같은 localStorage 키를 공유
export const USER_NAME_KEY = 'aitms-user-name'

export function readStoredUserName() {
  try {
    return localStorage.getItem(USER_NAME_KEY) ?? ''
  } catch {
    return ''
  }
}
