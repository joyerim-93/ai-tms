<script setup>
// TC Key 뱃지 (예: TC-101) — to 가 있으면 상세 링크
defineProps({
  code: { type: String, required: true },
  to: { type: [String, Object], default: null },
})
</script>

<template>
  <RouterLink v-if="to" :to="to" class="key-badge" @click.stop>{{ code }}</RouterLink>
  <span v-else class="key-badge">{{ code }}</span>
</template>

<style scoped>
.key-badge {
  display: inline-block;
  padding: 1px var(--space-2);
  border-radius: var(--radius-sm);
  background: var(--accent-soft);
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  line-height: 20px;
  white-space: nowrap;
}
a.key-badge:hover {
  text-decoration: underline;
}
</style>
