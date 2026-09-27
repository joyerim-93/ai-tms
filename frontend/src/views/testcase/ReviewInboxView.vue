<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { testCaseApi } from '@/api/testCases'
import { useProjectStore } from '@/stores/projectStore'
import { TC_SOURCE, formatDateTime } from '@/constants/labels'
import TestCaseKeyBadge from '@/components/TestCaseKeyBadge.vue'
import LabelChip from '@/components/LabelChip.vue'
import RepoTabs from './RepoTabs.vue'

// AI 추천/엑셀 업로드로 만들어진 검토대기(DRAFT) TC를 모아서 여러 건 한 번에 승인/반려.
// 개별 행 클릭은 기존 TC 상세화면으로 이동해 1건씩 처리하는 기존 동작도 유지.
const router = useRouter()
const { currentProjectId: projectId } = storeToRefs(useProjectStore())

const filter = reactive({ keyword: '' })
const items = ref([])
const total = ref(0)
const selected = ref(new Set())
const loading = ref(false)
const error = ref('')
const message = ref('')
const processing = ref(false)

const allChecked = computed(() => items.value.length > 0 && items.value.every((tc) => selected.value.has(tc.id)))

async function load() {
  if (!projectId.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await testCaseApi.search({
      ...filter,
      projectId: projectId.value,
      reviewStatus: 'DRAFT',
      size: 100,
    })
    items.value = res.items
    total.value = res.total
    selected.value = new Set([...selected.value].filter((id) => items.value.some((tc) => tc.id === id)))
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
watch(projectId, load, { immediate: true })

function toggle(id) {
  const next = new Set(selected.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selected.value = next
}
function toggleAll() {
  selected.value = allChecked.value ? new Set() : new Set(items.value.map((tc) => tc.id))
}

async function batch(reviewStatus) {
  if (!selected.value.size) return
  const label = reviewStatus === 'APPROVED' ? '승인' : '반려'
  if (!window.confirm(`선택한 ${selected.value.size}건을 ${label}하시겠습니까?`)) return
  processing.value = true
  error.value = ''
  message.value = ''
  try {
    const { updated } = await testCaseApi.reviewBatch([...selected.value], reviewStatus)
    message.value = `${updated}건을 ${label}했습니다.`
    selected.value = new Set()
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    processing.value = false
  }
}

function goDetail(id) {
  router.push(`/test-cases/${id}`)
}
</script>

<template>
  <RepoTabs />

  <section class="card">
    <div class="list-header">
      <span>
        검토대기 <strong>{{ total }}</strong>건
        <span class="muted small">— AI 추천(규칙/RAG/LLM) 또는 엑셀 업로드로 생성된 테스트케이스</span>
      </span>
      <span v-if="loading" class="muted">불러오는 중…</span>
    </div>
    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="message" class="message">{{ message }}</p>

    <form class="filters" @submit.prevent="load">
      <input v-model="filter.keyword" class="input" placeholder="코드 / 테스트케이스명 / 태그 검색" />
      <button type="submit" class="btn btn-primary">검색</button>
    </form>

    <div class="batch-bar">
      <span class="muted small">{{ selected.size }}건 선택</span>
      <div class="batch-actions">
        <button class="btn btn-sm" :disabled="!selected.size || processing" @click="batch('APPROVED')">
          ✓ 일괄 승인
        </button>
        <button class="btn btn-sm btn-danger" :disabled="!selected.size || processing" @click="batch('REJECTED')">
          ✕ 일괄 반려
        </button>
      </div>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th style="width: 36px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
          <th style="width: 84px">Key</th>
          <th>테스트케이스명</th>
          <th style="width: 84px">출처</th>
          <th style="width: 160px">프로젝트 / 폴더</th>
          <th style="width: 140px">생성일시</th>
          <th style="width: 90px">관련 요구사항</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tc in items" :key="tc.id" class="clickable" @click="goDetail(tc.id)">
          <td @click.stop><input type="checkbox" :checked="selected.has(tc.id)" @change="toggle(tc.id)" /></td>
          <td><TestCaseKeyBadge :code="tc.tcCode" /></td>
          <td>{{ tc.title }}</td>
          <td><LabelChip :map="TC_SOURCE" :value="tc.source" /></td>
          <td class="small">{{ tc.projectName }}<span v-if="tc.folderName" class="muted"> / {{ tc.folderName }}</span></td>
          <td class="small muted">{{ formatDateTime(tc.createdAt) }}</td>
          <td class="small">
            <span v-if="tc.requirementCount">{{ tc.requirementCount }}건</span>
            <span v-else class="muted">-</span>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="!loading && !items.length" class="empty">검토 대기 중인 테스트케이스가 없습니다.</div>
  </section>
</template>

<style scoped>
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}
.small {
  font-size: var(--font-size-xs);
}
.filters {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}
.filters .input {
  max-width: 320px;
}
.batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  margin-bottom: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
}
.batch-actions {
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
</style>
