<script setup>
// 테스트케이스 화면 상단 하위 탭(테스트케이스 / 요구사항). 프로젝트 등록은 헤더 ProjectSelector로 이동함
const tabs = [
  { to: '/test-cases', label: '테스트케이스' },
  { to: '/test-cases/requirements', label: '요구사항 · AI 추천' },
  { to: '/test-cases/review', label: 'AI 생성 검수' },
]
</script>

<template>
  <div class="repo-top">
    <nav class="tabs">
      <RouterLink v-for="t in tabs" :key="t.to" :to="t.to" class="tab" exact-active-class="active">
        {{ t.label }}
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.repo-top {
  margin-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
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
