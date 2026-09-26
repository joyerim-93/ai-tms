<script setup>
import { computed, ref, watch } from 'vue'
import { dashboardApi } from '@/api/dashboard'
import { defectApi } from '@/api/defects'
import { cycleApi } from '@/api/cycles'
import { useProject } from '@/composables/useProject'
import StatCard from '@/components/StatCard.vue'
import IssueListCard from '@/components/IssueListCard.vue'
import TestRoundProgressCard from '@/components/TestRoundProgressCard.vue'

const ISSUE_LIMIT = 5
const ROUND_LIMIT = 3

const { projectId } = useProject()
const summary = ref(null)
const issues = ref([])
const rounds = ref([])
const error = ref('')

async function load() {
  if (!projectId.value) return
  error.value = ''
  try {
    const [s, d, cycles] = await Promise.all([
      dashboardApi.summary(projectId.value),
      defectApi.search({ projectId: projectId.value, size: ISSUE_LIMIT }),
      cycleApi.list(projectId.value),
    ])
    summary.value = s
    issues.value = d.items
    // 진행중 → 계획 → 종료 순, 같은 상태는 최신 차수 우선 (API가 cycle_no 내림차순)
    const order = { IN_PROGRESS: 0, PLANNED: 1, CLOSED: 2 }
    rounds.value = [...cycles].sort((a, b) => order[a.status] - order[b.status]).slice(0, ROUND_LIMIT)
  } catch (e) {
    error.value = e.message
  }
}
watch(projectId, load, { immediate: true })

const signed = (n, suffix = '') => `${n > 0 ? '+' : ''}${n}${suffix}`

const stats = computed(() => {
  const s = summary.value
  if (!s) return []
  const delta =
    s.passRate != null && s.previousPassRate != null
      ? Math.round((s.passRate - s.previousPassRate) * 10) / 10
      : null
  return [
    {
      title: '총 테스트케이스',
      value: s.totalTestCaseCount.toLocaleString(),
      unit: '건',
      sub: `${signed(s.testCasesAddedThisWeek)} 이번 주`,
    },
    {
      title: '수행 통과율',
      value: s.passRate ?? '-',
      unit: s.passRate != null ? '%' : '',
      sub:
        delta != null
          ? `${signed(delta, '%p')} 전 차수 대비`
          : s.currentCycle
            ? `${s.currentCycle.cycleNo}차 기준`
            : '진행 중인 차수 없음',
    },
    {
      title: '열린 이슈',
      value: s.openDefectCount.toLocaleString(),
      unit: '건',
      sub: `${signed(s.defectsOpenedThisWeek)} 이번 주 등록`,
    },
    {
      title: '활성 테스트 차수',
      value: s.activeCycleCount,
      unit: '개',
      sub: `${s.inProgressCycleCount}개 진행중`,
    },
  ]
})
</script>

<template>
  <p v-if="error" class="error-text">{{ error }}</p>

  <template v-if="summary">
    <section class="stat-row">
      <StatCard v-for="s in stats" :key="s.title" v-bind="s" />
    </section>

    <section class="bottom">
      <IssueListCard :issues="issues" />
      <TestRoundProgressCard :rounds="rounds" />
    </section>
  </template>
</template>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}
.bottom {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
  align-items: start;
}
</style>
