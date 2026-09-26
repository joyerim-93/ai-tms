<script setup>
// 요약 카드: 제목(회색·작게) + 큰 숫자(+단위) + 보조텍스트(증감 등)
defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: '' },
  sub: { type: String, default: '' },
})
</script>

<template>
  <div class="card stat-card">
    <div class="title">{{ title }}</div>
    <div class="value">
      {{ value }}<span v-if="unit" class="unit">{{ unit }}</span>
    </div>
    <div v-if="sub" class="sub">{{ sub }}</div>
  </div>
</template>

<style scoped>
.stat-card {
  padding: var(--space-5);
}
.title {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}
.value {
  margin-top: var(--space-3);
  color: var(--text-primary);
  font-size: var(--font-size-stat);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.5px;
}
.unit {
  margin-left: var(--space-1);
  color: var(--text-secondary);
  font-size: var(--font-size-md);
  font-weight: 500;
}
.sub {
  margin-top: var(--space-3);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
}
</style>
