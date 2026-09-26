<script setup>
import { useProject } from '@/composables/useProject'

// 프로젝트 전환 pill 탭 — 선택 상태는 useProject(전역, localStorage 유지)
const { projects, projectId } = useProject()
</script>

<template>
  <div v-if="projects.length" class="project-tabs" role="tablist">
    <button
      v-for="p in projects"
      :key="p.id"
      type="button"
      role="tab"
      class="pill"
      :class="{ active: p.id === projectId }"
      :aria-selected="p.id === projectId"
      @click="projectId = p.id"
    >
      <span v-if="p.id === projectId" class="check">✓</span>
      {{ p.name }}
    </button>
  </div>
</template>

<style scoped>
.project-tabs {
  display: inline-flex;
  gap: var(--space-1);
  padding: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
}
.pill {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-4);
  border: none;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: color var(--transition), background var(--transition);
}
.pill:hover {
  color: var(--text-primary);
}
.pill.active {
  background: var(--surface-card);
  color: var(--accent);
  font-weight: 600;
  box-shadow: var(--shadow-card);
}
.check {
  font-weight: 700;
}
</style>
