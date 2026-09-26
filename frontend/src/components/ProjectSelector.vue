<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useProjectStore } from '@/stores/projectStore'

// 헤더 우측 프로젝트 선택 드롭다운 — "전환만" 가능 (등록은 테스트케이스 화면에서)
const projectStore = useProjectStore()
const { projects, currentProjectId, currentProject } = storeToRefs(projectStore)

const open = ref(false)
const root = ref(null)

function choose(id) {
  projectStore.selectProject(id)
  open.value = false
}

const closeOnOutside = (e) => {
  if (root.value && !root.value.contains(e.target)) open.value = false
}
const closeOnEsc = (e) => {
  if (e.key === 'Escape') open.value = false
}
onMounted(() => {
  document.addEventListener('click', closeOnOutside)
  document.addEventListener('keydown', closeOnEsc)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', closeOnOutside)
  document.removeEventListener('keydown', closeOnEsc)
})
</script>

<template>
  <div ref="root" class="project-selector">
    <button
      type="button"
      class="trigger"
      :aria-expanded="open"
      aria-haspopup="listbox"
      :disabled="!projects.length"
      @click="open = !open"
    >
      <span class="label">프로젝트</span>
      <span class="name">{{ currentProject?.name ?? '프로젝트 없음' }}</span>
      <span class="caret" :class="{ up: open }">▾</span>
    </button>

    <ul v-if="open" class="menu" role="listbox">
      <li
        v-for="p in projects"
        :key="p.id"
        role="option"
        :aria-selected="p.id === currentProjectId"
        class="option"
        :class="{ selected: p.id === currentProjectId }"
        @click="choose(p.id)"
      >
        <span class="check">{{ p.id === currentProjectId ? '✓' : '' }}</span>
        <span class="option-body">
          <span class="option-name">{{ p.name }}</span>
          <span class="option-code">{{ p.code }}</span>
        </span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.project-selector {
  position: relative;
  display: flex;
  align-items: center;
}
.trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  max-width: 320px;
  height: 36px;
  padding: 0 var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-primary);
  font: inherit;
  cursor: pointer;
  transition: border-color var(--transition);
}
.trigger:hover,
.trigger[aria-expanded='true'] {
  border-color: var(--accent);
}
.label {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}
.caret {
  color: var(--text-secondary);
  transition: transform var(--transition);
}
.caret.up {
  transform: rotate(180deg);
}
.menu {
  position: absolute;
  top: calc(100% + var(--space-1));
  right: 0;
  z-index: 20;
  min-width: 260px;
  margin: 0;
  padding: var(--space-1);
  list-style: none;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-overlay);
}
.option {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.option:hover {
  background: var(--surface-hover);
}
.option.selected .option-name {
  color: var(--accent);
  font-weight: 600;
}
.check {
  width: 14px;
  color: var(--accent);
  font-weight: 700;
}
.option-body {
  display: flex;
  flex-direction: column;
}
.option-code {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
}
</style>
