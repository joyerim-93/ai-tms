<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { testCaseApi } from '@/api/testCases'
import { TC_STATUS, formatDateTime } from '@/constants/labels'
import PriorityChip from '@/components/PriorityChip.vue'

const route = useRoute()
const router = useRouter()
const tc = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    tc.value = await testCaseApi.get(route.params.id)
  } catch (e) {
    error.value = e.message
  }
})

async function remove() {
  if (!window.confirm(`${tc.value.tcCode} 테스트케이스를 삭제할까요?`)) return
  try {
    await testCaseApi.remove(tc.value.id)
    router.push('/test-cases')
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <div class="page-actions">
    <button class="btn" @click="router.push('/test-cases')">목록</button>
    <template v-if="tc">
      <button class="btn btn-danger" @click="remove">삭제</button>
      <button class="btn btn-primary" @click="router.push(`/test-cases/${tc.id}/edit`)">수정</button>
    </template>
  </div>
  <p v-if="error" class="error-text">{{ error }}</p>

  <template v-if="tc">
    <section class="card summary">
      <div class="title-row">
        <span class="mono muted">{{ tc.tcCode }}</span>
        <PriorityChip :priority="tc.priority" />
        <span class="chip" :class="tc.status === 'ACTIVE' ? 'chip-low' : 'chip-muted'">
          {{ TC_STATUS[tc.status] }}
        </span>
      </div>
      <h2 class="title">{{ tc.title }}</h2>
      <dl class="meta">
        <div><dt>모듈</dt><dd>{{ tc.module ?? '-' }}</dd></div>
        <div><dt>태그</dt><dd>{{ tc.tags ?? '-' }}</dd></div>
        <div><dt>작성자</dt><dd>{{ tc.authorName ?? '-' }}</dd></div>
        <div><dt>버전</dt><dd>v{{ tc.version }}</dd></div>
        <div><dt>수정일</dt><dd>{{ formatDateTime(tc.updatedAt) }}</dd></div>
      </dl>
      <div v-if="tc.precondition" class="precondition">
        <div class="label">사전조건</div>
        <p class="pre">{{ tc.precondition }}</p>
      </div>
    </section>

    <section class="card">
      <div class="card-title">테스트 단계 ({{ tc.steps.length }})</div>
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
            <td class="pre">{{ s.action }}</td>
            <td class="pre">{{ s.expectedResult ?? '-' }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">등록된 단계가 없습니다.</div>
    </section>
  </template>
</template>

<style scoped>
.summary {
  margin-bottom: var(--space-4);
}
.title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.title {
  margin: var(--space-2) 0 var(--space-4);
  font-size: var(--font-size-xl);
}
.meta {
  display: flex;
  gap: var(--space-6);
  margin: 0;
}
.meta dt {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
.meta dd {
  margin: var(--space-1) 0 0;
}
.precondition {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
}
.pre {
  margin: 0;
  white-space: pre-wrap;
}
</style>
