<script setup>
import { ref } from 'vue'
import NewProjectModal from './NewProjectModal.vue'

// 테스트케이스 화면 상단: 하위 탭(테스트케이스 / 요구사항) + '+ 새 프로젝트'
// 프로젝트 등록은 이 화면에서만 가능 (헤더 ProjectSelector는 전환 전용)
const tabs = [
  { to: '/test-cases', label: '테스트케이스' },
  { to: '/test-cases/requirements', label: '요구사항 · AI 추천' },
]
const showNewProject = ref(false)
</script>

<template>
  <div class="repo-top">
    <nav class="tabs">
      <RouterLink v-for="t in tabs" :key="t.to" :to="t.to" class="tab" exact-active-class="active">
        {{ t.label }}
      </RouterLink>
    </nav>
    <button type="button" class="btn" @click="showNewProject = true">+ 새 프로젝트</button>
  </div>
  <NewProjectModal v-if="showNewProject" @close="showNewProject = false" />
</template>

<style scoped>
.repo-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
}
.repo-top .btn {
  margin-bottom: var(--space-2);
}
.tabs {
  display: flex;
  gap: var(--space-1);
}
.tab {
  padding: var(--space-2) var(--space-4);
  color: var(--text-secondary);
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.tab.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
  font-weight: 600;
}
</style>
