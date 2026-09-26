import { ref, watch } from 'vue'
import { projectApi } from '@/api/projects'

// 전역 선택 프로젝트 (헤더 셀렉트) — 차수/결함/대시보드가 공유
const STORAGE_KEY = 'aitms-project'

function readStored() {
  try {
    return Number(localStorage.getItem(STORAGE_KEY)) || null
  } catch {
    return null
  }
}

const projects = ref([])
const projectId = ref(readStored())
let loading = null

watch(projectId, (value) => {
  try {
    localStorage.setItem(STORAGE_KEY, value ?? '')
  } catch {
    // 저장 불가 환경은 무시
  }
})

export function useProject() {
  if (!loading) {
    loading = projectApi
      .list()
      .then((list) => {
        projects.value = list
        if (!list.some((p) => p.id === projectId.value)) projectId.value = list[0]?.id ?? null
      })
      .catch(() => {
        loading = null // 다음 호출 때 재시도
      })
  }
  return { projects, projectId }
}
