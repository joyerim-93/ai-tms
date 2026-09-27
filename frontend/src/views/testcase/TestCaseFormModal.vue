<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { testCaseApi } from '@/api/testCases'
import { folderApi } from '@/api/projects'
import { useProjectStore } from '@/stores/projectStore'
import { useAuthStore } from '@/stores/authStore'
import { PRIORITY, TC_STATUS, TECHNIQUE } from '@/constants/labels'
import { flattenFolders, indentLabel } from '@/utils/folders'
import { extractVariables, braced } from '@/utils/params'
import BaseModal from '@/components/BaseModal.vue'

// 등록/수정 공용 팝업. 요구사항 연결은 여기서 다루지 않음 — 저장 후 상세의 '연결된 요구사항' 탭에서 연결
const props = defineProps({
  testCaseId: { type: [Number, String], default: null },      // 있으면 수정
  defaultFolderId: { type: Number, default: null },           // 등록 시 기본 폴더 (목록에서 선택 중이던 폴더)
})
const emit = defineEmits(['close', 'saved'])
const id = props.testCaseId
const isEdit = computed(() => !!id)
const { currentProjectId, currentProject } = storeToRefs(useProjectStore())
const auth = useAuthStore()

const form = reactive({
  folderId: props.defaultFolderId ?? '',
  title: '',
  module: '',
  priority: 'MEDIUM',
  status: 'ACTIVE',
  tags: '',
  technique: '',
  isParameterized: false,
  precondition: '',
  steps: [{ action: '', expectedResult: '' }],
})
const tcAuthor = ref(null)                   // 수정 시 기존 작성자(변경 불가)
const modules = ref([])
const folders = ref([])        // 평면 [{ id, name, depth, path }]
const tcProject = ref(null)    // 수정 시 TC 소유 프로젝트 { id, name }
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
    tcAuthor.value = tc.authorName ?? '시스템/AI'
    await loadProjectOptions(tc.projectId) // 폴더 이동은 같은 프로젝트 안에서만
    Object.assign(form, {
      folderId: tc.folderId ?? '',
      title: tc.title,
      module: tc.module ?? '',
      priority: tc.priority,
      status: tc.status,
      tags: tc.tags ?? '',
      technique: tc.technique ?? '',
      isParameterized: !!tc.isParameterized,
      precondition: tc.precondition ?? '',
      steps: tc.steps.map(({ action, expectedResult }) => ({ action, expectedResult: expectedResult ?? '' })),
    })
  } catch (e) {
    error.value = e.message
  }
})

async function loadProjectOptions(projectId) {
  const [tree, mods] = await Promise.all([
    folderApi.tree(projectId),
    testCaseApi.modules(projectId).catch(() => []),
  ])
  folders.value = flattenFolders(tree.roots)
  modules.value = mods
}

const variables = computed(() => extractVariables(form.steps))

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
    emit('saved', saved)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :title="isEdit ? '테스트케이스 수정' : '테스트케이스 등록'" width="900px" @close="emit('close')">
    <form id="tc-form" @submit.prevent="save">
    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="section">
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
        <div class="span-2">
          <label class="label">작성자</label>
          <div class="readonly">{{ isEdit ? (tcAuthor ?? '-') : auth.currentUserName }}</div>
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
          <label class="param-toggle">
            <input v-model="form.isParameterized" type="checkbox" />
            <span>
              <strong>파라미터화 (데이터 기반 반복 실행)</strong>
              <span class="muted">— 단계에 <code>{변수}</code>를 쓰고, 저장 후 ‘데이터셋’ 탭에서 행마다 값을 넣습니다. <code>{expected}</code>는 행별 기대결과.</span>
            </span>
          </label>
          <p v-if="form.isParameterized && variables.length" class="muted small vars">
            단계에서 찾은 변수: <code v-for="v in variables" :key="v">{{ braced(v) }}</code>
          </p>
        </div>
        <div class="span-4">
          <label class="label">사전조건</label>
          <textarea v-model="form.precondition" class="textarea" maxlength="2000" />
        </div>
      </div>
    </section>

    <section>
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

    <template #footer>
      <span class="muted small">{{ isEdit ? '' : '요구사항 연결은 저장 후 상세 화면의 ‘연결된 요구사항’ 탭에서 할 수 있습니다.' }}</span>
      <div class="actions">
        <button type="button" class="btn" @click="emit('close')">취소</button>
        <button type="submit" form="tc-form" class="btn btn-primary" :disabled="saving">
          {{ saving ? '저장 중…' : '저장' }}
        </button>
      </div>
    </template>
  </BaseModal>
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
.param-toggle {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  cursor: pointer;
}
.param-toggle:has(input:checked) {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.param-toggle code,
.vars code {
  margin-right: var(--space-1);
  color: var(--accent);
  font-family: var(--font-mono);
}
.vars {
  margin: var(--space-2) 0 0;
}
.small {
  font-size: var(--font-size-xs);
}
.actions {
  display: flex;
  gap: var(--space-2);
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
