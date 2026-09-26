<script setup>
import { computed } from 'vue'
import { CYCLE_STATUS, DEFECT_STATUS, RESULT } from '@/constants/labels'

/**
 * 공용 상태 뱃지 — 테스트 결과 / 이슈 상태 / 차수 상태를 하나의 색상 체계로 표시.
 * 표시명은 labels.js, 색상은 theme.css 의 --result-* / --badge-* 토큰.
 */
const props = defineProps({
  status: { type: String, required: true },
  label: { type: String, default: '' }, // 표시명 강제 지정 (선택)
})

const TONE = {
  // 테스트 결과
  PASS: 'success',
  FAIL: 'fail',
  BLOCKED: 'block',
  NOT_RUN: 'notrun',
  // 이슈(결함)
  NEW: 'open',
  OPEN: 'open',
  IN_PROGRESS: 'progress', // 차수 IN_PROGRESS 도 동일(진행중)
  RESOLVED: 'resolved',
  CLOSED: 'closed',        // 차수 CLOSED 도 동일(종료)
  REJECTED: 'closed',
  // 차수
  PLANNED: 'closed',
}

// IN_PROGRESS·CLOSED 는 이슈/차수 표시명이 같음 (진행중/종료)
const LABEL = { ...CYCLE_STATUS, ...DEFECT_STATUS, ...RESULT }

const tone = computed(() => TONE[props.status] ?? 'closed')
const text = computed(() => props.label || LABEL[props.status] || props.status)
</script>

<template>
  <span class="badge" :class="`tone-${tone}`">{{ text }}</span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-xs);
  font-weight: 600;
  line-height: 20px;
  white-space: nowrap;
}
/* 테스트 결과 */
.tone-success { color: var(--result-success-text); background: var(--result-success-bg); }
.tone-fail { color: var(--result-fail-text); background: var(--result-fail-bg); }
.tone-block { color: var(--result-block-text); background: var(--result-block-bg); }
.tone-notrun { color: var(--result-notrun-text); background: var(--result-notrun-bg); }
/* 이슈 / 차수 */
.tone-open { color: var(--badge-open-text); background: var(--badge-open-bg); }
.tone-progress { color: var(--badge-progress-text); background: var(--badge-progress-bg); }
.tone-resolved { color: var(--badge-resolved-text); background: var(--badge-resolved-bg); }
.tone-closed { color: var(--badge-closed-text); background: var(--badge-closed-bg); }
</style>
