<script setup>
import { ref } from 'vue'

// 재귀 폴더 트리 — 자기 자신(FolderTree)을 children 렌더링에 재사용
defineOptions({ name: 'FolderTree' })

const props = defineProps({
  folders: { type: Array, required: true },   // [{ id, name, totalCount, children }]
  selectedId: { type: [Number, String, null], default: null },
  depth: { type: Number, default: 0 },
})
const emit = defineEmits(['select'])

// 기본 펼침. 접은 폴더 id만 기억
const collapsed = ref(new Set())
function toggle(id) {
  const next = new Set(collapsed.value)
  next.has(id) ? next.delete(id) : next.add(id)
  collapsed.value = next
}
</script>

<template>
  <ul class="tree" :class="{ nested: depth > 0 }">
    <li v-for="f in props.folders" :key="f.id">
      <div
        class="node"
        :class="{ selected: selectedId === f.id }"
        :style="{ paddingLeft: `calc(var(--space-2) + ${depth} * var(--space-4))` }"
        @click="emit('select', f.id)"
      >
        <button
          v-if="f.children?.length"
          type="button"
          class="toggle"
          :aria-label="collapsed.has(f.id) ? '펼치기' : '접기'"
          @click.stop="toggle(f.id)"
        >
          {{ collapsed.has(f.id) ? '▸' : '▾' }}
        </button>
        <span v-else class="toggle-space" />
        <span class="icon">📁</span>
        <span class="name">{{ f.name }}</span>
        <span class="count">{{ f.totalCount }}</span>
      </div>
      <FolderTree
        v-if="f.children?.length && !collapsed.has(f.id)"
        :folders="f.children"
        :selected-id="selectedId"
        :depth="depth + 1"
        @select="emit('select', $event)"
      />
    </li>
  </ul>
</template>

<style scoped>
.tree {
  margin: 0;
  padding: 0;
  list-style: none;
}
.node {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  height: 32px;
  padding-right: var(--space-2);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  user-select: none;
}
.node:hover {
  background: var(--surface-hover);
}
.node.selected {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}
.toggle,
.toggle-space {
  width: 16px;
  flex-shrink: 0;
}
.toggle {
  padding: 0;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  cursor: pointer;
}
.icon {
  font-size: var(--font-size-xs);
}
.name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.count {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  font-weight: 400;
}
</style>
