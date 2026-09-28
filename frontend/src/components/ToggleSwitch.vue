<script setup>
// 범용 on/off 스위치. v-model 로 사용 — <ToggleSwitch v-model="enabled" />
defineProps({
  modelValue: { type: Boolean, required: true },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <button
    type="button"
    class="switch"
    role="switch"
    :aria-checked="modelValue"
    :class="{ on: modelValue }"
    :disabled="disabled"
    @click="emit('update:modelValue', !modelValue)"
  >
    <span class="knob" />
  </button>
</template>

<style scoped>
.switch {
  position: relative;
  flex-shrink: 0;
  width: 40px;
  height: 22px;
  padding: 2px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  cursor: pointer;
  transition: background 0.15s;
}
.switch.on {
  background: var(--accent);
}
.switch:disabled {
  cursor: default;
  opacity: 0.6;
}
.knob {
  display: block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--surface-card);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: transform 0.15s;
}
.switch.on .knob {
  transform: translateX(18px);
}
</style>
