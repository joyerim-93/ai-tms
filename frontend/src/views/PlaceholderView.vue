<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div class="card">
    <div class="card-title">{{ route.meta.title }}</div>
    <p class="muted">준비 중입니다.</p>
  </div>
</template>

<style scoped>
.muted {
  color: var(--text-muted);
  margin: 0;
}
</style>
