<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { cycleApi } from '@/api/cycles'
import { storeToRefs } from 'pinia'
import { useProjectStore } from '@/stores/projectStore'
import { progressRate } from '@/constants/labels'
import ProgressBar from '@/components/ProgressBar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import StatCard from '@/components/StatCard.vue'

const router = useRouter()
const { currentProjectId: projectId } = storeToRefs(useProjectStore())

const cycles = ref([])
const error = ref('')

// 프로젝트 전체 요약 — 이미 불러온 cycles(차수별 결과 집계 포함)로 클라이언트에서 계산, 별도 API 호출 없음
const stats = computed(() => {
  const inProgress = cycles.value.filter((c) => c.status === 'IN_PROGRESS').length
  const planned = cycles.value.filter((c) => c.status === 'PLANNED').length
  const closed = cycles.value.filter((c) => c.status === 'CLOSED').length
  const executed = cycles.value.reduce((sum, c) => sum + (c.totalCount - c.notRunCount), 0)
  const passed = cycles.value.reduce((sum, c) => sum + c.passCount, 0)
  return [
    { title: '전체 차수', value: cycles.value.length, unit: '개' },
    { title: '진행중', value: inProgress, unit: '개', sub: `계획 ${planned}개` },
    { title: '종료', value: closed, unit: '개' },
    {
      title: '전체 통과율',
      value: executed ? Math.round((passed / executed) * 100) : 0,
      unit: '%',
      sub: executed ? `${executed}건 수행` : '수행 이력 없음',
    },
  ]
})
const showForm = ref(false)
const form = reactive({ name: '', startDate: '', endDate: '' })

async function load() {
  if (!projectId.value) return
  error.value = ''
  try {
    cycles.value = await cycleApi.list(projectId.value)
  } catch (e) {
    error.value = e.message
  }
}

async function create() {
  error.value = ''
  try {
    const cycle = await cycleApi.create({
      projectId: projectId.value,
      name: form.name,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
    })
    router.push(`/cycles/${cycle.id}`)
  } catch (e) {
    error.value = e.message
  }
}


watch(projectId, load, { immediate: true })
</script>

<template>
  <div class="page-actions">
    <button class="btn btn-primary" :disabled="!projectId" @click="showForm = !showForm">+ 새 차수</button>
  </div>
  <p v-if="error" class="error-text">{{ error }}</p>

  <section v-if="cycles.length" class="stat-row">
    <StatCard v-for="s in stats" :key="s.title" v-bind="s" />
  </section>

  <form v-if="showForm" class="card create-form" @submit.prevent="create">
    <div class="field name">
      <label class="label required">차수명</label>
      <input v-model="form.name" class="input" maxlength="100" placeholder="예: 통합테스트 1차" required />
    </div>
    <div class="field">
      <label class="label">시작일</label>
      <input v-model="form.startDate" type="date" class="input" />
    </div>
    <div class="field">
      <label class="label">종료일</label>
      <input v-model="form.endDate" type="date" class="input" />
    </div>
    <button type="button" class="btn" @click="showForm = false">취소</button>
    <button class="btn btn-primary">생성</button>
  </form>

  <div class="cycle-grid">
    <article
      v-for="c in cycles"
      :key="c.id"
      class="card cycle-card"
      @click="router.push(`/cycles/${c.id}`)"
    >
      <div class="cycle-head">
        <span class="cycle-no">{{ c.cycleNo }}차</span>
        <StatusBadge :status="c.status" />
      </div>
      <h3 class="cycle-name">{{ c.name }}</h3>
      <p class="muted period">{{ c.startDate ?? '미정' }} ~ {{ c.endDate ?? '미정' }}</p>

      <div class="progress-row">
        <span class="rate">{{ progressRate(c) }}%</span>
        <span class="muted">{{ c.totalCount - c.notRunCount }} / {{ c.totalCount }} 수행</span>
      </div>
      <ProgressBar :stats="c" />
      <div class="counts">
        <span class="c-pass">성공 {{ c.passCount }}</span>
        <span class="c-fail">실패 {{ c.failCount }}</span>
        <span class="c-blocked">Block {{ c.blockedCount }}</span>
        <span class="c-notrun">미수행 {{ c.notRunCount }}</span>
      </div>
    </article>
  </div>
  <div v-if="!cycles.length" class="card empty">등록된 차수가 없습니다. ‘새 차수’로 시작하세요.</div>
</template>

<style scoped>
.create-form {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
.field.name {
  flex: 1;
}
.field .input[type='date'] {
  width: 160px;
}
.cycle-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}
.cycle-card {
  cursor: pointer;
  transition: border-color var(--transition);
}
.cycle-card:hover {
  border-color: var(--accent);
}
.cycle-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cycle-no {
  color: var(--accent);
  font-weight: 700;
}
.cycle-name {
  margin-top: var(--space-2);
  font-size: var(--font-size-lg);
}
.period {
  margin: var(--space-1) 0 var(--space-4);
  font-size: var(--font-size-sm);
}
.progress-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: var(--space-2);
}
.rate {
  font-size: var(--font-size-xl);
  font-weight: 700;
}
.counts {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.c-pass { color: var(--result-success-text); }
.c-fail { color: var(--result-fail-text); }
.c-blocked { color: var(--result-block-text); }
.c-notrun { color: var(--result-notrun-text); }
</style>
