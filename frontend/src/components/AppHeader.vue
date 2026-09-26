<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const menus = [
  { to: '/', label: '대시보드' },
  { to: '/test-cases', label: '테스트케이스' },
  { to: '/cycles', label: '테스트 수행' },
  { to: '/defects', label: '이슈관리' },
]

// 하위 경로(/test-cases/1 등)도 해당 탭 활성. 대시보드('/')는 정확히 일치할 때만.
const isActive = (to) => (to === '/' ? route.path === '/' : route.path.startsWith(to))
</script>

<template>
  <header class="app-header">
    <div class="inner">
      <RouterLink to="/" class="logo">AI-TMS</RouterLink>
      <nav class="menu">
        <RouterLink v-for="m in menus" :key="m.to" :to="m.to" class="menu-item" :class="{ active: isActive(m.to) }">
          {{ m.label }}
        </RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--surface-card);
  border-bottom: 1px solid var(--border);
}
.inner {
  display: flex;
  align-items: stretch;
  gap: var(--space-6);
  height: var(--header-height);
  padding: 0 var(--space-5);
}
.logo {
  display: flex;
  align-items: center;
  color: var(--text-primary);
  font-size: var(--font-size-xl);
  font-weight: 800;
  letter-spacing: -0.3px;
}
.menu {
  display: flex;
  gap: var(--space-5);
}
.menu-item {
  display: flex;
  align-items: center;
  color: var(--text-secondary);
  font-size: var(--font-size-md);
  font-weight: 500;
  border-bottom: 2px solid transparent;
  transition: color var(--transition);
}
.menu-item:hover {
  color: var(--text-primary);
}
.menu-item.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
  font-weight: 600;
}
</style>
