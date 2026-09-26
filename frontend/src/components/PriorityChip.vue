<script setup>
import { PRIORITY } from '@/constants/labels'

defineProps({
  priority: { type: String, required: true }, // HIGH | MEDIUM | LOW
})
</script>

<template>
  <span class="chip" :class="`chip-${priority.toLowerCase()}`">{{ PRIORITY[priority] ?? priority }}</span>
</template>
