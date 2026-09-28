<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { llmSettingsApi, recommendationJobApi, requirementApi, ruleCatalogApi } from '@/api/requirements'
import { storeToRefs } from 'pinia'
import { useProjectStore } from '@/stores/projectStore'
import { useRoute } from 'vue-router'
import { PRIORITY, REQUIREMENT_TYPE, REVIEW_STATUS, TC_SOURCE, TECHNIQUE } from '@/constants/labels'
import PriorityChip from '@/components/PriorityChip.vue'
import LabelChip from '@/components/LabelChip.vue'
import RecommendationStatusBadge from '@/components/RecommendationStatusBadge.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'
import RepoTabs from './RepoTabs.vue'

// 요구사항 원문 → 원자 요구사항(AI 분해) → 규칙/RAG/LLM 추천 TC 흐름의 입구
const { currentProjectId: projectId } = storeToRefs(useProjectStore())
const route = useRoute()

const requirements = ref([])
const expanded = ref(null)       // 펼친 요구사항 상세 (atomics 포함)
const rulesByType = ref({})
const showForm = ref(false)
const form = reactive({ title: '', priority: 'MEDIUM', description: '' })
const error = ref('')

async function load() {
  if (!projectId.value) return
  error.value = ''
  try {
    requirements.value = await requirementApi.list(projectId.value)
  } catch (e) {
    error.value = e.message
  }
}

async function toggle(r) {
  if (expanded.value?.id === r.id) {
    expanded.value = null
    return
  }
  try {
    expanded.value = await requirementApi.get(r.id)
  } catch (e) {
    error.value = e.message
  }
}

async function create() {
  error.value = ''
  try {
    await requirementApi.create({ ...form, projectId: projectId.value })
    Object.assign(form, { title: '', priority: 'MEDIUM', description: '' })
    showForm.value = false
    await load()
  } catch (e) {
    error.value = e.message
  }
}

// AI 추천 요청(규칙기반 RULE + RAG + AI 생성 LLM) → 잡 생성 → 2초마다 상태 폴링 → 완료되면 DRAFT TC 결과 표시 + 목록 새로고침
const POLL_MS = 2000
const jobs = reactive({})     // requirementId → 최근 잡 { id, status, errorMessage, result }
const results = reactive({})  // requirementId → 완료된 추천 결과 { message, created, skipped, warnings, scores }
const timers = {}

const isActive = (job) => job?.status === 'PENDING' || job?.status === 'RUNNING'

function stopPolling(reqId) {
  clearInterval(timers[reqId])
  delete timers[reqId]
}
function stopAllPolling() {
  Object.keys(timers).forEach(stopPolling)
}

function poll(reqId, jobId) {
  stopPolling(reqId)
  timers[reqId] = setInterval(async () => {
    try {
      const job = await recommendationJobApi.get(jobId)
      jobs[reqId] = job
      if (isActive(job)) return
      stopPolling(reqId)
      if (job.status === 'SUCCEEDED') await onSucceeded(reqId, job)
    } catch (e) {
      stopPolling(reqId)
      error.value = e.message
    }
  }, POLL_MS)
}

async function onSucceeded(reqId, job) {
  const { created, skipped } = job.result
  results[reqId] = {
    ...job.result,
    message: created.length
      ? `추천으로 검토대기(DRAFT) 테스트케이스 ${created.length}건을 만들었습니다${skipped ? ` (이미 있는 ${skipped}건 제외)` : ''}. 검토 후 승인하세요.`
      : `새로 만들 추천이 없습니다${skipped ? ` — 같은 추천 TC ${skipped}건이 이미 있습니다` : ''}.`,
  }
  await load() // 커버리지·연결 TC 수 갱신
  if (expanded.value?.id === reqId) expanded.value = await requirementApi.get(reqId) // 원자별 커버 TC 목록 갱신
}

async function recommend(reqId) {
  error.value = ''
  delete results[reqId]
  try {
    const job = await requirementApi.recommend(reqId)
    jobs[reqId] = job
    poll(reqId, job.id)
  } catch (e) {
    error.value = e.message
  }
}

// 화면 진입/새로고침 시: 진행 중이던 잡은 폴링 재개, 마지막 잡이 실패면 실패 뱃지 복원
async function restoreJobs() {
  await Promise.all(
    requirements.value.map(async (r) => {
      const job = await recommendationJobApi.latest(r.id).catch(() => null)
      if (!job || job.status === 'SUCCEEDED') return
      jobs[r.id] = job
      if (isActive(job)) poll(r.id, job.id)
    }),
  )
}

const range = (a) => {
  if (a.minValue == null && a.maxValue == null) return '-'
  return `${a.minValue?.toLocaleString() ?? ''} ~ ${a.maxValue?.toLocaleString() ?? ''} ${a.unit ?? ''}`
}

// ?req=<requirementId> 로 진입하면 해당 요구사항을 펼침 (TC 상세 → 요구사항 Traceability)
watch(projectId, async (next, prev) => {
  expanded.value = null
  stopAllPolling()
  Object.keys(jobs).forEach((k) => delete jobs[k])
  Object.keys(results).forEach((k) => delete results[k])
  await load()
  restoreJobs()
  const reqId = Number(route.query.req)
  const target = !prev && reqId ? requirements.value.find((r) => r.id === reqId) : null
  if (target) toggle(target)
}, { immediate: true })

onBeforeUnmount(stopAllPolling)

onMounted(async () => {
  const rules = await ruleCatalogApi.list().catch(() => [])
  rulesByType.value = Object.groupBy
    ? Object.groupBy(rules, (r) => r.requirementType)
    : rules.reduce((acc, r) => ((acc[r.requirementType] ??= []).push(r), acc), {})
  await loadLlmSettings()
})

// AI 생성(LLM) 추천 on/off 토글 — 서버 메모리에만 저장되고, 재기동하면 app.ai.llm.enabled(설정 파일) 값으로 되돌아감
const llmEnabled = ref(false)
const llmProvider = ref('')
const llmToggling = ref(false)

async function loadLlmSettings() {
  try {
    const s = await llmSettingsApi.get()
    llmEnabled.value = s.enabled
    llmProvider.value = s.provider
  } catch {
    // 토글 조회 실패는 부가 기능이라 조용히 무시 — 버튼이 꺼진 상태로만 보임
  }
}

async function toggleLlm(next) {
  llmToggling.value = true
  error.value = ''
  try {
    const s = await llmSettingsApi.update(next)
    llmEnabled.value = s.enabled
  } catch (e) {
    error.value = e.message
  } finally {
    llmToggling.value = false
  }
}
</script>

<template>
  <RepoTabs />
  <div class="toolbar">
    <label class="llm-toggle" :title="`서버 재기동하면 기본값으로 돌아갑니다 (provider: ${llmProvider || '-'})`">
      <ToggleSwitch v-model="llmEnabled" :disabled="llmToggling" @update:model-value="toggleLlm" />
      <span>AI 생성(LLM) 추천 사용<span class="muted small"> — provider: {{ llmProvider || '-' }}</span></span>
    </label>
    <div class="page-actions">
      <button class="btn btn-primary" :disabled="!projectId" @click="showForm = !showForm">+ 요구사항 등록</button>
    </div>
  </div>
  <p v-if="error" class="error-text">{{ error }}</p>

  <form v-if="showForm" class="card create-form" @submit.prevent="create">
    <div class="row">
      <div class="grow">
        <label class="label required">제목</label>
        <input v-model="form.title" class="input" maxlength="200" required />
      </div>
      <div>
        <label class="label">우선순위</label>
        <select v-model="form.priority" class="select">
          <option v-for="(label, key) in PRIORITY" :key="key" :value="key">{{ label }}</option>
        </select>
      </div>
    </div>
    <label class="label required">요구사항 원문</label>
    <textarea
      v-model="form.description"
      class="textarea"
      rows="4"
      required
      placeholder="예: 가입금액은 최소 1만원, 최대 300만원. 거치기간 1/2/3년에 따라 금리 2.5~3.9%"
    />
    <div class="form-actions">
      <button type="button" class="btn" @click="showForm = false">취소</button>
      <button class="btn btn-primary">등록</button>
    </div>
  </form>

  <section class="card">
    <table class="table">
      <thead>
        <tr>
          <th style="width: 90px">코드</th>
          <th>제목</th>
          <th style="width: 80px">우선순위</th>
          <th style="width: 110px">커버리지</th>
          <th style="width: 70px">연결 TC</th>
          <th style="width: 70px">등록경로</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="r in requirements" :key="r.id">
          <tr class="clickable" :class="{ selected: expanded?.id === r.id }" @click="toggle(r)">
            <td class="mono">{{ r.reqCode }}</td>
            <td>
              {{ r.title }}
              <RecommendationStatusBadge :status="jobs[r.id]?.status" :error-message="jobs[r.id]?.errorMessage" @retry="recommend(r.id)" />
            </td>
            <td><PriorityChip :priority="r.priority" /></td>
            <td>
              <span :class="r.atomicCount && r.coveredAtomicCount === r.atomicCount ? 'covered' : 'uncovered-text'">
                {{ r.coveredAtomicCount }} / {{ r.atomicCount }}
              </span>
              <span class="muted small"> 원자</span>
            </td>
            <td>{{ r.testCaseCount }}</td>
            <td class="muted small">{{ r.source }}</td>
          </tr>
          <tr v-if="expanded?.id === r.id" class="detail-row">
            <td colspan="6">
              <div class="detail">
                <div class="detail-head">
                  <div class="label">원문</div>
                  <button class="btn btn-sm btn-primary" :disabled="isActive(jobs[r.id])" title="규칙기반(규칙 카탈로그) + RAG(다른 프로젝트의 승인된 유사 TC) + AI 생성(Claude)" @click="recommend(r.id)">
                    ✨ AI 추천 요청
                  </button>
                </div>
                <p class="pre raw">{{ expanded.description }}</p>
                <div v-if="results[r.id]" class="message">
                  {{ results[r.id].message }}
                  <ul v-if="results[r.id].created.length" class="created">
                    <li v-for="t in results[r.id].created" :key="t.id">
                      <RouterLink :to="`/test-cases/${t.id}?tab=dataset`" class="mono">{{ t.tcCode }}</RouterLink>
                      {{ t.title }}
                      <LabelChip :map="TC_SOURCE" :value="t.source" />
                      <span v-if="t.datasets.length" class="chip chip-accent">🔢 {{ t.datasets.length }}</span>
                      <span v-if="results[r.id].scores?.[t.id] != null" class="muted">
                        유사도 {{ Math.round(results[r.id].scores[t.id] * 100) }}% · 출처 {{ t.originProjectName }}
                      </span>
                    </li>
                  </ul>
                  <ul v-if="results[r.id].warnings.length" class="warnings">
                    <li v-for="w in results[r.id].warnings" :key="w">⚠ {{ w }}</li>
                  </ul>
                </div>

                <div class="label">원자 요구사항 ({{ expanded.atomics.length }})</div>
                <table v-if="expanded.atomics.length" class="table inner">
                  <thead>
                    <tr>
                      <th style="width: 90px">유형</th>
                      <th>내용</th>
                      <th style="width: 180px">범위</th>
                      <th style="width: 110px" title="규칙 카탈로그 — 마우스를 올리면 템플릿 표시">적용 규칙</th>
                      <th style="width: 380px">커버하는 테스트케이스</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="a in expanded.atomics" :key="a.id">
                      <td><span class="chip chip-accent">{{ REQUIREMENT_TYPE[a.type] }}</span></td>
                      <td>
                        {{ a.atomicText }}
                        <RecommendationStatusBadge :status="jobs[r.id]?.status" :error-message="jobs[r.id]?.errorMessage" @retry="recommend(r.id)" />
                        <div v-if="a.conditions" class="mono muted small">{{ a.conditions }}</div>
                      </td>
                      <td class="small">{{ range(a) }}</td>
                      <td class="small">
                        <span
                          v-for="rule in rulesByType[a.type] ?? []"
                          :key="rule.id"
                          class="chip chip-muted rule"
                          :title="rule.template"
                        >
                          {{ TECHNIQUE[rule.technique] }}
                        </span>
                      </td>
                      <td>
                        <ul v-if="a.testCases.length" class="covering">
                          <li v-for="tc in a.testCases" :key="tc.id">
                            <RouterLink :to="`/test-cases/${tc.id}`" class="mono">{{ tc.tcCode }}</RouterLink>
                            <span class="covering-title" :title="tc.title">{{ tc.title }}</span>
                            <LabelChip :map="REVIEW_STATUS" :value="tc.reviewStatus" />
                          </li>
                        </ul>
                        <span v-else class="chip chip-high">미커버</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <p v-else class="muted small">
                  아직 분해된 원자 요구사항이 없습니다. (AI 에이전트 연동 후 자동 분해 예정)
                </p>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
    <div v-if="!requirements.length" class="empty">등록된 요구사항이 없습니다.</div>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
.toolbar .page-actions {
  margin-bottom: 0;
}
.llm-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  cursor: pointer;
}
.create-form {
  margin-bottom: var(--space-4);
}
.row {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}
.grow {
  flex: 1;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-3);
}
tr.selected {
  background: var(--surface-hover);
}
.detail-row > td {
  background: var(--surface-page);
}
.detail {
  padding: var(--space-2) var(--space-3);
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.raw {
  margin: 0 0 var(--space-4);
}
.pre {
  white-space: pre-wrap;
}
.message {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--accent);
  background: var(--accent-soft);
  font-size: var(--font-size-sm);
}
.table.inner {
  background: var(--surface-card);
  border-radius: var(--radius-md);
}
.created,
.warnings {
  margin: var(--space-2) 0 0;
  padding-left: var(--space-4);
}
.created li,
.warnings li {
  margin-top: var(--space-1);
}
.warnings {
  color: var(--badge-progress-text);
}
.rule {
  cursor: help;
}
.covered {
  color: var(--result-success-text);
  font-weight: 600;
}
.uncovered-text {
  color: var(--result-fail-text);
  font-weight: 600;
}
.covering {
  margin: 0;
  padding: 0;
  list-style: none;
}
.covering li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-xs);
}
.covering li + li {
  margin-top: var(--space-1);
}
.covering-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.small {
  font-size: var(--font-size-xs);
}
</style>
