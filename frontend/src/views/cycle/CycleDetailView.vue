<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cycleApi, executionApi } from '@/api/cycles'
import { CYCLE_STATUS, RESULT, formatDateTime, progressRate } from '@/constants/labels'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import ResultSelect from '@/components/ResultSelect.vue'
import TestCaseKeyBadge from '@/components/TestCaseKeyBadge.vue'
import { formatParam } from '@/utils/params'
import UserCombo from '@/components/UserCombo.vue'
import { userApi } from '@/api/users'
import TcPickerModal from './TcPickerModal.vue'
import ExecutionPanel from './ExecutionPanel.vue'

const route = useRoute()
const router = useRouter()
const cycleId = Number(route.params.id)

const cycle = ref(null)
const executions = ref([])
const users = ref([]) // 가입된 사용자 전체 (담당자 선택 — 프로젝트 멤버 제한 없음)
const filter = reactive({ keyword: '', result: '', assigneeId: '' })
const selected = ref(new Set())
const bulkAssignee = ref(null)
const showPicker = ref(false)
// ?exec=<executionId> 로 진입하면 결과 입력 패널을 바로 연다 (대시보드 → 내 할일)
const panelExecId = ref(Number(route.query.exec) || null)
const error = ref('')
const message = ref('')

const closed = computed(() => cycle.value?.status === 'CLOSED')
const registeredTcIds = computed(() => new Set(executions.value.map((e) => e.testCaseId)))
const allChecked = computed(
  () => executions.value.length > 0 && executions.value.every((e) => selected.value.has(e.id)),
)

async function run(fn) {
  error.value = ''
  message.value = ''
  try {
    await fn()
  } catch (e) {
    error.value = e.message
  }
}

const loadCycle = () => run(async () => (cycle.value = await cycleApi.get(cycleId)))
const loadExecutions = () =>
  run(async () => {
    executions.value = await cycleApi.executions(cycleId, filter)
    selected.value = new Set()
  })
const reload = () => Promise.all([loadCycle(), loadExecutions()])

function changeStatus(status) {
  const { name, startDate, endDate } = cycle.value
  run(async () => (cycle.value = await cycleApi.update(cycleId, { name, startDate, endDate, status })))
}

function removeCycle() {
  if (!window.confirm(`${cycle.value.cycleNo}차 '${cycle.value.name}'을(를) 삭제할까요?`)) return
  run(async () => {
    await cycleApi.remove(cycleId)
    router.push('/cycles')
  })
}

function toggle(id) {
  const next = new Set(selected.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selected.value = next
}
function toggleAll() {
  selected.value = allChecked.value ? new Set() : new Set(executions.value.map((e) => e.id))
}

function assignSelected() {
  run(async () => {
    const { updated } = await cycleApi.assign(cycleId, [...selected.value], bulkAssignee.value)
    await loadExecutions()
    message.value = `${updated}건 담당자를 변경했습니다.`
  })
}

function removeExecution(e) {
  if (!window.confirm(`${e.tcCode}을(를) 차수에서 제외할까요?`)) return
  run(async () => {
    await cycleApi.removeExecution(cycleId, e.id)
    await reload()
  })
}

// ── Zephyr식 행 구성: 파라미터화 TC는 상위 행(하위 집계) + 데이터셋 하위 행
const collapsed = ref(new Set()) // 접힌 파라미터화 TC(testCaseId)
const rows = computed(() => {
  const groups = []
  for (const e of executions.value) {
    const last = groups[groups.length - 1]
    if (e.datasetId && last?.type === 'param' && last.testCaseId === e.testCaseId) {
      last.children.push(e)
    } else if (e.datasetId) {
      groups.push({ type: 'param', key: `p${e.testCaseId}`, testCaseId: e.testCaseId, head: e, children: [e] })
    } else {
      groups.push({ type: 'single', key: `s${e.id}`, exec: e })
    }
  }
  return groups
})

/** "4개 행 중 3개 성공 · 1개 실패" */
function summarize(children) {
  const count = (r) => children.filter((c) => c.result === r).length
  const parts = [['PASS', '성공'], ['FAIL', '실패'], ['BLOCKED', 'Block'], ['NOT_RUN', '미수행']]
    .map(([r, label]) => [count(r), label])
    .filter(([n]) => n > 0)
    .map(([n, label]) => `${n}개 ${label}`)
  return `${children.length}개 행 중 ${parts.join(' · ')}`
}
const groupAssignee = (children) => {
  const names = [...new Set(children.map((c) => c.assigneeName ?? '미지정'))]
  return names.length === 1 ? names[0] : `${names.length}명`
}
const latestRun = (children) => children.map((c) => c.executedAt).filter(Boolean).sort().at(-1)
const paramChips = (e) => Object.entries(e.datasetParams ?? {}).map(([k, v]) => `${k}=${formatParam(v)}`)

function toggleCollapse(testCaseId) {
  const next = new Set(collapsed.value)
  next.has(testCaseId) ? next.delete(testCaseId) : next.add(testCaseId)
  collapsed.value = next
}
function toggleGroup(children) {
  const next = new Set(selected.value)
  const allOn = children.every((c) => next.has(c.id))
  children.forEach((c) => (allOn ? next.delete(c.id) : next.add(c.id)))
  selected.value = next
}

// ── 결과 셀 인라인 수정 (코멘트 없이 결과만 기록 → 이력 1건)
const savingId = ref(null)
async function changeResult(e, result) {
  savingId.value = e.id
  await run(async () => {
    await executionApi.record(e.id, result, '')
    await reload()
  })
  savingId.value = null
}

async function onAdded(count) {
  showPicker.value = false
  await reload()
  message.value = `${count}건을 추가했습니다.`
}

onMounted(async () => {
  await reload()
  if (cycle.value) users.value = await userApi.list().catch(() => [])
})
</script>

<template>
  <div class="page-actions">
    <button class="btn" @click="router.push('/cycles')">목록</button>
    <button v-if="cycle" class="btn btn-danger" @click="removeCycle">차수 삭제</button>
  </div>
  <p v-if="error" class="error-text">{{ error }}</p>
  <p v-if="message" class="message">{{ message }}</p>

  <template v-if="cycle">
    <section class="card summary">
      <div class="summary-left">
        <div class="cycle-title">
          <span class="cycle-no">{{ cycle.cycleNo }}차</span>
          <h2>{{ cycle.name }}</h2>
        </div>
        <p class="muted">{{ cycle.startDate ?? '미정' }} ~ {{ cycle.endDate ?? '미정' }}</p>
        <label class="status-select">
          상태
          <select class="select" :value="cycle.status" @change="changeStatus($event.target.value)">
            <option v-for="(label, key) in CYCLE_STATUS" :key="key" :value="key">{{ label }}</option>
          </select>
        </label>
      </div>
      <div class="summary-right">
        <div class="progress-row">
          <span class="rate">{{ progressRate(cycle) }}%</span>
          <span class="muted">{{ cycle.totalCount - cycle.notRunCount }} / {{ cycle.totalCount }} 수행</span>
        </div>
        <ProgressBar :stats="cycle" />
        <div class="stat-row">
          <div><StatusBadge status="PASS" /> <strong>{{ cycle.passCount }}</strong></div>
          <div><StatusBadge status="FAIL" /> <strong>{{ cycle.failCount }}</strong></div>
          <div><StatusBadge status="BLOCKED" /> <strong>{{ cycle.blockedCount }}</strong></div>
          <div><StatusBadge status="NOT_RUN" /> <strong>{{ cycle.notRunCount }}</strong></div>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="toolbar">
        <form class="filters" @submit.prevent="loadExecutions">
          <input v-model="filter.keyword" class="input" placeholder="TC 코드 / 제목" />
          <select v-model="filter.result" class="select" @change="loadExecutions">
            <option value="">전체 결과</option>
            <option v-for="(label, key) in RESULT" :key="key" :value="key">{{ label }}</option>
          </select>
          <select v-model="filter.assigneeId" class="select" @change="loadExecutions">
            <option value="">전체 담당자</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName }}</option>
          </select>
        </form>
        <div class="actions">
          <template v-if="selected.size && !closed">
            <UserCombo v-model="bulkAssignee" :users="users" placeholder="담당자 해제" />
            <button class="btn" @click="assignSelected">선택 {{ selected.size }}건 담당자 지정</button>
          </template>
          <button class="btn btn-primary" :disabled="closed" @click="showPicker = true">+ TC 추가</button>
        </div>
      </div>

      <table class="table run-table">
        <thead>
          <tr>
            <th style="width: 36px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
            <th style="width: 110px">TC Key</th>
            <th>제목</th>
            <th style="width: 110px">결과</th>
            <th style="width: 90px">담당자</th>
            <th style="width: 130px">실행일시</th>
            <th style="width: 200px">코멘트</th>
            <th style="width: 40px"></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="g in rows" :key="g.key">
            <!-- 일반 TC: 1행 -->
            <tr v-if="g.type === 'single'" class="clickable" @click="panelExecId = g.exec.id">
              <td @click.stop><input type="checkbox" :checked="selected.has(g.exec.id)" @change="toggle(g.exec.id)" /></td>
              <td><TestCaseKeyBadge :code="g.exec.tcCode" :to="`/test-cases/${g.exec.testCaseId}`" /></td>
              <td>
                {{ g.exec.tcTitle }}
                <span v-if="g.exec.tcVersion !== g.exec.currentTcVersion" class="chip chip-medium" title="차수 등록 후 TC가 수정됨">
                  TC 변경됨
                </span>
              </td>
              <td>
                <ResultSelect
                  :result="g.exec.result"
                  :disabled="closed"
                  :saving="savingId === g.exec.id"
                  @change="changeResult(g.exec, $event)"
                />
              </td>
              <td>{{ g.exec.assigneeName ?? '-' }}</td>
              <td class="muted small">{{ g.exec.executedAt ? formatDateTime(g.exec.executedAt) : '-' }}</td>
              <td class="comment small" :title="g.exec.lastComment ?? ''">{{ g.exec.lastComment ?? '' }}</td>
              <td @click.stop>
                <button v-if="!closed && !g.exec.executedAt" class="btn btn-sm btn-danger" title="차수에서 제외" @click="removeExecution(g.exec)">✕</button>
              </td>
            </tr>

            <!-- 파라미터화 TC: 상위 행(하위 집계) -->
            <template v-else>
              <tr class="group-row" @click="toggleCollapse(g.testCaseId)">
                <td @click.stop>
                  <input
                    type="checkbox"
                    :checked="g.children.every((c) => selected.has(c.id))"
                    @change="toggleGroup(g.children)"
                  />
                </td>
                <td>
                  <span class="fold">{{ collapsed.has(g.testCaseId) ? '▸' : '▾' }}</span>
                  <TestCaseKeyBadge :code="g.head.tcCode" :to="`/test-cases/${g.testCaseId}`" />
                </td>
                <td>
                  {{ g.head.tcTitle }}
                  <span class="chip chip-accent">🔢 {{ g.children.length }}</span>
                  <div class="group-summary">{{ summarize(g.children) }}</div>
                </td>
                <td>
                  <div class="mini-bar" :title="summarize(g.children)">
                    <span
                      v-for="c in g.children"
                      :key="c.id"
                      :class="`seg-${c.result.toLowerCase()}`"
                    />
                  </div>
                </td>
                <td>{{ groupAssignee(g.children) }}</td>
                <td class="muted small">{{ latestRun(g.children) ? formatDateTime(latestRun(g.children)) : '-' }}</td>
                <td />
                <td />
              </tr>
              <!-- 데이터셋 하위 행 -->
              <template v-if="!collapsed.has(g.testCaseId)">
                <tr v-for="c in g.children" :key="c.id" class="clickable child-row" @click="panelExecId = c.id">
                  <td @click.stop><input type="checkbox" :checked="selected.has(c.id)" @change="toggle(c.id)" /></td>
                  <td class="indent muted">└ {{ c.datasetOrder }}</td>
                  <td>
                    <span class="row-label">{{ c.datasetLabel }}</span>
                    <span v-for="p in paramChips(c)" :key="p" class="param mono">{{ p }}</span>
                  </td>
                  <td>
                    <ResultSelect
                      :result="c.result"
                      :disabled="closed"
                      :saving="savingId === c.id"
                      @change="changeResult(c, $event)"
                    />
                  </td>
                  <td>{{ c.assigneeName ?? '-' }}</td>
                  <td class="muted small">{{ c.executedAt ? formatDateTime(c.executedAt) : '-' }}</td>
                  <td class="comment small" :title="c.lastComment ?? ''">{{ c.lastComment ?? '' }}</td>
                  <td @click.stop>
                    <button v-if="!closed && !c.executedAt" class="btn btn-sm btn-danger" title="차수에서 제외" @click="removeExecution(c)">✕</button>
                  </td>
                </tr>
              </template>
            </template>
          </template>
        </tbody>
      </table>
      <div v-if="!executions.length" class="empty">등록된 테스트케이스가 없습니다. ‘TC 추가’로 저장소에서 가져오세요.</div>
    </section>
  </template>

  <TcPickerModal
    v-if="showPicker"
    :cycle-id="cycleId"
    :project-id="cycle.projectId"
    :users="users"
    :registered-tc-ids="registeredTcIds"
    @close="showPicker = false"
    @added="onAdded"
  />
  <ExecutionPanel
    v-if="panelExecId"
    :execution-id="panelExecId"
    :readonly="closed"
    @close="panelExecId = null"
    @recorded="reload"
  />
</template>

<style scoped>
.message {
  color: var(--result-success-text);
}
.summary {
  display: flex;
  gap: var(--space-6);
  margin-bottom: var(--space-4);
}
.summary-left {
  flex: 1;
}
.summary-right {
  width: 480px;
}
.cycle-title {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}
.cycle-no {
  color: var(--accent);
  font-weight: 700;
}
.cycle-title h2 {
  font-size: var(--font-size-xl);
}
.status-select {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-secondary);
}
.status-select .select {
  width: 120px;
}
.progress-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: var(--space-2);
}
.rate {
  font-size: var(--font-size-stat);
  font-weight: 700;
}
.stat-row {
  display: flex;
  justify-content: space-between;
  margin-top: var(--space-3);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
.filters,
.actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.filters .input {
  width: 220px;
}
.filters .select,
.actions .select {
  width: 140px;
}
.group-row {
  background: var(--surface-page);
  cursor: pointer;
}
.group-row:hover {
  background: var(--surface-hover);
}
.fold {
  display: inline-block;
  width: 14px;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
.group-summary {
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
}
.mini-bar {
  display: flex;
  gap: 2px;
  width: 90px;
  height: 8px;
}
.mini-bar span {
  flex: 1;
  border-radius: 2px;
}
.seg-pass { background: var(--result-success-text); }
.seg-fail { background: var(--result-fail-text); }
.seg-blocked { background: var(--result-block-text); }
.seg-not_run { background: var(--result-notrun-bg); }
.child-row td {
  padding-top: var(--space-2);
  padding-bottom: var(--space-2);
}
.indent {
  padding-left: var(--space-6) !important;
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
}
.row-label {
  margin-right: var(--space-2);
}
.param {
  display: inline-block;
  margin-right: var(--space-1);
  padding: 0 6px;
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
}
.comment {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
}
.small {
  font-size: var(--font-size-xs);
}
</style>
