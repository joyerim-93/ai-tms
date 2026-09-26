<script setup>
import { onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/projectStore'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const { currentProjectId } = storeToRefs(projectStore)

onMounted(() => projectStore.loadProjects().catch(() => {}))

// 프로젝트 전환 시, 이전 프로젝트에 속한 상세 화면(차수/이슈)에 머물러 있으면 해당 목록으로 이동
const PROJECT_SCOPED = ['/cycles', '/defects']
watch(currentProjectId, (next, prev) => {
  if (!prev || !route.params.id) return
  const root = PROJECT_SCOPED.find((p) => route.path.startsWith(p))
  if (root) router.push(root)
})
</script>

<template>
  <AppHeader />
  <main class="content">
    <!-- 같은 컴포넌트 재사용 경로(수정→등록 등) 이동 시 상태 초기화 -->
    <RouterView :key="route.path" />
  </main>
</template>

<style scoped>
.content {
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: var(--space-5) var(--space-5) var(--space-6);
}
</style>
