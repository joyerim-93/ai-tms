<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import ProjectSelector from '@/components/ProjectSelector.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 사용자명 클릭 → 로그아웃 드롭다운 (ProjectSelector와 같은 패턴: 바깥 클릭·Esc로 닫힘)
const menuOpen = ref(false)
const userRoot = ref(null)
const closeOnOutside = (e) => {
  if (userRoot.value && !userRoot.value.contains(e.target)) menuOpen.value = false
}
const closeOnEsc = (e) => {
  if (e.key === 'Escape') menuOpen.value = false
}
onMounted(() => {
  document.addEventListener('click', closeOnOutside)
  document.addEventListener('keydown', closeOnEsc)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', closeOnOutside)
  document.removeEventListener('keydown', closeOnEsc)
})

async function logout() {
  menuOpen.value = false
  await auth.logout()
  router.push('/login')
}

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
      <ProjectSelector class="selector" />
      <ThemeToggle />
      <div ref="userRoot" class="user-menu">
        <button
          type="button"
          class="user-badge"
          :title="`${auth.user?.username} (${auth.user?.role})`"
          :aria-expanded="menuOpen"
          aria-haspopup="menu"
          @click="menuOpen = !menuOpen"
        >
          {{ auth.currentUserName }} 님
        </button>
        <ul v-if="menuOpen" class="menu-dropdown" role="menu">
          <li role="menuitem" class="menu-option" @click="logout">로그아웃</li>
        </ul>
      </div>
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
.selector {
  margin-left: auto;
}
.user-menu {
  position: relative;
  align-self: center;
}
.user-badge {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface-card);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--font-size-sm);
  white-space: nowrap;
  cursor: pointer;
  transition: border-color var(--transition);
}
.user-badge:hover,
.user-badge[aria-expanded='true'] {
  border-color: var(--accent);
}
.menu-dropdown {
  position: absolute;
  top: calc(100% + var(--space-1));
  right: 0;
  z-index: 20;
  min-width: 140px;
  margin: 0;
  padding: var(--space-1);
  list-style: none;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-overlay);
}
.menu-option {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  cursor: pointer;
}
.menu-option:hover {
  background: var(--surface-hover);
  color: var(--accent);
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
