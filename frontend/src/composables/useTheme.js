import { ref, watch } from 'vue'

const STORAGE_KEY = 'aitms-theme'
// index.html 인라인 스크립트가 첫 페인트 전에 data-theme을 세팅해 둠
const theme = ref(document.documentElement.dataset.theme || 'light')

watch(theme, (value) => {
  document.documentElement.dataset.theme = value
  try {
    localStorage.setItem(STORAGE_KEY, value)
  } catch {
    // 저장 불가 환경(프라이빗 모드 등)은 무시
  }
})

export function useTheme() {
  const toggleTheme = () => {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }
  return { theme, toggleTheme }
}
