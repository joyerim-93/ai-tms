<script setup>
import { reactive, ref } from 'vue'
import { projectApi } from '@/api/projects'
import { useProjectStore } from '@/stores/projectStore'
import BaseModal from '@/components/BaseModal.vue'

// 프로젝트 등록 — 테스트케이스 화면에서만 노출 (헤더 선택기는 전환 전용)
const emit = defineEmits(['close'])
const projectStore = useProjectStore()

const form = reactive({ code: '', name: '', description: '', startDate: '', endDate: '' })
const saving = ref(false)
const error = ref('')

async function submit() {
  saving.value = true
  error.value = ''
  try {
    const created = await projectApi.create({
      ...form,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
    })
    await projectStore.loadProjects()
    projectStore.selectProject(created.id) // 등록 즉시 새 프로젝트로 전환
    emit('close')
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal title="새 프로젝트" width="520px" @close="emit('close')">
    <form id="new-project-form" class="form" @submit.prevent="submit">
      <p v-if="error" class="error-text">{{ error }}</p>
      <div class="row">
        <div class="code">
          <label class="label required">코드</label>
          <input v-model="form.code" class="input mono" maxlength="30" placeholder="OPEN-BANK" required />
        </div>
        <div class="grow">
          <label class="label required">프로젝트명</label>
          <input v-model="form.name" class="input" maxlength="100" required />
        </div>
      </div>
      <div>
        <label class="label">설명</label>
        <textarea v-model="form.description" class="textarea" maxlength="2000" />
      </div>
      <div class="row">
        <div class="grow">
          <label class="label">시작일</label>
          <input v-model="form.startDate" type="date" class="input" />
        </div>
        <div class="grow">
          <label class="label">종료일</label>
          <input v-model="form.endDate" type="date" class="input" />
        </div>
      </div>
      <p class="muted hint">코드는 영문/숫자/-/_ 만 가능하며 대문자로 저장됩니다. 등록자는 PM으로 자동 참여합니다.</p>
    </form>
    <template #footer>
      <span />
      <div class="actions">
        <button type="button" class="btn" @click="emit('close')">취소</button>
        <button type="submit" form="new-project-form" class="btn btn-primary" :disabled="saving">등록</button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.row {
  display: flex;
  gap: var(--space-3);
}
.code {
  width: 160px;
}
.grow {
  flex: 1;
}
.hint {
  margin: 0;
  font-size: var(--font-size-xs);
}
.actions {
  display: flex;
  gap: var(--space-2);
}
</style>
