<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  sub: { type: String, default: '' },
})
</script>

<template>
  <div class="card kpi">
    <div class="kpi-label">{{ label }}</div>
    <div class="kpi-value">{{ value }}</div>
    <div v-if="sub" class="kpi-sub">{{ sub }}</div>
  </div>
</template>

<style scoped>
.kpi {
  border-top: 3px solid var(--color-accent);
}
.kpi-label {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}
.kpi-value {
  margin-top: var(--space-2);
  font-size: var(--font-size-kpi);
  font-weight: 700;
}
.kpi-sub {
  margin-top: var(--space-1);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
</style>
