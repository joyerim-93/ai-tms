<script setup>
import { computed, ref, watch } from 'vue'

// 담당자 자유입력 콤보박스 — 가입된 사용자 목록에서 선택하거나 이름을 직접 타이핑.
// 입력한 이름이 목록과 정확히(대소문자 무시) 일치하면 그 사용자로, 비우면 미지정으로, 일치하는 사용자가 없으면 미지정으로 처리하고 안내를 보여줌.
const props = defineProps({
  modelValue: { type: [Number, String, null], default: null }, // userId | null
  users: { type: Array, default: () => [] },                   // [{ id, displayName }]
  placeholder: { type: String, default: '이름 검색 또는 입력' },
  listId: { type: String, default: () => `user-combo-${Math.random().toString(36).slice(2)}` },
})
const emit = defineEmits(['update:modelValue'])

const nameOf = (id) => props.users.find((u) => u.id === id)?.displayName ?? ''
const text = ref(nameOf(props.modelValue))

// 부모가 modelValue 를 밖에서 바꾸면(초기화 등) 입력창도 맞춰줌
watch(
  () => props.modelValue,
  (id) => {
    if (nameOf(id) !== text.value) text.value = nameOf(id)
  },
)

const matched = computed(() => {
  const name = text.value.trim().toLowerCase()
  return name ? props.users.find((u) => u.displayName.trim().toLowerCase() === name) : null
})
const unmatched = computed(() => text.value.trim() && !matched.value)

function onInput() {
  emit('update:modelValue', matched.value?.id ?? null)
}
</script>

<template>
  <div class="user-combo">
    <input
      v-model="text"
      class="input"
      :list="listId"
      :placeholder="placeholder"
      maxlength="50"
      @input="onInput"
      @change="onInput"
    />
    <datalist :id="listId">
      <option v-for="u in users" :key="u.id" :value="u.displayName" />
    </datalist>
    <p v-if="unmatched" class="hint">일치하는 사용자가 없습니다 — 미지정으로 저장됩니다.</p>
  </div>
</template>

<style scoped>
.user-combo {
  min-width: 160px;
}
.hint {
  margin: var(--space-1) 0 0;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  white-space: nowrap;
}
</style>
