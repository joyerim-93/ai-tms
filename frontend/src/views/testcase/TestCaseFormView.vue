<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { testCaseApi } from '@/api/testCases'
import { folderApi } from '@/api/projects'
import { requirementApi } from '@/api/requirements'
import { useProjectStore } from '@/stores/projectStore'
import { PRIORITY, TC_STATUS, TECHNIQUE } from '@/constants/labels'
import { flattenFolders, indentLabel } from '@/utils/folders'
import RequirementLinkPicker from './RequirementLinkPicker.vue'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const isEdit = computed(() => !!id)
const { currentProjectId, currentProject } = storeToRefs(useProjectStore())

const form = reactive({
  folderId: route.query.folder ? Number(route.query.folder) : '', // 목록에서 선택 중이던 폴더가 기본
  title: '',
  module: '',
  priority: 'MEDIUM',
  status: 'ACTIVE',
  tags: '',
  technique: '',
  atomicRequirementIds: [], // 검증하는 원자 요구사항 (다대다)
  precondition: '',
  steps: [{ action: '', expectedResult: '' }],
})
const modules = ref([])
const folders = ref([])        // 평면 [{ id, name, depth, path }]
const tcProject = ref(null)    // 수정 시 TC 소유 프로젝트 { id, name }
const requirementOptions = ref([]) // 같은 프로젝트의 원자 요구사항
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    if (!isEdit.value) {
      if (currentProjectId.value) await loadProjectOptions(currentProjectId.value)
      return
    }
    const tc = await testCaseApi.get(id)
    tcProject.value = { id: tc.projectId, name: tc.projectName }
    await loadProjectOptions(tc.projectId) // 폴더 이동·요구사항 연결은 같은 프로젝트 안에서만
    Object.assign(form, {
      folderId: tc.folderId ?? '',
      title: tc.title,
      module: tc.module ?? '',
      priority: tc.priority,
      status: tc.status,
      tags: tc.tags ?? '',
      technique: tc.technique ?? '',
      atomicRequirementIds: tc.requirements.map((r) => r.atomicRequirementId),
      precondition: tc.precondition ?? '',
      steps: tc.steps.map(({ action, expectedResult }) => ({ action, expectedResult: expectedResult ?? '' })),
    })
  } catch (e) {
    error.value = e.message
  }
})

async function loadProjectOptions(projectId) {
  const [tree, atomics, mods] = await Promise.all([
    folderApi.tree(projectId),
    requirementApi.atomics(projectId),
    testCaseApi.modules(projectId).catch(() => []),
  ])
  folders.value = flattenFolders(tree.roots)
  requirementOptions.value = atomics
  modules.value = mods
}

const addStep = () => form.steps.push({ action: '', expectedResult: '' })
const removeStep = (i) => form.steps.splice(i, 1)
function moveStep(i, delta) {
  const j = i + delta
  if (j < 0 || j >= form.steps.length) return
  ;[form.steps[i], form.steps[j]] = [form.steps[j], form.steps[i]]
}

async function save() {
  error.value = ''
  // 완전히 빈 단계 행은 제외, 기대결과만 있는 행은 서버 검증에서 걸러지도록 전송
  const steps = form.steps.filter((s) => s.action.trim() || s.expectedResult.trim())
  saving.value = true
  try {
    const body = {
      ...form,
      projectId: currentProjectId.value, // 등록 시에만 사용 (수정 시 서버에서 무시)
      folderId: form.folderId || null,
      technique: form.technique || null,
      steps,
    }
    const saved = isEdit.value ? await testCaseApi.update(id, body) : await testCaseApi.create(body)
    router.push(`/test-cases/${saved.id}`)
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
      <button type="submit" class="btn btn-primary" :disabled="saving">
        {{ saving ? '저장 중…' : '저장' }}
      </button>
    </div>
    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="card section">
      <div class="card-title">기본 정보</div>
      <div class="grid">
        <div class="span-2">
          <label class="label">프로젝트</label>
          <div class="readonly">{{ (isEdit ? tcProject?.name : currentProject?.name) ?? '-' }}</div>
        </div>
        <div class="span-2">
          <label class="label">폴더</label>
          <select v-model="form.folderId" class="select">
            <option value="">미분류</option>
            <option v-for="f in folders" :key="f.id" :value="f.id">{{ indentLabel(f) }}</option>
          </select>
        </div>
        <div class="span-4">
          <label class="label required">제목</label>
          <input v-model="form.title" class="input" maxlength="300" required />
        </div>
        <div class="span-2">
          <label class="label">모듈</label>
          <input v-model="form.module" class="input" list="module-options" maxlength="100" placeholder="예: 인증" />
          <datalist id="module-options">
            <option v-for="m in modules" :key="m" :value="m" />
          </datalist>
        </div>
        <div>
          <label class="label required">우선순위</label>
          <select v-model="form.priority" class="select">
            <option v-for="(label, key) in PRIORITY" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div>
          <label class="label">상태</label>
          <select v-model="form.status" class="select">
            <option v-for="(label, key) in TC_STATUS" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div>
          <label class="label">테스트 기법</label>
          <select v-model="form.technique" class="select">
            <option value="">-</option>
            <option v-for="(label, key) in TECHNIQUE" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div>
          <label class="label">태그</label>
          <input v-model="form.tags" class="input" maxlength="500" placeholder="콤마로 구분 (예: smoke,login)" />
        </div>
        <div class="span-4">
          <label class="label">사전조건</label>
          <textarea v-model="form.precondition" class="textarea" maxlength="2000" />
        </div>
      </div>
    </section>

    <section class="card section">
      <div class="card-title">검증하는 요구사항 <span class="muted count">{{ form.atomicRequirementIds.length }}건 선택</span></div>
      <RequirementLinkPicker v-model="form.atomicRequirementIds" :options="requirementOptions" />
    </section>

    <section class="card">
      <div class="steps-header">
        <div class="card-title">테스트 단계</div>
        <button type="button" class="btn btn-sm" @click="addStep">+ 단계 추가</button>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th style="width: 50px">No</th>
            <th>수행 절차 <span class="error-text">*</span></th>
            <th>기대 결과</th>
            <th style="width: 120px"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(s, i) in form.steps" :key="i">
            <td>{{ i + 1 }}</td>
            <td><textarea v-model="s.action" class="textarea step-input" maxlength="2000" /></td>
            <td><textarea v-model="s.expectedResult" class="textarea step-input" maxlength="2000" /></td>
            <td class="step-buttons">
              <button type="button" class="btn btn-sm" :disabled="i === 0" @click="moveStep(i, -1)">↑</button>
              <button type="button" class="btn btn-sm" :disabled="i === form.steps.length - 1" @click="moveStep(i, 1)">↓</button>
              <button type="button" class="btn btn-sm btn-danger" @click="removeStep(i)">✕</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!form.steps.length" class="empty">단계가 없습니다. ‘단계 추가’를 눌러 주세요.</div>
    </section>
  </form>
</template>

<style scoped>
.section {
  margin-bottom: var(--space-4);
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}
.count {
  margin-left: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: 400;
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
.span-2 {
  grid-column: span 2;
}
.span-4 {
  grid-column: span 4;
}
.steps-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.step-input {
  min-height: 56px;
}
.step-buttons {
  white-space: nowrap;
}
.step-buttons .btn + .btn {
  margin-left: var(--space-1);
}
</style>
