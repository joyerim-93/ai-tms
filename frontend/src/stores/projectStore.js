import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import { projectApi } from '@/api/projects'

// 전역 프로젝트 컨텍스트 — 대시보드/테스트케이스/테스트 수행/이슈관리가 모두 currentProjectId 기준으로 조회
// 새로고침해도 유지되도록 localStorage에 보관
const STORAGE_KEY = 'aitms-project'

function readStored() {
  try {
    return Number(localStorage.getItem(STORAGE_KEY)) || null
  } catch {
    return null
  }
}

export const useProjectStore = defineStore('project', () => {
  const projects = ref([])
  const currentProjectId = ref(readStored())
  const loaded = ref(false)

  const currentProject = computed(() => projects.value.find((p) => p.id === currentProjectId.value) ?? null)

  /** 목록 갱신. 저장된 선택이 목록에 없으면 첫 프로젝트로 보정 */
  async function loadProjects() {
    projects.value = await projectApi.list()
    if (!projects.value.some((p) => p.id === currentProjectId.value)) {
      currentProjectId.value = projects.value[0]?.id ?? null
    }
    loaded.value = true
  }

  function selectProject(id) {
    currentProjectId.value = id
  }

  watch(currentProjectId, (value) => {
    try {
      localStorage.setItem(STORAGE_KEY, value ?? '')
    } catch {
      // 저장 불가 환경은 무시
    }
  })

  return { projects, currentProjectId, currentProject, loaded, loadProjects, selectProject }
})
