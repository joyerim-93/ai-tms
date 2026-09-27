<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { testCaseApi } from '@/api/testCases'
import { requirementApi } from '@/api/requirements'
import { TC_STATUS, TC_SOURCE, REVIEW_STATUS, TECHNIQUE, REQUIREMENT_TYPE, formatDateTime } from '@/constants/labels'
import { extractVariables, substitute, braced } from '@/utils/params'
import PriorityChip from '@/components/PriorityChip.vue'
import LabelChip from '@/components/LabelChip.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TestCaseKeyBadge from '@/components/TestCaseKeyBadge.vue'
import DatasetTable from '@/components/DatasetTable.vue'
import RequirementLinkPicker from './RequirementLinkPicker.vue'
import TestCaseFormModal from './TestCaseFormModal.vue'

// Zephyr "Test Case Detail" 배치: 상단 Key·제목·뱃지 + 탭 5개 (선택 탭은 ?tab= 로 유지)
const TABS = [
  { key: 'overview', label: '개요' },
  { key: 'script', label: '테스트 스크립트' },
  { key: 'dataset', label: '데이터셋' },
  { key: 'runs', label: '실행 이력' },
  { key: 'requirements', label: '연결된 요구사항' },
]

const route = useRoute()
const router = useRouter()
const tc = ref(null)
const error = ref('')
const showEdit = ref(false)

const tab = computed(() => (TABS.some((t) => t.key === route.query.tab) ? route.query.tab : 'overview'))
const selectTab = (key) => router.replace({ query: { ...route.query, tab: key === 'overview' ? undefined : key } })

async function load() {
  tc.value = await testCaseApi.get(route.params.id)
}
onMounted(() => load().catch((e) => (error.value = e.message)))

async function run(fn) {
  error.value = ''
  try {
    await fn()
  } catch (e) {
    error.value = e.message
  }
}

// ── 개요: 검토 / 삭제
const review = (reviewStatus) => run(async () => (tc.value = await testCaseApi.review(tc.value.id, reviewStatus)))
function remove() {
  if (!window.confirm(`${tc.value.tcCode} 테스트케이스를 삭제할까요?`)) return
  run(async () => {
    await testCaseApi.remove(tc.value.id)
    router.push('/test-cases')
  })
}

// ── 테스트 스크립트: 데이터 행 선택 시 {변수} 치환 미리보기
const previewRowId = ref('') // '' = 원본(치환 전)
const previewRow = computed(() => tc.value?.datasets.find((d) => d.id === previewRowId.value) ?? null)
const stepText = (text) =>
  previewRow.value ? substitute(text, previewRow.value.paramValues, previewRow.value.expectedResultOverride) : text
const variables = computed(() => extractVariables(tc.value?.steps))

// ── 데이터셋: 행 단위 저장 후 목록 갱신
const reloadDatasets = async () => (tc.value.datasets = await testCaseApi.datasets(tc.value.id))
const createRow = (body) => run(async () => (await testCaseApi.createDataset(tc.value.id, body), reloadDatasets()))
const updateRow = (rowId, body) =>
  run(async () => (await testCaseApi.updateDataset(tc.value.id, rowId, body), reloadDatasets()))
function removeRow(rowId) {
  if (!window.confirm('이 데이터 행을 삭제할까요?')) return
  run(async () => (await testCaseApi.deleteDataset(tc.value.id, rowId), reloadDatasets()))
}

// ── 실행 이력: 탭 열 때 로드
const runs = ref(null)
watch(
  [tab, () => tc.value?.id],
  async ([t, id]) => {
    if (t === 'runs' && id && runs.value === null) await run(async () => (runs.value = await testCaseApi.runs(id)))
  },
  { immediate: true },
)

// ── 연결된 요구사항: 편집 모드에서 추가/제거
const editingLinks = ref(false)
const linkOptions = ref([])
const linkDraft = ref([])
async function startEditLinks() {
  await run(async () => {
    linkOptions.value = await requirementApi.atomics(tc.value.projectId)
    linkDraft.value = tc.value.requirements.map((r) => r.atomicRequirementId)
    editingLinks.value = true
  })
}
const saveLinks = () =>
  run(async () => {
    tc.value = await testCaseApi.replaceRequirements(tc.value.id, linkDraft.value)
    editingLinks.value = false
  })
</script>

<template>
  <div class="page-actions">
    <button class="btn" @click="router.push({ path: '/test-cases', query: tc?.folderId ? { folder: tc.folderId } : {} })">
      목록
    </button>
    <template v-if="tc">
      <button class="btn btn-danger" @click="remove">삭제</button>
      <button class="btn btn-primary" @click="showEdit = true">수정</button>
    </template>
  </div>
  <p v-if="error" class="error-text">{{ error }}</p>

  <template v-if="tc">
    <!-- 헤더: Key · 제목 · 뱃지 -->
    <section class="card head">
      <div class="head-row">
        <TestCaseKeyBadge :code="tc.tcCode" />
        <LabelChip :map="REVIEW_STATUS" :value="tc.reviewStatus" />
        <PriorityChip :priority="tc.priority" />
        <span v-if="tc.status === 'DEPRECATED'" class="chip chip-muted">{{ TC_STATUS.DEPRECATED }}</span>
        <span v-if="tc.isParameterized" class="chip chip-accent">Data-Driven · {{ tc.datasets.length }}행</span>
      </div>
      <h2 class="title">{{ tc.title }}</h2>
      <nav class="tabs">
        <button
          v-for="t in TABS"
          :key="t.key"
          type="button"
          class="tab"
          :class="{ active: tab === t.key }"
          @click="selectTab(t.key)"
        >
          {{ t.label }}
          <span v-if="t.key === 'dataset' && tc.isParameterized" class="tab-count">{{ tc.datasets.length }}</span>
          <span v-if="t.key === 'requirements'" class="tab-count">{{ tc.requirements.length }}</span>
        </button>
      </nav>
    </section>

    <!-- 개요 -->
    <section v-if="tab === 'overview'" class="card">
      <div v-if="tc.source !== 'MANUAL'" class="review-bar" :class="`review-${tc.reviewStatus.toLowerCase()}`">
        <div class="review-info">
          <LabelChip :map="TC_SOURCE" :value="tc.source" />
          <span v-if="tc.reviewStatus === 'DRAFT'" class="muted">AI 추천 TC입니다. 검토 후 승인해야 차수에 등록할 수 있습니다.</span>
          <span v-else class="muted">검토 {{ tc.reviewedByName ?? '-' }} · {{ formatDateTime(tc.reviewedAt) }}</span>
        </div>
        <div class="review-buttons">
          <button v-if="tc.reviewStatus !== 'APPROVED'" class="btn btn-sm" @click="review('APPROVED')">승인</button>
          <button v-if="tc.reviewStatus !== 'REJECTED'" class="btn btn-sm btn-danger" @click="review('REJECTED')">반려</button>
        </div>
      </div>
      <dl class="meta">
        <div><dt>프로젝트</dt><dd>{{ tc.projectName }}</dd></div>
        <div><dt>폴더</dt><dd>{{ tc.folderName ?? '미분류' }}</dd></div>
        <div><dt>모듈</dt><dd>{{ tc.module ?? '-' }}</dd></div>
        <div><dt>테스트 기법</dt><dd>{{ TECHNIQUE[tc.technique] ?? '-' }}</dd></div>
        <div><dt>출처</dt><dd><LabelChip :map="TC_SOURCE" :value="tc.source" /></dd></div>
        <div><dt>상태</dt><dd>{{ TC_STATUS[tc.status] }}</dd></div>
        <div v-if="tc.originProjectName"><dt>원본 프로젝트</dt><dd>{{ tc.originProjectName }}</dd></div>
        <div><dt>태그</dt><dd>{{ tc.tags ?? '-' }}</dd></div>
        <div><dt>작성자</dt><dd>{{ tc.authorName ?? '시스템/AI' }}</dd></div>
        <div><dt>버전</dt><dd>v{{ tc.version }}</dd></div>
        <div><dt>수정일</dt><dd>{{ formatDateTime(tc.updatedAt) }}</dd></div>
      </dl>
      <div class="block">
        <div class="label">사전조건</div>
        <p class="pre">{{ tc.precondition ?? '-' }}</p>
      </div>
    </section>

    <!-- 테스트 스크립트 -->
    <section v-else-if="tab === 'script'" class="card">
      <div class="script-head">
        <div class="card-title">
          테스트 단계 ({{ tc.steps.length }})
          <span v-if="tc.isParameterized" class="chip chip-accent">Data-Driven</span>
        </div>
        <label v-if="tc.isParameterized && tc.datasets.length" class="preview-select">
          데이터 행
          <select v-model="previewRowId" class="select">
            <option value="">원본 (치환 전)</option>
            <option v-for="d in tc.datasets" :key="d.id" :value="d.id">{{ d.rowLabel }}</option>
          </select>
        </label>
      </div>
      <p v-if="tc.isParameterized && variables.length" class="muted small">
        사용 변수: <span v-for="v in variables" :key="v" class="var">{{ braced(v) }}</span>
        <span class="var">{expected}</span> = 데이터 행의 기대결과
      </p>
      <table v-if="tc.steps.length" class="table">
        <thead>
          <tr>
            <th style="width: 60px">No</th>
            <th>수행 절차</th>
            <th>기대 결과</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in tc.steps" :key="s.id">
            <td>{{ s.stepNo }}</td>
            <td class="pre">{{ stepText(s.action) }}</td>
            <td class="pre">{{ stepText(s.expectedResult) ?? '-' }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">등록된 단계가 없습니다.</div>
    </section>

    <!-- 데이터셋 -->
    <section v-else-if="tab === 'dataset'" class="card">
      <template v-if="tc.isParameterized">
        <div class="card-title">데이터셋 <span class="muted small">— 행마다 차수에서 개별 실행됩니다</span></div>
        <DatasetTable
          :rows="tc.datasets"
          :suggested-variables="variables"
          editable
          @create="createRow"
          @update="updateRow"
          @remove="removeRow"
        />
      </template>
      <div v-else class="empty">
        파라미터화(데이터 기반) 테스트케이스가 아닙니다.<br />
        ‘수정’에서 <strong>파라미터화</strong>를 켜고 단계에 <span class="var">{변수}</span>를 쓰면 데이터셋을 편집할 수 있습니다.
      </div>
    </section>

    <!-- 실행 이력 -->
    <section v-else-if="tab === 'runs'" class="card">
      <div class="card-title">실행 이력 <span class="muted small">— 최근 순</span></div>
      <table v-if="runs?.length" class="table">
        <thead>
          <tr>
            <th style="width: 140px">실행일시</th>
            <th style="width: 180px">차수</th>
            <th v-if="tc.isParameterized">데이터 행</th>
            <th style="width: 80px">결과</th>
            <th style="width: 90px">수행자</th>
            <th>코멘트</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in runs" :key="r.id">
            <td class="muted small">{{ formatDateTime(r.executedAt) }}</td>
            <td>
              <RouterLink :to="{ path: `/cycles/${r.cycleId}`, query: { exec: r.executionId } }">
                {{ r.cycleNo }}차 {{ r.cycleName }}
              </RouterLink>
            </td>
            <td v-if="tc.isParameterized">{{ r.datasetLabel ?? '-' }}</td>
            <td><StatusBadge :status="r.result" /></td>
            <td>{{ r.executedByName ?? '-' }}</td>
            <td class="pre small">{{ r.comment ?? '' }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else-if="runs" class="empty">아직 실행 이력이 없습니다.</div>
    </section>

    <!-- 연결된 요구사항 -->
    <section v-else-if="tab === 'requirements'" class="card">
      <div class="script-head">
        <div class="card-title">이 케이스가 검증하는 요구사항 ({{ tc.requirements.length }})</div>
        <div v-if="editingLinks" class="link-actions">
          <button class="btn btn-sm" @click="editingLinks = false">취소</button>
          <button class="btn btn-sm btn-primary" @click="saveLinks">저장</button>
        </div>
        <button v-else class="btn btn-sm" @click="startEditLinks">추가 / 제거</button>
      </div>
      <RequirementLinkPicker v-if="editingLinks" v-model="linkDraft" :options="linkOptions" />
      <template v-else>
        <ul v-if="tc.requirements.length" class="req-list">
          <li v-for="r in tc.requirements" :key="r.atomicRequirementId">
            <RouterLink :to="{ path: '/test-cases/requirements', query: { req: r.requirementId } }" class="mono">
              {{ r.reqCode }}
            </RouterLink>
            <span class="chip chip-accent">{{ REQUIREMENT_TYPE[r.type] }}</span>
            <span>{{ r.atomicText }}</span>
            <span class="muted small">— {{ r.requirementTitle }}</span>
          </li>
        </ul>
        <p v-else class="muted">연결된 요구사항이 없습니다. ‘추가 / 제거’로 연결하세요.</p>
      </template>
    </section>
  </template>

  <TestCaseFormModal
    v-if="showEdit && tc"
    :test-case-id="tc.id"
    @close="showEdit = false"
    @saved="(saved) => { tc = saved; showEdit = false }"
  />
</template>

<style scoped>
.head {
  margin-bottom: var(--space-4);
  padding-bottom: 0;
}
.head-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.title {
  margin: var(--space-3) 0 var(--space-4);
  font-size: var(--font-size-xl);
}
.tabs {
  display: flex;
  gap: var(--space-1);
  margin: 0 calc(var(--space-5) * -1);
  padding: 0 var(--space-5);
  border-top: 1px solid var(--border);
}
.tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4);
  border: none;
  border-bottom: 2px solid transparent;
  background: none;
  color: var(--text-secondary);
  font: inherit;
  cursor: pointer;
}
.tab:hover {
  color: var(--text-primary);
}
.tab.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
  font-weight: 600;
}
.tab-count {
  padding: 0 6px;
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  font-weight: 500;
}
.review-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--surface-page);
}
.review-draft {
  border-left: 3px solid var(--badge-progress-text);
}
.review-info,
.review-buttons,
.link-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.meta {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin: 0;
}
.meta dt {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
.meta dd {
  margin: var(--space-1) 0 0;
}
.block {
  margin-top: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
}
.pre {
  margin: 0;
  white-space: pre-wrap;
}
.script-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.preview-select {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}
.preview-select .select {
  width: 240px;
}
.var {
  margin-right: var(--space-1);
  font-family: var(--font-mono);
  color: var(--accent);
}
.small {
  font-size: var(--font-size-xs);
}
.req-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.req-list li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
}
.req-list li + li {
  border-top: 1px solid var(--border);
}
</style>
