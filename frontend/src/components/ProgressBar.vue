<script setup>
import { computed } from 'vue'

// 결과별 누적 막대. stats: { totalCount, passCount, failCount, blockedCount, notRunCount }
const props = defineProps({
  stats: { type: Object, required: true },
})

const segments = computed(() => {
  const s = props.stats
  const pct = (n) => (s.totalCount ? (n / s.totalCount) * 100 : 0)
  return [
    { key: 'pass', width: pct(s.passCount) },
    { key: 'fail', width: pct(s.failCount) },
    { key: 'blocked', width: pct(s.blockedCount) },
    { key: 'notrun', width: pct(s.notRunCount) },
  ].filter((seg) => seg.width > 0)
})
</script>

<template>
  <div class="bar">
    <span v-for="seg in segments" :key="seg.key" :class="seg.key" :style="{ width: `${seg.width}%` }" />
  </div>
</template>

<style scoped>
.bar {
  display: flex;
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: var(--bg-hover);
}
.pass { background: var(--status-pass); }
.fail { background: var(--status-fail); }
.blocked { background: var(--status-blocked); }
.notrun { background: var(--status-notrun-bg); }
</style>
