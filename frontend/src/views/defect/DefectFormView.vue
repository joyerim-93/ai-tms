<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defectApi } from '@/api/defects'
import { executionApi } from '@/api/cycles'
import { projectApi } from '@/api/projects'
import { storeToRefs } from 'pinia'
import { useProjectStore } from '@/stores/projectStore'
import { useAuthStore } from '@/stores/authStore'
import { PRIORITY, SEVERITY } from '@/constants/labels'
import StatusBadge from '@/components/StatusBadge.vue'

// 등록: /defects/new[?executionId=] (실패 결과 패널에서 진입 시 TC 연결·내용 자동 채움)
// 수정: /defects/:id/edit
const route = useRoute()
const router = useRouter()
const { currentProjectId: projectId } = storeToRefs(useProjectStore())
const id = route.params.id
const isEdit = computed(() => !!id)
const auth = useAuthStore() // 보고자 = 로그인 사용자(서버가 기록), 담당자는 기본값 없이 직접 지정

const form = reactive({
  title: '',
  severity: 'MAJOR',
  priority: 'MEDIUM',
  assigneeId: '',
  description: '',
})
const execution = ref(null) // 연결된 수행 항목
const members = ref([])
const saving = ref(false)
const error = ref('')

async function prefillFromExecution(executionId) {
  const [exec, history] = await Promise.all([executionApi.get(executionId), executionApi.history(executionId)])
  execution.value = exec
  form.title = `[${exec.tcCode}] ${exec.tcTitle} - ${exec.result === 'BLOCKED' ? '수행 불가' : '실패'}`
  const lastComment = history.find((h) => h.comment)?.comment
  form.description = [
    `■ 테스트케이스: ${exec.tcCode} ${exec.tcTitle}`,
    '■ 재현 절차:',
    '',
    '■ 기대 결과:',
    '',
    '■ 실제 결과:',
    lastComment ?? '',
  ].join('\n')
}

onMounted(async () => {
  try {
    if (isEdit.value) {
      const d = await defectApi.get(id)
      Object.assign(form, {
        title: d.title,
        severity: d.severity,
        priority: d.priority,
        assigneeId: d.assigneeId ?? '',
        description: d.description ?? '',
      })
      if (d.executionId) execution.value = await executionApi.get(d.executionId)
    } else if (route.query.executionId) {
      await prefillFromExecution(route.query.executionId)
    }
  } catch (e) {
    error.value = e.message
  }
})

// 첫 방문 시 projectStore가 비동기로 채워지므로 watch
watch(
  projectId,
  async (pid) => {
    if (pid) members.value = await projectApi.members(pid).catch(() => [])
  },
  { immediate: true },
)

async function save() {
  saving.value = true
  error.value = ''
  const body = {
    ...form,
    assigneeId: form.assigneeId || null,
    projectId: projectId.value,
    executionId: execution.value?.id ?? null,
  }
  try {
    const saved = isEdit.value ? await defectApi.update(id, body) : await defectApi.create(body)
    router.replace(`/defects/${saved.id}`)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form @submit.prevent="save">
    <div class="page-actions">
      <button type="button" class="btn" @click="router.back()">취소</button>
      <button type="submit" class="btn btn-primary" :disabled="saving || !projectId">
        {{ saving ? '저장 중…' : '저장' }}
      </button>
    </div>
    <p v-if="error" class="error-text">{{ error }}</p>

    <section v-if="execution" class="card linked">
      <span class="label">연결된 테스트 수행</span>
      <span class="mono">{{ execution.tcCode }}</span>
      <span>{{ execution.tcTitle }}</span>
      <StatusBadge :status="execution.result" />
    </section>

    <section class="card">
      <div class="grid">
        <div class="span-4">
          <label class="label required">제목</label>
          <input v-model="form.title" class="input" maxlength="200" required />
        </div>
        <div>
          <label class="label required">심각도</label>
          <select v-model="form.severity" class="select">
            <option v-for="(s, key) in SEVERITY" :key="key" :value="key">{{ s.label }}</option>
          </select>
        </div>
        <div>
          <label class="label required">우선순위</label>
          <select v-model="form.priority" class="select">
            <option v-for="(label, key) in PRIORITY" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div v-if="!isEdit" class="span-2">
          <label class="label">보고자</label>
          <div class="readonly">{{ auth.currentUserName }}</div>
        </div>
        <div class="span-2">
          <label class="label">담당자</label>
          <select v-model="form.assigneeId" class="select">
            <option value="">미지정</option>
            <option v-for="m in members" :key="m.userId" :value="m.userId">{{ m.name }} ({{ m.projectRole }})</option>
          </select>
        </div>
        <div class="span-4">
          <label class="label">상세 내용</label>
          <textarea v-model="form.description" class="textarea description" />
        </div>
      </div>
    </section>
  </form>
</template>

<style scoped>
.linked {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-5);
}
.linked .label {
  margin: 0;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}
.span-2 {
  grid-column: span 2;
}
.span-4 {
  grid-column: span 4;
}
.description {
  min-height: 260px;
}
.readonly {
  display: flex;
  align-items: center;
  height: 34px;
  padding: 0 var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  color: var(--text-secondary);
}
</style>
