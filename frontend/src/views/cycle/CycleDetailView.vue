<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cycleApi } from '@/api/cycles'
import { projectApi } from '@/api/projects'
import { CYCLE_STATUS, RESULT, formatDateTime, progressRate } from '@/constants/labels'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import PriorityChip from '@/components/PriorityChip.vue'
import TcPickerModal from './TcPickerModal.vue'
import ExecutionPanel from './ExecutionPanel.vue'

const route = useRoute()
const router = useRouter()
const cycleId = Number(route.params.id)

const cycle = ref(null)
const executions = ref([])
const members = ref([])
const filter = reactive({ keyword: '', result: '', assigneeId: '' })
const selected = ref(new Set())
const bulkAssignee = ref('')
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
    const { updated } = await cycleApi.assign(cycleId, [...selected.value], bulkAssignee.value || null)
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

async function onAdded(count) {
  showPicker.value = false
  await reload()
  message.value = `${count}건을 추가했습니다.`
}

onMounted(async () => {
  await reload()
  if (cycle.value) members.value = await projectApi.members(cycle.value.projectId).catch(() => [])
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
            <option v-for="m in members" :key="m.userId" :value="m.userId">{{ m.name }}</option>
          </select>
        </form>
        <div class="actions">
          <template v-if="selected.size && !closed">
            <select v-model="bulkAssignee" class="select">
              <option value="">담당자 해제</option>
              <option v-for="m in members" :key="m.userId" :value="m.userId">{{ m.name }}</option>
            </select>
            <button class="btn" @click="assignSelected">선택 {{ selected.size }}건 담당자 지정</button>
          </template>
          <button class="btn btn-primary" :disabled="closed" @click="showPicker = true">+ TC 추가</button>
        </div>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th style="width: 36px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
            <th style="width: 110px">코드</th>
            <th>제목</th>
            <th style="width: 110px">모듈</th>
            <th style="width: 80px">우선순위</th>
            <th style="width: 90px">담당자</th>
            <th style="width: 80px">결과</th>
            <th style="width: 150px">최종 수행</th>
            <th style="width: 44px"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in executions" :key="e.id" class="clickable" @click="panelExecId = e.id">
            <td @click.stop><input type="checkbox" :checked="selected.has(e.id)" @change="toggle(e.id)" /></td>
            <td class="mono">{{ e.tcCode }}</td>
            <td>
              {{ e.tcTitle }}
              <span v-if="e.tcVersion !== e.currentTcVersion" class="chip chip-medium" title="차수 등록 후 TC가 수정됨">
                TC 변경됨
              </span>
            </td>
            <td>{{ e.tcModule ?? '-' }}</td>
            <td><PriorityChip :priority="e.tcPriority" /></td>
            <td>{{ e.assigneeName ?? '-' }}</td>
            <td><StatusBadge :status="e.result" /></td>
            <td class="muted small">
              <template v-if="e.executedAt">{{ e.executedByName }} · {{ formatDateTime(e.executedAt) }}</template>
              <template v-else>-</template>
            </td>
            <td @click.stop>
              <button
                v-if="!closed && !e.executedAt"
                class="btn btn-sm btn-danger"
                title="차수에서 제외"
                @click="removeExecution(e)"
              >
                ✕
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!executions.length" class="empty">등록된 테스트케이스가 없습니다. ‘TC 추가’로 저장소에서 가져오세요.</div>
    </section>
  </template>

  <TcPickerModal
    v-if="showPicker"
    :cycle-id="cycleId"
    :project-id="cycle.projectId"
    :members="members"
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
  gap: var(--space-2);
}
.filters .input {
  width: 220px;
}
.filters .select,
.actions .select {
  width: 140px;
}
.small {
  font-size: var(--font-size-xs);
}
</style>
