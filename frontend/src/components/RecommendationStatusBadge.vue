<script setup>
import { computed } from 'vue'

// AI 추천 잡 상태 뱃지 — 진행 중(스피너) / 실패(클릭하면 재요청) / 그 외(성공·없음)는 표시 없음
const props = defineProps({
  status: { type: String, default: null },       // PENDING | RUNNING | SUCCEEDED | FAILED | null
  errorMessage: { type: String, default: null },
})
const emit = defineEmits(['retry'])

const running = computed(() => props.status === 'PENDING' || props.status === 'RUNNING')
const failed = computed(() => props.status === 'FAILED')
</script>

<template>
  <span v-if="running" class="rec-badge running" role="status">
    <span class="spinner" aria-hidden="true" />✨ AI 추천 중...
  </span>
  <button
    v-else-if="failed"
    type="button"
    class="rec-badge failed"
    :title="errorMessage || '추천에 실패했습니다'"
    @click.stop="emit('retry')"
  >
    ⚠️ 추천 실패, 다시 시도
  </button>
</template>

<style scoped>
.rec-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: 2px var(--space-2);
  border: none;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-xs);
  font-weight: 600;
  white-space: nowrap;
}
.running {
  background: var(--accent-soft);
  color: var(--accent);
}
.failed {
  background: var(--result-fail-bg);
  color: var(--result-fail-text);
  cursor: pointer;
}
.failed:hover {
  filter: brightness(0.96);
}
.spinner {
  width: 10px;
  height: 10px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: rec-spin 0.8s linear infinite;
}
@keyframes rec-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
