<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'

// 재귀 폴더 트리 — 자기 자신(FolderTree)을 children 렌더링에 재사용
defineOptions({ name: 'FolderTree' })

const props = defineProps({
  folders: { type: Array, required: true },   // [{ id, name, totalCount, children }]
  selectedId: { type: [Number, String, null], default: null },
  depth: { type: Number, default: 0 },
})
const emit = defineEmits(['select', 'rename', 'delete'])

// 기본 펼침. 접은 폴더 id만 기억
const collapsed = ref(new Set())
function toggle(id) {
  const next = new Set(collapsed.value)
  next.has(id) ? next.delete(id) : next.add(id)
  collapsed.value = next
}

// 이름 수정: 더블클릭 또는 우클릭 메뉴 → 인라인 입력 (Enter 저장 / Esc·빈 값·변경 없음 취소 / 포커스 아웃 저장)
const editingId = ref(null)
const editName = ref('')
const editInput = ref(null)
async function startEdit(f) {
  menu.value = null
  editingId.value = f.id
  editName.value = f.name
  await nextTick()
  editInput.value?.[0]?.focus()
  editInput.value?.[0]?.select()
}
function commitEdit(f) {
  if (editingId.value !== f.id) return // Esc 등으로 이미 종료
  const name = editName.value.trim()
  editingId.value = null
  if (name && name !== f.name) emit('rename', { id: f.id, name })
}
const cancelEdit = () => (editingId.value = null)

// 우클릭 컨텍스트 메뉴 (화면 고정 좌표)
const menu = ref(null) // { folder, x, y }
function openMenu(e, f) {
  menu.value = { folder: f, x: e.clientX, y: e.clientY }
  document.addEventListener('click', closeMenu, { once: true })
}
const closeMenu = () => (menu.value = null)
onBeforeUnmount(() => document.removeEventListener('click', closeMenu))

// 삭제 — 안이 비어있지 않으면 이동 안내를 포함한 확인창. 확인하면 id만 emit(실제 이동·삭제는 서버가 처리)
function confirmDelete(f) {
  menu.value = null
  const tcCount = f.testCaseCount ?? 0
  const subCount = f.children?.length ?? 0
  const message =
    tcCount || subCount
      ? `이 폴더에 TC ${tcCount}건, 하위폴더 ${subCount}개가 있습니다. 삭제하면 TC들은 상위 폴더로 이동합니다. 계속할까요?`
      : `'${f.name}' 폴더를 삭제할까요?`
  if (window.confirm(message)) emit('delete', f.id)
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
        @dblclick.stop="startEdit(f)"
        @contextmenu.prevent="openMenu($event, f)"
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
        <input
          v-if="editingId === f.id"
          ref="editInput"
          v-model="editName"
          class="rename-input"
          maxlength="200"
          @click.stop
          @dblclick.stop
          @keydown.enter.prevent="commitEdit(f)"
          @keydown.esc.prevent="cancelEdit"
          @blur="commitEdit(f)"
        />
        <span v-else class="name">{{ f.name }}</span>
        <span class="count">{{ f.totalCount }}</span>
      </div>
      <FolderTree
        v-if="f.children?.length && !collapsed.has(f.id)"
        :folders="f.children"
        :selected-id="selectedId"
        :depth="depth + 1"
        @select="emit('select', $event)"
        @rename="emit('rename', $event)"
        @delete="emit('delete', $event)"
      />
    </li>
  </ul>

  <Teleport to="body">
    <ul v-if="menu" class="ctx-menu" :style="{ left: `${menu.x}px`, top: `${menu.y}px` }" @click.stop>
      <li><button type="button" @click="startEdit(menu.folder)">✏️ 이름 수정</button></li>
      <li><button type="button" class="danger" @click="confirmDelete(menu.folder)">🗑️ 삭제</button></li>
    </ul>
  </Teleport>
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
.rename-input {
  flex: 1;
  min-width: 0;
  height: 24px;
  padding: 0 var(--space-1);
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  background: var(--surface-card);
  color: var(--text-primary);
  font: inherit;
  outline: none;
}
.ctx-menu {
  position: fixed;
  z-index: 200;
  min-width: 120px;
  margin: 0;
  padding: var(--space-1);
  list-style: none;
  background: var(--surface-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-overlay);
}
.ctx-menu button {
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  text-align: left;
  cursor: pointer;
}
.ctx-menu button:hover {
  background: var(--surface-hover);
}
.ctx-menu button.danger {
  color: var(--result-fail-text);
}
.ctx-menu button.danger:hover {
  background: var(--result-fail-bg);
}
.count {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  font-weight: 400;
}
</style>
