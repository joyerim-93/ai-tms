<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { testCaseApi } from '@/api/testCases'
import { TC_STATUS, TC_SOURCE, REVIEW_STATUS, TECHNIQUE, formatDateTime } from '@/constants/labels'
import PriorityChip from '@/components/PriorityChip.vue'
import LabelChip from '@/components/LabelChip.vue'

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

// 사람이 직접 작성한 TC(MANUAL)는 검토 대상 아님
async function review(reviewStatus) {
  error.value = ''
  try {
    tc.value = await testCaseApi.review(tc.value.id, reviewStatus)
  } catch (e) {
    error.value = e.message
  }
}

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
    <button class="btn" @click="router.push({ path: '/test-cases', query: tc?.folderId ? { folder: tc.folderId } : {} })">목록</button>
    <template v-if="tc">
      <button class="btn btn-danger" @click="remove">삭제</button>
      <button class="btn btn-primary" @click="router.push(`/test-cases/${tc.id}/edit`)">수정</button>
    </template>
  </div>
  <p v-if="error" class="error-text">{{ error }}</p>

  <template v-if="tc">
    <section v-if="tc.source !== 'MANUAL'" class="card review-bar" :class="`review-${tc.reviewStatus.toLowerCase()}`">
      <div>
        <LabelChip :map="TC_SOURCE" :value="tc.source" />
        <LabelChip :map="REVIEW_STATUS" :value="tc.reviewStatus" />
        <span v-if="tc.reviewStatus === 'DRAFT'" class="muted">AI 추천 TC입니다. 검토 후 승인해야 차수에 등록할 수 있습니다.</span>
        <span v-else class="muted">{{ tc.reviewedByName ?? '-' }} · {{ formatDateTime(tc.reviewedAt) }}</span>
      </div>
      <div class="review-buttons">
        <button v-if="tc.reviewStatus !== 'APPROVED'" class="btn btn-sm" @click="review('APPROVED')">승인</button>
        <button v-if="tc.reviewStatus !== 'REJECTED'" class="btn btn-sm btn-danger" @click="review('REJECTED')">반려</button>
      </div>
    </section>

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
        <div><dt>프로젝트</dt><dd>{{ tc.projectName }}</dd></div>
        <div><dt>폴더</dt><dd>{{ tc.folderName ?? '미분류' }}</dd></div>
        <div v-if="tc.originProjectName"><dt>원본 프로젝트</dt><dd>{{ tc.originProjectName }}</dd></div>
        <div><dt>모듈</dt><dd>{{ tc.module ?? '-' }}</dd></div>
        <div><dt>태그</dt><dd>{{ tc.tags ?? '-' }}</dd></div>
        <div><dt>작성자</dt><dd>{{ tc.authorName ?? '시스템/AI' }}</dd></div>
        <div><dt>테스트 기법</dt><dd>{{ TECHNIQUE[tc.technique] ?? '-' }}</dd></div>
        <div><dt>버전</dt><dd>v{{ tc.version }}</dd></div>
        <div><dt>수정일</dt><dd>{{ formatDateTime(tc.updatedAt) }}</dd></div>
      </dl>
      <div v-if="tc.atomicText" class="precondition">
        <div class="label">근거 요구사항</div>
        <p class="pre">
          <RouterLink to="/test-cases/requirements" class="mono">{{ tc.reqCode }}</RouterLink>
          {{ tc.atomicText }}
        </p>
      </div>
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
.review-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-5);
}
.review-bar > div:first-child {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.review-draft {
  border-left: 3px solid var(--badge-progress-text);
}
.review-buttons {
  display: flex;
  gap: var(--space-2);
}
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
