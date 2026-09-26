<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RESULT } from '@/constants/labels'
import StatusBadge from './StatusBadge.vue'

// 결과 셀 인라인 수정 (Zephyr식) — 뱃지 클릭 → 드롭다운 → 선택 즉시 emit('change')
const props = defineProps({
  result: { type: String, required: true },
  disabled: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['change'])

const open = ref(false)
const root = ref(null)

function choose(value) {
  open.value = false
  if (value !== props.result) emit('change', value)
}
const close = (e) => {
  if (root.value && !root.value.contains(e.target)) open.value = false
}
onMounted(() => document.addEventListener('click', close))
onBeforeUnmount(() => document.removeEventListener('click', close))
</script>

<template>
  <div ref="root" class="result-select" @click.stop>
    <button
      type="button"
      class="trigger"
      :disabled="disabled || saving"
      :title="disabled ? '종료된 차수입니다' : '클릭해서 결과 변경'"
      @click="open = !open"
    >
      <StatusBadge :status="result" />
      <span v-if="!disabled" class="caret">▾</span>
    </button>
    <ul v-if="open" class="menu" role="listbox">
      <li v-for="(label, key) in RESULT" :key="key" role="option" :aria-selected="key === result" @click="choose(key)">
        <StatusBadge :status="key" />
        <span v-if="key === result" class="check">✓</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.result-select {
  position: relative;
  display: inline-block;
}
.trigger {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  background: none;
  cursor: pointer;
}
.trigger:hover:not(:disabled) {
  border-color: var(--border);
}
.trigger:disabled {
  cursor: default;
}
.caret {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
.menu {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  z-index: 30;
  min-width: 120px;
  margin: 0;
  padding: var(--space-1);
  list-style: none;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-overlay);
}
.menu li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.menu li:hover {
  background: var(--surface-hover);
}
.check {
  color: var(--accent);
  font-weight: 700;
}
</style>
