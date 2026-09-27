<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { testCaseApi } from '@/api/testCases'
import { folderApi } from '@/api/projects'
import { useProjectStore } from '@/stores/projectStore'
import { TC_STATUS, TC_SOURCE, REVIEW_STATUS, TECHNIQUE } from '@/constants/labels'
import { flattenFolders, indentLabel } from '@/utils/folders'
import FolderTree from '@/components/FolderTree.vue'
import TestCaseKeyBadge from '@/components/TestCaseKeyBadge.vue'
import PriorityChip from '@/components/PriorityChip.vue'
import StatCard from '@/components/StatCard.vue'
import LabelChip from '@/components/LabelChip.vue'
import RepoTabs from './RepoTabs.vue'
import ImportTestCaseModal from './ImportTestCaseModal.vue'
import TestCaseFormModal from './TestCaseFormModal.vue'
import ExcelUploadModal from './ExcelUploadModal.vue'
import ExcelIcon from '@/components/ExcelIcon.vue'

// 좌: 폴더 트리 / 우: 선택 폴더(하위 포함)의 TC 목록. 선택 상태는 ?folder= (all | unfiled | id) 로 유지
const route = useRoute()
const router = useRouter()
const { currentProjectId: projectId, currentProject } = storeToRefs(useProjectStore())
// '공통 테스트케이스'(마스터 프로젝트)는 자체 관리만 — 다른 프로젝트 TC를 여기로 복사해올 수 없음(반대 방향은 그대로 허용)
const isCommonProject = computed(() => currentProject.value?.code === 'COMMON-TC')

const tree = ref({ roots: [], totalCount: 0, unfiledCount: 0 })
const counts = ref({ active: 0, draft: 0 }) // 상단 StatCard용 — 전체/미분류는 폴더 트리 값 재사용
const flatFolders = computed(() => flattenFolders(tree.value.roots))

const folderKey = computed(() => {
  const q = route.query.folder
  if (!q || q === 'all') return 'all'
  return q === 'unfiled' ? 'unfiled' : Number(q)
})
const selectedFolderId = computed(() => (typeof folderKey.value === 'number' ? folderKey.value : null))
const folderLabel = computed(() => {
  if (folderKey.value === 'all') return '전체 테스트케이스'
  if (folderKey.value === 'unfiled') return '미분류'
  return flatFolders.value.find((f) => f.id === folderKey.value)?.path ?? '폴더'
})

const DEFAULT_FILTER = { keyword: '', status: 'ACTIVE', source: '', reviewStatus: '' }
// 요구사항 탭의 'TC n건' 링크로 진입 시 ?atomicRequirementId= 필터
const filter = reactive({ ...DEFAULT_FILTER, atomicRequirementId: route.query.atomicRequirementId ?? '' })
const page = ref(1)
const size = 20
const result = ref({ items: [], total: 0 })
const loading = ref(false)
const error = ref('')
const message = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(result.value.total / size)))

// 폴더 추가 폼
const showFolderForm = ref(false)
const folderForm = reactive({ name: '', parentFolderId: '' })
const showImport = ref(false)
const showForm = ref(false)
const showExcel = ref(false)

async function loadTree() {
  if (!projectId.value) return
  tree.value = await folderApi.tree(projectId.value)
}

async function loadCounts() {
  if (!projectId.value) return
  const [active, draft] = await Promise.all([
    testCaseApi.search({ projectId: projectId.value, status: 'ACTIVE', size: 1 }),
    testCaseApi.search({ projectId: projectId.value, reviewStatus: 'DRAFT', size: 1 }),
  ])
  counts.value = { active: active.total, draft: draft.total }
}

const stats = computed(() => [
  { title: '전체 테스트케이스', value: tree.value.totalCount.toLocaleString(), unit: '건' },
  { title: '활성 테스트케이스', value: counts.value.active.toLocaleString(), unit: '건', sub: '상태 ACTIVE' },
  { title: '검토대기', value: counts.value.draft.toLocaleString(), unit: '건', sub: counts.value.draft ? 'AI 추천 승인 필요' : '' },
  { title: '미분류', value: tree.value.unfiledCount.toLocaleString(), unit: '건', sub: '폴더 미지정' },
])

async function load(p = 1) {
  if (!projectId.value) return
  page.value = p
  loading.value = true
  error.value = ''
  try {
    result.value = await testCaseApi.search({
      ...filter,
      projectId: projectId.value,
      folderId: selectedFolderId.value ?? '',
      unfiled: folderKey.value === 'unfiled' ? true : '',
      page: p,
      size,
    })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function selectFolder(key) {
  router.replace({ query: { ...route.query, folder: key === 'all' ? undefined : key } })
}

function reset() {
  Object.assign(filter, { ...DEFAULT_FILTER, atomicRequirementId: '' })
  load()
}

function openFolderForm() {
  folderForm.name = ''
  folderForm.parentFolderId = selectedFolderId.value ?? '' // 선택된 폴더 아래에 추가하는 것이 기본
  showFolderForm.value = true
}

async function createFolder() {
  error.value = ''
  try {
    const created = await folderApi.create(projectId.value, {
      name: folderForm.name,
      parentFolderId: folderForm.parentFolderId || null,
    })
    showFolderForm.value = false
    await loadTree()
    selectFolder(created.id)
  } catch (e) {
    error.value = e.message
  }
}

async function renameFolder({ id, name }) {
  error.value = ''
  try {
    await folderApi.rename(projectId.value, id, name)
    await loadTree()
    await load(page.value) // 목록의 폴더명 컬럼 갱신
  } catch (e) {
    error.value = e.message
  }
}

// 폴더 삭제(FolderTree에서 이미 확인창을 거침) — 안의 TC·직속 하위 폴더는 서버가 상위 폴더(최상위면 미분류)로 이동시킨 뒤 삭제
async function deleteFolder(id) {
  error.value = ''
  try {
    await folderApi.remove(projectId.value, id)
    if (selectedFolderId.value === id) selectFolder('all') // 삭제된 폴더를 보고 있었으면 전체로 이동
    await Promise.all([loadTree(), load(page.value)])
  } catch (e) {
    error.value = e.message
  }
}

// 등록 팝업 저장 → 상세로 이동 (파라미터화 TC를 새로 켰으면 바로 데이터셋 탭)
function onSaved(saved) {
  showForm.value = false
  router.push({ path: `/test-cases/${saved.id}`, query: saved.isParameterized && !saved.datasets.length ? { tab: 'dataset' } : {} })
}

// 엑셀 업로드: 결과는 팝업 안에 표시되고, 뒤의 폴더 트리/목록만 새로고침 (팝업은 사용자가 닫음)
async function onExcelUploaded() {
  await Promise.all([loadTree(), load(page.value)])
}

async function onImported(count) {
  const target = selectedFolderId.value ? folderLabel.value : '미분류'
  showImport.value = false
  message.value = `${count}건을 '${target}'에 가져왔습니다.`
  await Promise.all([loadTree(), load(page.value)])
}

// 프로젝트 전환: 트리 교체 + 폴더 선택 초기화
watch(
  projectId,
  async (next, prev) => {
    if (prev && route.query.folder) selectFolder('all')
    await Promise.all([loadTree().catch((e) => (error.value = e.message)), loadCounts().catch(() => {}), load()])
  },
  { immediate: true },
)
watch(folderKey, () => load())
</script>

<template>
  <RepoTabs />

  <section class="stat-row">
    <StatCard v-for="s in stats" :key="s.title" v-bind="s" />
  </section>

  <div class="split">
    <!-- 좌측: 폴더 트리 -->
    <aside class="card folder-panel">
      <div class="panel-title">폴더</div>
      <div class="node-root" :class="{ selected: folderKey === 'all' }" @click="selectFolder('all')">
        <span>🗂 전체 테스트케이스</span><span class="count">{{ tree.totalCount }}</span>
      </div>
      <FolderTree
        :folders="tree.roots"
        :selected-id="selectedFolderId"
        @select="selectFolder"
        @rename="renameFolder"
        @delete="deleteFolder"
      />
      <div class="node-root" :class="{ selected: folderKey === 'unfiled' }" @click="selectFolder('unfiled')">
        <span>📥 미분류</span><span class="count">{{ tree.unfiledCount }}</span>
      </div>

      <form v-if="showFolderForm" class="folder-form" @submit.prevent="createFolder">
        <input v-model="folderForm.name" class="input" maxlength="200" placeholder="폴더 이름" required autofocus />
        <select v-model="folderForm.parentFolderId" class="select">
          <option value="">최상위</option>
          <option v-for="f in flatFolders" :key="f.id" :value="f.id">{{ indentLabel(f) }}</option>
        </select>
        <div class="folder-form-actions">
          <button type="button" class="btn btn-sm" @click="showFolderForm = false">취소</button>
          <button class="btn btn-sm btn-primary">추가</button>
        </div>
      </form>
      <button v-else type="button" class="add-folder" :disabled="!projectId" @click="openFolderForm">+ 폴더 추가</button>
    </aside>

    <!-- 우측: 테스트케이스 목록 -->
    <section class="list-area">
      <div class="list-top">
        <h3 class="folder-title">{{ folderLabel }}</h3>
        <div class="actions">
          <button class="btn" :disabled="!projectId" @click="showExcel = true"><ExcelIcon />엑셀 업로드</button>
          <button v-if="!isCommonProject" class="btn" :disabled="!projectId" @click="showImport = true">
            다른 프로젝트에서 가져오기
          </button>
          <button class="btn btn-primary" :disabled="!projectId" @click="showForm = true">+ 테스트케이스 추가</button>
        </div>
      </div>
      <p v-if="message" class="message">{{ message }}</p>

      <form class="card filters" @submit.prevent="load()">
        <input v-model="filter.keyword" class="input keyword" placeholder="코드 / 테스트케이스명 / 태그 검색" />
        <select v-model="filter.status" class="select">
          <option value="">전체 상태</option>
          <option v-for="(label, key) in TC_STATUS" :key="key" :value="key">{{ label }}</option>
        </select>
        <select v-model="filter.source" class="select">
          <option value="">전체 출처</option>
          <option v-for="(s, key) in TC_SOURCE" :key="key" :value="key">{{ s.label }}</option>
        </select>
        <select v-model="filter.reviewStatus" class="select">
          <option value="">전체 검토</option>
          <option v-for="(s, key) in REVIEW_STATUS" :key="key" :value="key">{{ s.label }}</option>
        </select>
        <button type="submit" class="btn btn-primary">검색</button>
        <button type="button" class="btn" @click="reset">초기화</button>
      </form>

      <section class="card">
        <div class="list-header">
          <span>
            총 <strong>{{ result.total }}</strong>건
            <button
              v-if="filter.atomicRequirementId"
              class="chip chip-accent filter-chip"
              @click="filter.atomicRequirementId = ''; load()"
            >
              원자 요구사항 #{{ filter.atomicRequirementId }} ✕
            </button>
          </span>
          <span v-if="loading" class="muted">불러오는 중…</span>
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>

        <table class="table">
          <thead>
            <tr>
              <th style="width: 84px">Key</th>
              <th>테스트케이스명</th>
              <th style="width: 72px">우선순위</th>
              <th style="width: 120px">폴더</th>
              <th style="width: 96px">기법</th>
              <th style="width: 76px">출처</th>
              <th style="width: 76px">상태</th>
              <th style="width: 92px">데이터셋</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tc in result.items" :key="tc.id" class="clickable" @click="router.push(`/test-cases/${tc.id}`)">
              <td><TestCaseKeyBadge :code="tc.tcCode" /></td>
              <td>
                {{ tc.title }}
                <span v-if="tc.isParameterized" class="chip chip-accent" :title="`데이터셋 ${tc.datasetCount}행`">🔢 {{ tc.datasetCount }}</span>
                <span v-if="tc.status === 'DEPRECATED'" class="chip chip-muted">폐기</span>
                <span v-if="tc.originProjectName" class="chip chip-muted" :title="`원본: ${tc.originProjectName}`">가져옴</span>
              </td>
              <td><PriorityChip :priority="tc.priority" /></td>
              <td class="small">{{ tc.folderName ?? '미분류' }}</td>
              <td><span v-if="tc.technique" class="chip chip-muted">{{ TECHNIQUE[tc.technique] }}</span></td>
              <td><LabelChip :map="TC_SOURCE" :value="tc.source" /></td>
              <td><LabelChip :map="REVIEW_STATUS" :value="tc.reviewStatus" /></td>
              <td class="small">
                <span v-if="tc.isParameterized" class="nowrap">Data-Driven</span>
                <span v-else class="muted">-</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!loading && !result.items.length" class="empty">이 폴더에 테스트케이스가 없습니다.</div>

        <div v-if="totalPages > 1" class="pagination">
          <button class="btn btn-sm" :disabled="page <= 1" @click="load(page - 1)">이전</button>
          <span>{{ page }} / {{ totalPages }}</span>
          <button class="btn btn-sm" :disabled="page >= totalPages" @click="load(page + 1)">다음</button>
        </div>
      </section>
    </section>
  </div>

  <TestCaseFormModal
    v-if="showForm && projectId"
    :default-folder-id="selectedFolderId"
    @close="showForm = false"
    @saved="onSaved"
  />

  <ExcelUploadModal v-if="showExcel && projectId" :project-id="projectId" @close="showExcel = false" @uploaded="onExcelUploaded" />

  <ImportTestCaseModal
    v-if="showImport && projectId"
    :project-id="projectId"
    :folder-id="selectedFolderId"
    :folder-label="selectedFolderId ? folderLabel : '미분류'"
    @close="showImport = false"
    @imported="onImported"
  />
</template>

<style scoped>
.split {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: var(--space-4);
  align-items: start;
}
.folder-panel {
  position: sticky;
  top: calc(var(--header-height) + var(--space-4));
  padding: var(--space-3);
}
.panel-title {
  padding: var(--space-1) var(--space-2) var(--space-2);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.node-root {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 32px;
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  cursor: pointer;
}
.node-root:hover {
  background: var(--surface-hover);
}
.node-root.selected {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}
.count {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  font-weight: 400;
}
.add-folder {
  width: 100%;
  margin-top: var(--space-3);
  padding: var(--space-2);
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  background: none;
  color: var(--text-secondary);
  font: inherit;
  font-size: var(--font-size-sm);
  cursor: pointer;
}
.add-folder:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.folder-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border);
}
.folder-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
.list-area {
  min-width: 0;
}
.list-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}
.folder-title {
  font-size: var(--font-size-lg);
}
.actions {
  display: flex;
  gap: var(--space-2);
}
.message {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--accent);
  background: var(--accent-soft);
  font-size: var(--font-size-sm);
}
.filters {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  padding: var(--space-3);
}
.filters .keyword {
  flex: 1;
}
.filters .select {
  width: 110px;
}
.list-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-3);
  color: var(--text-secondary);
}
.filter-chip {
  margin-left: var(--space-2);
  border: none;
  cursor: pointer;
}
.small {
  font-size: var(--font-size-xs);
}
.nowrap {
  white-space: nowrap;
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
