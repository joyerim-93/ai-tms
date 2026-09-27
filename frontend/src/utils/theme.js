// 다크모드 저장/적용 — <html data-theme="light|dark">. theme.css 의 [data-theme='dark'] 블록이 실제 색을 정의.
const KEY = 'aitms-theme'

function systemPrefersDark() {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
}

export function readStoredTheme() {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function resolveInitialTheme() {
  return readStoredTheme() ?? (systemPrefersDark() ? 'dark' : 'light')
}

export function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme)
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    /* 저장 불가 환경이면 이번 세션에만 적용 */
  }
}
