<script setup>
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import ProjectTabs from '@/components/ProjectTabs.vue'

const route = useRoute()
</script>

<template>
  <AppHeader />
  <main class="content">
    <ProjectTabs class="project-tabs" />
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
.project-tabs {
  margin-bottom: var(--space-5);
}
</style>
