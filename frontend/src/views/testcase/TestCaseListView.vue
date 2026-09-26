<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { testCaseApi } from '@/api/testCases'
import { PRIORITY, TC_STATUS, formatDateTime } from '@/constants/labels'
import PriorityChip from '@/components/PriorityChip.vue'

const router = useRouter()

const filter = reactive({ keyword: '', module: '', priority: '', status: 'ACTIVE' })
const page = ref(1)
const size = 20
const result = ref({ items: [], total: 0 })
const modules = ref([])
const loading = ref(false)
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(result.value.total / size)))

async function load(p = 1) {
  page.value = p
  loading.value = true
  error.value = ''
  try {
    result.value = await testCaseApi.search({ ...filter, page: p, size })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(filter, { keyword: '', module: '', priority: '', status: 'ACTIVE' })
  load()
}

onMounted(async () => {
  load()
  modules.value = await testCaseApi.modules().catch(() => [])
})
</script>

<template>
  <div class="page-actions">
    <button class="btn btn-primary" @click="router.push('/test-cases/new')">+ 새 테스트케이스</button>
  </div>

  <form class="card filters" @submit.prevent="load()">
    <input v-model="filter.keyword" class="input keyword" placeholder="코드 / 제목 / 태그 검색" />
    <select v-model="filter.module" class="select">
      <option value="">전체 모듈</option>
      <option v-for="m in modules" :key="m" :value="m">{{ m }}</option>
    </select>
    <select v-model="filter.priority" class="select">
      <option value="">전체 우선순위</option>
      <option v-for="(label, key) in PRIORITY" :key="key" :value="key">{{ label }}</option>
    </select>
    <select v-model="filter.status" class="select">
      <option value="">전체 상태</option>
      <option v-for="(label, key) in TC_STATUS" :key="key" :value="key">{{ label }}</option>
    </select>
    <button type="submit" class="btn btn-primary">검색</button>
    <button type="button" class="btn" @click="reset">초기화</button>
  </form>

  <section class="card">
    <div class="list-header">
      <span>총 <strong>{{ result.total }}</strong>건</span>
      <span v-if="loading" class="muted">불러오는 중…</span>
    </div>
    <p v-if="error" class="error-text">{{ error }}</p>

    <table class="table">
      <thead>
        <tr>
          <th style="width: 110px">코드</th>
          <th>제목</th>
          <th style="width: 130px">모듈</th>
          <th style="width: 80px">우선순위</th>
          <th style="width: 60px">단계</th>
          <th style="width: 60px">버전</th>
          <th style="width: 90px">작성자</th>
          <th style="width: 140px">수정일</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="tc in result.items"
          :key="tc.id"
          class="clickable"
          @click="router.push(`/test-cases/${tc.id}`)"
        >
          <td class="mono">{{ tc.tcCode }}</td>
          <td>
            {{ tc.title }}
            <span v-if="tc.status === 'DEPRECATED'" class="chip chip-muted">폐기</span>
          </td>
          <td>{{ tc.module ?? '-' }}</td>
          <td><PriorityChip :priority="tc.priority" /></td>
          <td>{{ tc.stepCount }}</td>
          <td>v{{ tc.version }}</td>
          <td>{{ tc.authorName ?? '-' }}</td>
          <td class="muted">{{ formatDateTime(tc.updatedAt) }}</td>
        </tr>
      </tbody>
    </table>
    <div v-if="!loading && !result.items.length" class="empty">조건에 맞는 테스트케이스가 없습니다.</div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="btn btn-sm" :disabled="page <= 1" @click="load(page - 1)">이전</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button class="btn btn-sm" :disabled="page >= totalPages" @click="load(page + 1)">다음</button>
    </div>
  </section>
</template>

<style scoped>
.filters {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  padding: var(--space-4);
}
.filters .select {
  width: 150px;
}
.filters .keyword {
  flex: 1;
}
.list-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-3);
  color: var(--text-secondary);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
