<script setup>
// 엑셀(초록 스프레드시트) 아이콘 — 업로드/다운로드 버튼에 붙여 한눈에 보이게. currentColor를 안 쓰고 브랜드 그린 고정(엑셀 인지성이 우선)
defineProps({ size: { type: Number, default: 16 } })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 16 16" aria-hidden="true" class="excel-icon">
    <rect x="1" y="1" width="14" height="14" rx="3" fill="#1D7044" />
    <rect x="1" y="1" width="14" height="14" rx="3" fill="#21A366" fill-opacity="0.9" />
    <path d="M4 4.5h8M4 8h8M4 11.5h8" stroke="#FFFFFF" stroke-width="1" stroke-linecap="round" />
    <path d="M6.5 4.5v7M9.5 4.5v7" stroke="#FFFFFF" stroke-width="1" stroke-linecap="round" />
  </svg>
</template>

<style scoped>
.excel-icon {
  flex-shrink: 0;
}
</style>
