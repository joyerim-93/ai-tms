<script setup>
defineProps({
  title: { type: String, required: true },
  width: { type: String, default: '720px' },
})
const emit = defineEmits(['close'])
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal" :style="{ width }" role="dialog">
      <header class="modal-header">
        <h3>{{ title }}</h3>
        <button class="btn btn-sm" @click="emit('close')">✕</button>
      </header>
      <div class="modal-body"><slot /></div>
      <footer v-if="$slots.footer" class="modal-footer"><slot name="footer" /></footer>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-overlay);
}
.modal {
  display: flex;
  flex-direction: column;
  max-height: 85vh;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-overlay);
}
.modal-header,
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-5);
}
.modal-header {
  border-bottom: 1px solid var(--border);
}
.modal-header h3 {
  font-size: var(--font-size-lg);
}
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4) var(--space-5);
}
.modal-footer {
  border-top: 1px solid var(--border);
}
</style>
