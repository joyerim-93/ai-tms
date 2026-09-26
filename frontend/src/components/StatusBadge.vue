<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }, // PASS | FAIL | BLOCKED | NOT_RUN
})

const STATUS = {
  PASS: { label: '성공', cls: 'pass' },
  FAIL: { label: '실패', cls: 'fail' },
  BLOCKED: { label: 'Block', cls: 'blocked' },
  NOT_RUN: { label: '미수행', cls: 'notrun' },
}

const info = computed(() => STATUS[props.status] ?? { label: props.status, cls: 'blocked' })
</script>

<template>
  <span class="badge" :class="info.cls">{{ info.label }}</span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
  line-height: 20px;
}
.pass { color: var(--status-pass); background: var(--status-pass-bg); }
.fail { color: var(--status-fail); background: var(--status-fail-bg); }
.blocked { color: var(--status-blocked); background: var(--status-blocked-bg); }
.notrun { color: var(--status-notrun); background: var(--status-notrun-bg); }
</style>
