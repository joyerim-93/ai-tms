<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/userStore'
import BaseModal from '@/components/BaseModal.vue'

// 담당자명 입력 — 첫 접속(required)에는 닫을 수 없고, 헤더에서 열면 취소 가능
const props = defineProps({
  required: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const userStore = useUserStore()
const { currentUserName } = storeToRefs(userStore)
const name = ref(currentUserName.value)
const canSave = () => name.value.trim().length > 0

function save() {
  if (!canSave()) return
  userStore.setName(name.value)
  emit('close')
}
</script>

<template>
  <BaseModal :title="required ? '이름을 입력해주세요' : '이름 수정'" width="420px" :closable="!required" @close="emit('close')">
    <form id="user-name-form" @submit.prevent="save">
      <p class="muted hint">
        작성자·실행자·보고자에 표시될 이름입니다. 로그인이 아니라 <strong>누가 입력했는지 표시하는 용도</strong>이며,
        이 브라우저에 저장됩니다.
      </p>
      <input v-model="name" class="input" maxlength="50" placeholder="예: 김큐에이" autofocus />
    </form>
    <template #footer>
      <span />
      <div class="actions">
        <button v-if="!props.required" type="button" class="btn" @click="emit('close')">취소</button>
        <button type="submit" form="user-name-form" class="btn btn-primary" :disabled="!canSave()">저장</button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
.hint {
  margin: 0 0 var(--space-3);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}
.actions {
  display: flex;
  gap: var(--space-2);
}
</style>
