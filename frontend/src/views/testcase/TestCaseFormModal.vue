<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { testCaseApi } from '@/api/testCases'
import { folderApi } from '@/api/projects'
import { useProjectStore } from '@/stores/projectStore'
import { PRIORITY, TC_STATUS, TECHNIQUE } from '@/constants/labels'
import { flattenFolders, indentLabel } from '@/utils/folders'
import { extractVariables, braced, expectedToText, pairSteps, stepsToText } from '@/utils/params'
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

const form = reactive({
  folderId: props.defaultFolderId ?? '',
  title: '',
  module: '',              // 화면에서는 숨김(더 이상 안 씀) — 있던 값은 그대로 보존해서 저장
  priority: 'MEDIUM',
  status: 'ACTIVE',
  tags: '',
  technique: '',
  isParameterized: false,
  precondition: '',
  stepsText: '',           // '테스트 단계' 단일 텍스트 영역(줄 단위) — 저장 시 steps[] 로 변환
  expectedText: '',        // '기대결과' 단일 텍스트 영역
})
const tcAuthor = ref(null)                   // 수정 시 기존 작성자(변경 불가)
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
      stepsText: stepsToText(tc.steps),
      expectedText: expectedToText(tc.steps),
    })
  } catch (e) {
    error.value = e.message
  }
})

async function loadProjectOptions(projectId) {
  folders.value = flattenFolders((await folderApi.tree(projectId)).roots)
}

// 파라미터화(데이터 기반) 방식을 보여주는 예시 placeholder — 값으로 저장되지 않음(네이티브 placeholder)
const STEPS_PLACEHOLDER = '예:\n1. 가입금액에 {amount}원 입력\n2. 가입기간을 {period}개월로 설정\n3. 가입 신청 버튼 클릭'
const EXPECTED_PLACEHOLDER = '예: {expected} 안내 문구가 노출되며 가입이 {result}된다'

// 파라미터화 토글의 '단계에서 찾은 변수' 힌트용 — 저장할 형태(steps[])로 미리 변환해서 검사
const variables = computed(() => extractVariables(pairSteps(form.stepsText, form.expectedText)))

async function save() {
  error.value = ''
  saving.value = true
  try {
    const { stepsText, expectedText, ...rest } = form
    const body = {
      ...rest,
      projectId: currentProjectId.value, // 등록 시에만 사용 (수정 시 서버에서 무시)
      folderId: form.folderId || null,
      technique: form.technique || null,
      steps: pairSteps(stepsText, expectedText),
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
  <BaseModal :title="isEdit ? '테스트케이스 수정' : '테스트케이스 등록'" width="1120px" @close="emit('close')">
    <form id="tc-form" @submit.prevent="save">
    <p v-if="error" class="error-text">{{ error }}</p>

    <div class="layout">
      <!-- 좌측: 기본 정보 -->
      <section class="col">
        <div class="card-title">기본 정보</div>
        <div class="grid">
          <div>
            <label class="label">프로젝트</label>
            <div class="readonly">{{ (isEdit ? tcProject?.name : currentProject?.name) ?? '-' }}</div>
          </div>
          <div>
            <label class="label">폴더</label>
            <select v-model="form.folderId" class="select">
              <option value="">미분류</option>
              <option v-for="f in folders" :key="f.id" :value="f.id">{{ indentLabel(f) }}</option>
            </select>
          </div>
          <div v-if="isEdit" class="span-2">
            <label class="label">작성자</label>
            <div class="readonly">{{ tcAuthor ?? '-' }}</div>
          </div>
          <div class="span-2">
            <label class="label required">테스트케이스명</label>
            <input v-model="form.title" class="input" maxlength="300" required placeholder="예: 가입금액 경계값 검증 (데이터 기반)" />
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
            <input v-model="form.tags" class="input" maxlength="500" placeholder="콤마 구분" />
          </div>
          <div class="span-2">
            <label class="param-toggle">
              <input v-model="form.isParameterized" type="checkbox" />
              <span>
                <strong>파라미터화</strong>
                <span class="muted">— 단계에 <code>{변수}</code>, ‘데이터셋’ 탭에서 값 입력. <code>{expected}</code>는 행별 기대결과.</span>
              </span>
            </label>
            <p v-if="form.isParameterized && variables.length" class="muted small vars">
              변수: <code v-for="v in variables" :key="v">{{ braced(v) }}</code>
            </p>
          </div>
          <div class="span-2">
            <label class="label">사전조건 <span class="muted">(선택)</span></label>
            <textarea
              v-model="form.precondition"
              class="textarea precondition-input"
              maxlength="2000"
              placeholder="예: 상품 가입 화면에 진입한 상태"
            />
          </div>
        </div>
      </section>

      <!-- 우측: 절차 · 기대결과 -->
      <section class="col">
        <div class="card-title">절차 · 기대결과</div>
        <div class="steps-col">
          <div>
            <label class="label required">테스트 단계</label>
            <textarea
              v-model="form.stepsText"
              class="textarea steps-input"
              maxlength="4000"
              required
              :placeholder="STEPS_PLACEHOLDER"
            />
          </div>
          <div>
            <label class="label required">기대결과</label>
            <textarea
              v-model="form.expectedText"
              class="textarea steps-input"
              maxlength="4000"
              required
              :placeholder="EXPECTED_PLACEHOLDER"
            />
          </div>
          <p class="muted small param-hint">
            <code>{변수명}</code> 형태로 입력하면, 아래 ‘파라미터화’ 토글을 켰을 때 데이터셋 탭에서 값을 여러 세트로 넣어 반복 실행할 수 있어요.
          </p>
        </div>
      </section>
    </div>
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
.layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
  align-items: start;
}
.col .card-title {
  margin-bottom: var(--space-2);
}
.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-2) var(--space-3);
}
.grid .label,
.steps-col .label {
  margin-bottom: 2px;
}
.param-toggle {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
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
  margin: var(--space-1) 0 0;
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
.precondition-input {
  min-height: 48px;
}
.steps-col {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.steps-input {
  min-height: 118px;
  font-family: var(--font-mono);
  white-space: pre-wrap;
}
.param-hint {
  margin: 0;
}
.param-hint code {
  color: var(--accent);
  font-family: var(--font-mono);
}
</style>
