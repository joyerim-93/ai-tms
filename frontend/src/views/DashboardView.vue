<script setup>
import KpiCard from '@/components/KpiCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'

// TODO: /api/dashboard 연동 전 임시 값
const kpis = [
  { label: '내 할일', value: '-', sub: '담당 미수행 TC + 결함' },
  { label: '이번 차수 진행률', value: '-', sub: '수행 완료 / 전체' },
  { label: '미해결 결함', value: '-', sub: 'NEW ~ IN_PROGRESS' },
  { label: '실패 TC', value: '-', sub: '이번 차수' },
]
</script>

<template>
  <section class="kpi-row">
    <KpiCard v-for="k in kpis" :key="k.label" v-bind="k" />
  </section>

  <section class="card">
    <div class="card-title">상태 뱃지</div>
    <div class="badges">
      <StatusBadge status="PASS" />
      <StatusBadge status="FAIL" />
      <StatusBadge status="BLOCKED" />
      <StatusBadge status="NOT_RUN" />
    </div>
  </section>
</template>

<style scoped>
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}
.badges {
  display: flex;
  gap: var(--space-2);
}
</style>
