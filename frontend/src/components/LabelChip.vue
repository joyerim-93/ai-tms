<script setup>
import { computed } from 'vue'

// labels.js 의 { KEY: { label, chip } } 형태 맵으로 칩 표시 (DEFECT_STATUS, SEVERITY 등)
const props = defineProps({
  map: { type: Object, required: true },
  value: { type: String, required: true },
})

const info = computed(() => props.map[props.value] ?? { label: props.value, chip: 'chip-muted' })
</script>

<template>
  <span class="chip" :class="info.chip">{{ info.label }}</span>
</template>
