<script setup>
import { ref } from 'vue'
import { applyTheme, resolveInitialTheme } from '@/utils/theme'

// 헤더의 라이트/다크 토글 — 상태는 <html data-theme>, 저장은 localStorage(utils/theme.js)
const theme = ref(resolveInitialTheme())
const toggle = () => applyTheme((theme.value = theme.value === 'dark' ? 'light' : 'dark'))
</script>

<template>
  <button type="button" class="theme-toggle" :title="theme === 'dark' ? '라이트 모드로' : '다크 모드로'" @click="toggle">
    {{ theme === 'dark' ? '☀️' : '🌙' }}
  </button>
</template>

<style scoped>
.theme-toggle {
  align-self: center;
  padding: var(--space-1) var(--space-2);
  border: none;
  background: none;
  font-size: var(--font-size-md);
  line-height: 1;
  cursor: pointer;
}
.theme-toggle:hover {
  opacity: 0.7;
}
</style>
