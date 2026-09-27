<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { executionApi } from '@/api/cycles'
import { testCaseApi } from '@/api/testCases'
import { defectApi } from '@/api/defects'
import { RESULT, formatDateTime } from '@/constants/labels'
import StatusBadge from '@/components/StatusBadge.vue'
import PriorityChip from '@/components/PriorityChip.vue'
import TestCaseKeyBadge from '@/components/TestCaseKeyBadge.vue'
import DatasetTable from '@/components/DatasetTable.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import { substitute } from '@/utils/params'

// 우측 슬라이드 패널: TC 절차 확인 + 결과 입력(자동저장) + 수행 이력
const props = defineProps({
  executionId: { type: Number, required: true },
  readonly: { type: Boolean, default: false }, // 종료된 차수
})
const emit = defineEmits(['close', 'recorded'])

const DEBOUNCE_MS = 700
const RETRY_BASE_MS = 3000
const RETRY_MAX_MS = 15000

const exec = ref(null)
const tc = ref(null)
const history = ref([])
const comment = ref('')
const selectedResult = ref(null) // 항상 현재(마지막 저장된) 결과를 반영 — 값이 바뀌면 자동저장
const auth = useAuthStore() // 실행자 = 로그인 사용자(서버가 기록)
const defects = ref([]) // 이 수행 항목에 연결된 이슈
const error = ref('') // 목록 로딩 등 일반 오류
const router = useRouter()

// ── 자동저장 상태: idle(편집 전) | saving | saved | error
const saveStatus = ref('idle')
let ready = false // 최초 로드가 끝나기 전까지는 프리필로 인한 watch를 무시
let debounceTimer = null
let retryTimer = null
let retryDelay = RETRY_BASE_MS

async function fetchAll() {
  exec.value = await executionApi.get(props.executionId)
  const [testCase, hist, linked] = await Promise.all([
    testCaseApi.get(exec.value.testCaseId),
    executionApi.history(props.executionId),
    defectApi.search({ executionId: props.executionId, size: 50 }),
  ])
  tc.value = testCase
  history.value = hist
  defects.value = linked.items
}

// 파라미터화 TC의 데이터셋 행이면 {변수}를 이 행의 값으로 치환해서 표시
const stepText = (text) =>
  exec.value?.datasetId ? substitute(text, exec.value.datasetParams ?? {}, exec.value.datasetExpected) : text

const canReportDefect = () => ['FAIL', 'BLOCKED'].includes(exec.value?.result)
const reportDefect = () => router.push(`/defects/new?executionId=${props.executionId}`)

const selectResult = (key) => (selectedResult.value = key)

function scheduleSave() {
  if (!ready || props.readonly) return
  clearTimeout(debounceTimer)
  clearTimeout(retryTimer)
  saveStatus.value = 'saving'
  debounceTimer = setTimeout(doSave, DEBOUNCE_MS)
}

async function doSave() {
  saveStatus.value = 'saving'
  try {
    await executionApi.patch(props.executionId, { result: selectedResult.value, comment: comment.value })
    await fetchAll() // 이력·연결 이슈 등 화면을 갱신 (comment/selectedResult 는 건드리지 않음 — 편집 중 값 보존)
    saveStatus.value = 'saved'
    retryDelay = RETRY_BASE_MS
    emit('recorded')
  } catch (e) {
    saveStatus.value = 'error'
    error.value = e.message
    retryTimer = setTimeout(doSave, retryDelay)
    retryDelay = Math.min(retryDelay * 2, RETRY_MAX_MS)
  }
}

function retryNow() {
  clearTimeout(retryTimer)
  retryDelay = RETRY_BASE_MS
  doSave()
}

watch([selectedResult, comment], scheduleSave)

onMounted(async () => {
  try {
    await fetchAll()
    selectedResult.value = exec.value.result
    comment.value = exec.value.comment ?? ''
    await nextTick() // 위 두 대입으로 예약된 watch 콜백이 (ready=false인 채로) 먼저 실행되도록 한 틱 대기
  } catch (e) {
    error.value = e.message
  } finally {
    ready = true
  }
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  clearTimeout(retryTimer)
})
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <aside class="panel">
      <header class="panel-header">
        <div v-if="exec">
          <TestCaseKeyBadge :code="exec.tcCode" />
          <h3>{{ exec.tcTitle }}</h3>
          <div v-if="exec.datasetId" class="dataset-label">🔢 {{ exec.datasetLabel }}</div>
        </div>
        <div class="header-actions">
          <span v-if="!readonly && saveStatus !== 'idle'" class="save-status" :class="saveStatus">
            <span v-if="saveStatus === 'saving'">저장 중…</span>
            <span v-else-if="saveStatus === 'saved'">✓ 저장됨</span>
            <button v-else type="button" class="retry" @click="retryNow">⚠ 저장 실패, 재시도</button>
          </span>
          <button class="btn btn-sm" @click="emit('close')">✕</button>
        </div>
      </header>

      <div class="panel-body">
        <p v-if="error" class="error-text">{{ error }}</p>
        <template v-if="exec && tc">
          <div class="meta">
            <PriorityChip :priority="tc.priority" />
            <StatusBadge :status="exec.result" />
            <span class="muted">담당 {{ exec.assigneeName ?? '미지정' }}</span>
          </div>
          <p v-if="exec.tcVersion !== tc.version" class="notice">
            ⚠ 차수 등록 후 TC가 수정되었습니다 (등록 v{{ exec.tcVersion }} → 현재 v{{ tc.version }}). 아래는 현재 버전입니다.
          </p>

          <section v-if="tc.precondition" class="block">
            <div class="label">사전조건</div>
            <p class="pre">{{ tc.precondition }}</p>
          </section>

          <section class="block">
            <div class="label">
              테스트 단계
              <span v-if="exec.datasetId" class="chip chip-accent">‘{{ exec.datasetLabel }}’ 값으로 치환됨</span>
            </div>
            <ol v-if="tc.steps.length" class="steps">
              <li v-for="s in tc.steps" :key="s.id">
                <div class="pre">{{ stepText(s.action) }}</div>
                <div v-if="s.expectedResult" class="expected pre">→ {{ stepText(s.expectedResult) }}</div>
              </li>
            </ol>
            <p v-else class="muted">등록된 단계가 없습니다.</p>
          </section>

          <section v-if="exec.datasetId && tc.datasets.length" class="block">
            <div class="label">데이터셋 <span class="muted">(현재 행 강조)</span></div>
            <DatasetTable :rows="tc.datasets" :highlight-id="exec.datasetId" />
          </section>

          <section v-if="!readonly" class="block record">
            <div class="label">결과 입력</div>
            <div class="executor">
              <label class="label">실행자</label>
              <span class="executor-name">{{ auth.currentUserName }}</span>
            </div>
            <textarea v-model="comment" class="textarea" maxlength="2000" placeholder="코멘트 (선택)" />
            <AttachmentPanel
              base="executions"
              :owner-id="executionId"
              :emphasize="exec.result === 'FAIL' || exec.result === 'BLOCKED'"
              emphasize-text="실패/Block 결과입니다 — 원인 확인을 위해 스크린샷이나 로그를 첨부해 주세요."
              class="attach-block"
            />
            <div class="result-buttons">
              <button
                v-for="(label, key) in RESULT"
                :key="key"
                type="button"
                class="btn result-btn"
                :class="[`result-${key.toLowerCase()}`, { selected: selectedResult === key }]"
                @click="selectResult(key)"
              >
                {{ label }}
              </button>
            </div>
          </section>
          <section v-else class="block">
            <p class="muted">종료된 차수는 결과를 입력할 수 없습니다.</p>
            <AttachmentPanel base="executions" :owner-id="executionId" readonly />
          </section>

          <section class="block">
            <div class="block-head">
              <div class="label">연결된 이슈 ({{ defects.length }})</div>
              <button v-if="canReportDefect()" class="btn btn-sm btn-danger" @click="reportDefect">
                + 이슈 등록
              </button>
            </div>
            <ul v-if="defects.length" class="defects">
              <li v-for="d in defects" :key="d.id">
                <RouterLink :to="`/defects/${d.id}`" class="mono">{{ d.defectCode }}</RouterLink>
                <span class="defect-title">{{ d.title }}</span>
                <StatusBadge :status="d.status" />
              </li>
            </ul>
            <p v-else class="muted small">
              {{ canReportDefect() ? '실패/Block 결과입니다. 이슈를 등록하세요.' : '연결된 이슈가 없습니다.' }}
            </p>
          </section>

          <section class="block">
            <div class="label">수행 이력 ({{ history.length }})</div>
            <ul v-if="history.length" class="history">
              <li v-for="h in history" :key="h.id">
                <div class="history-head">
                  <StatusBadge :status="h.result" />
                  <span>{{ h.executedByName }}</span>
                  <span class="muted">{{ formatDateTime(h.executedAt) }}</span>
                </div>
                <p v-if="h.comment" class="pre comment">{{ h.comment }}</p>
              </li>
            </ul>
            <p v-else class="muted">아직 수행 이력이 없습니다.</p>
          </section>
        </template>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: var(--overlay);
}
.panel {
  position: absolute;
  inset: 0 0 0 auto;
  width: 520px;
  display: flex;
  flex-direction: column;
  background: var(--surface-card);
  box-shadow: var(--shadow-overlay);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.header-actions {
  display: flex;
  flex-shrink: 0;
  gap: var(--space-2);
}
.panel-header h3 {
  margin-top: var(--space-1);
  font-size: var(--font-size-lg);
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4) var(--space-5);
}
.meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.dataset-label {
  margin-top: var(--space-1);
  color: var(--accent);
  font-size: var(--font-size-sm);
  font-weight: 600;
}
.notice {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--badge-progress-text);
  background: var(--badge-progress-bg);
  font-size: var(--font-size-sm);
}
.block-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.defects {
  margin: var(--space-2) 0 0;
  padding: 0;
  list-style: none;
}
.defects li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
}
.defect-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.small {
  font-size: var(--font-size-xs);
}
.block {
  margin-top: var(--space-4);
}
.pre {
  margin: 0;
  white-space: pre-wrap;
}
.steps {
  margin: 0;
  padding-left: var(--space-5);
}
.steps li + li {
  margin-top: var(--space-3);
}
.expected {
  margin-top: var(--space-1);
  color: var(--text-secondary);
}
.result-buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-2);
  margin-top: var(--space-2);
}
.result-btn {
  justify-content: center;
  font-weight: 600;
  border: 2px solid transparent;
}
.result-pass { color: var(--result-success-text); background: var(--result-success-bg); }
.result-fail { color: var(--result-fail-text); background: var(--result-fail-bg); }
.result-blocked { color: var(--result-block-text); background: var(--result-block-bg); }
.result-not_run { color: var(--result-notrun-text); background: var(--result-notrun-bg); }
.result-btn:hover { filter: brightness(0.95); }
.result-btn.selected {
  border-color: currentColor;
}
.save-status {
  align-self: center;
  font-size: var(--font-size-sm);
  white-space: nowrap;
}
.save-status.saving,
.save-status.saved {
  color: var(--text-secondary);
}
.save-status.saved {
  color: var(--result-success-text);
}
.retry {
  padding: 0;
  border: none;
  background: none;
  color: var(--result-fail-text);
  font: inherit;
  font-size: var(--font-size-sm);
  cursor: pointer;
}
.retry:hover {
  text-decoration: underline;
}
.history {
  margin: 0;
  padding: 0;
  list-style: none;
}
.history li {
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border);
}
.history-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}
.attach-block {
  margin: var(--space-3) 0;
}
.executor {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}
.executor .label {
  margin: 0;
  white-space: nowrap;
}
.executor-name {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}
.comment {
  margin-top: var(--space-2);
  color: var(--text-secondary);
}
</style>
