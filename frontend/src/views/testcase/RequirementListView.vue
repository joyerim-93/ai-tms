<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { requirementApi, ruleCatalogApi } from '@/api/requirements'
import { storeToRefs } from 'pinia'
import { useProjectStore } from '@/stores/projectStore'
import { PRIORITY, REQUIREMENT_TYPE, TECHNIQUE } from '@/constants/labels'
import PriorityChip from '@/components/PriorityChip.vue'
import RepoTabs from './RepoTabs.vue'

// 요구사항 원문 → 원자 요구사항(AI 분해) → 규칙/RAG/LLM 추천 TC 흐름의 입구
const { currentProjectId: projectId } = storeToRefs(useProjectStore())

const requirements = ref([])
const expanded = ref(null)       // 펼친 요구사항 상세 (atomics 포함)
const rulesByType = ref({})
const showForm = ref(false)
const form = reactive({ title: '', priority: 'MEDIUM', description: '' })
const error = ref('')
const message = ref('')

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
  message.value = ''
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

async function recommend(id) {
  error.value = ''
  message.value = ''
  try {
    const list = await requirementApi.recommend(id)
    message.value = list.length
      ? `추천 결과 ${list.length}건`
      : 'AI 추천 엔진이 아직 연동되지 않았습니다 (추천 0건). 에이전트 연동 후 DRAFT TC로 생성됩니다.'
  } catch (e) {
    error.value = e.message
  }
}

const range = (a) => {
  if (a.minValue == null && a.maxValue == null) return '-'
  return `${a.minValue?.toLocaleString() ?? ''} ~ ${a.maxValue?.toLocaleString() ?? ''} ${a.unit ?? ''}`
}

watch(projectId, () => {
  expanded.value = null
  load()
}, { immediate: true })

onMounted(async () => {
  const rules = await ruleCatalogApi.list().catch(() => [])
  rulesByType.value = Object.groupBy
    ? Object.groupBy(rules, (r) => r.requirementType)
    : rules.reduce((acc, r) => ((acc[r.requirementType] ??= []).push(r), acc), {})
})
</script>

<template>
  <RepoTabs />
  <div class="page-actions">
    <button class="btn btn-primary" :disabled="!projectId" @click="showForm = !showForm">+ 요구사항 등록</button>
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
          <th style="width: 90px">원자 요구사항</th>
          <th style="width: 70px">연결 TC</th>
          <th style="width: 70px">등록경로</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="r in requirements" :key="r.id">
          <tr class="clickable" :class="{ selected: expanded?.id === r.id }" @click="toggle(r)">
            <td class="mono">{{ r.reqCode }}</td>
            <td>{{ r.title }}</td>
            <td><PriorityChip :priority="r.priority" /></td>
            <td>{{ r.atomicCount }}</td>
            <td>{{ r.testCaseCount }}</td>
            <td class="muted small">{{ r.source }}</td>
          </tr>
          <tr v-if="expanded?.id === r.id" class="detail-row">
            <td colspan="6">
              <div class="detail">
                <div class="detail-head">
                  <div class="label">원문</div>
                  <button class="btn btn-sm btn-primary" @click="recommend(r.id)">✨ AI 추천 실행</button>
                </div>
                <p class="pre raw">{{ expanded.description }}</p>
                <p v-if="message" class="message">{{ message }}</p>

                <div class="label">원자 요구사항 ({{ expanded.atomics.length }})</div>
                <table v-if="expanded.atomics.length" class="table inner">
                  <thead>
                    <tr>
                      <th style="width: 90px">유형</th>
                      <th>내용</th>
                      <th style="width: 180px">범위</th>
                      <th>적용 규칙 (규칙 카탈로그)</th>
                      <th style="width: 60px">TC</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="a in expanded.atomics" :key="a.id">
                      <td><span class="chip chip-accent">{{ REQUIREMENT_TYPE[a.type] }}</span></td>
                      <td>
                        {{ a.atomicText }}
                        <div v-if="a.conditions" class="mono muted small">{{ a.conditions }}</div>
                      </td>
                      <td class="small">{{ range(a) }}</td>
                      <td class="small">
                        <div v-for="rule in rulesByType[a.type] ?? []" :key="rule.id">
                          <strong>{{ TECHNIQUE[rule.technique] }}</strong> — {{ rule.template }}
                        </div>
                      </td>
                      <td>
                        <RouterLink :to="`/test-cases?atomicRequirementId=${a.id}`">{{ a.testCaseCount }}건</RouterLink>
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
.small {
  font-size: var(--font-size-xs);
}
</style>
