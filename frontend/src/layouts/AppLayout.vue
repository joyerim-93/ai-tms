<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from '@/composables/useTheme'

const { theme, toggleTheme } = useTheme()
const route = useRoute()

const menus = [
  { to: '/', label: '대시보드', icon: '📊' },
  { to: '/test-cases', label: '테스트케이스 저장소', icon: '🗂️' },
  { to: '/cycles', label: '테스트수행관리', icon: '▶️' },
  { to: '/defects', label: '결함관리', icon: '🐞' },
]

const pageTitle = computed(() => route.meta.title ?? '')
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">AI-TMS</div>
      <nav>
        <RouterLink
          v-for="m in menus"
          :key="m.to"
          :to="m.to"
          class="nav-item"
          exact-active-class="active"
        >
          <span class="nav-icon">{{ m.icon }}</span>{{ m.label }}
        </RouterLink>
      </nav>
    </aside>

    <div class="main">
      <header class="header">
        <h1 class="page-title">{{ pageTitle }}</h1>
        <button class="btn theme-toggle" @click="toggleTheme">
          {{ theme === 'dark' ? '☀️ 라이트' : '🌙 다크' }}
        </button>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: var(--sidebar-width);
  background: var(--bg-sidebar);
  padding: var(--space-5) var(--space-3);
}
.brand {
  color: var(--text-sidebar-active);
  font-size: var(--font-size-xl);
  font-weight: 800;
  padding: 0 var(--space-3) var(--space-5);
  letter-spacing: 0.5px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  margin-bottom: var(--space-1);
  border-radius: var(--radius-md);
  color: var(--text-sidebar);
  transition: background var(--transition);
}
.nav-item:hover {
  background: var(--bg-sidebar-active);
}
.nav-item.active {
  background: var(--bg-sidebar-active);
  color: var(--text-sidebar-active);
  box-shadow: inset 3px 0 0 var(--color-accent);
  font-weight: 600;
}
.nav-icon {
  width: 20px;
  text-align: center;
}
.main {
  flex: 1;
  margin-left: var(--sidebar-width);
}
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
}
.page-title {
  font-size: var(--font-size-lg);
}
.content {
  padding: var(--space-6);
}
</style>
