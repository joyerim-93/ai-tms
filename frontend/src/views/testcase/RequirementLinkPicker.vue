<script setup>
import { computed } from 'vue'
import { REQUIREMENT_TYPE } from '@/constants/labels'

// "이 케이스가 검증하는 요구사항" 다중 선택 — 원문 요구사항별로 원자 요구사항 체크
const props = defineProps({
  modelValue: { type: Array, required: true },   // 선택된 atomicRequirementId[]
  options: { type: Array, required: true },      // AtomicRequirementRef[] (같은 프로젝트)
})
const emit = defineEmits(['update:modelValue'])

const groups = computed(() => {
  const map = new Map()
  for (const o of props.options) {
    if (!map.has(o.requirementId)) {
      map.set(o.requirementId, { reqCode: o.reqCode, title: o.requirementTitle, atomics: [] })
    }
    map.get(o.requirementId).atomics.push(o)
  }
  return [...map.values()]
})
const selected = computed(() => new Set(props.modelValue))

function toggle(id) {
  const next = new Set(selected.value)
  next.has(id) ? next.delete(id) : next.add(id)
  emit('update:modelValue', [...next])
}
</script>

<template>
  <div class="picker">
    <p v-if="!options.length" class="muted small">
      이 프로젝트에 원자 요구사항이 없습니다. ‘요구사항 · AI 추천’ 탭에서 요구사항을 먼저 등록하세요.
    </p>
    <div v-for="g in groups" :key="g.reqCode" class="group">
      <div class="group-head"><span class="mono">{{ g.reqCode }}</span> {{ g.title }}</div>
      <label v-for="a in g.atomics" :key="a.atomicRequirementId" class="option" :class="{ on: selected.has(a.atomicRequirementId) }">
        <input type="checkbox" :checked="selected.has(a.atomicRequirementId)" @change="toggle(a.atomicRequirementId)" />
        <span class="chip chip-accent">{{ REQUIREMENT_TYPE[a.type] }}</span>
        <span>{{ a.atomicText }}</span>
      </label>
    </div>
  </div>
</template>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.group-head {
  margin-bottom: var(--space-1);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 600;
}
.option {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  cursor: pointer;
}
.option + .option {
  margin-top: var(--space-1);
}
.option.on {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.small {
  margin: 0;
  font-size: var(--font-size-xs);
}
</style>
