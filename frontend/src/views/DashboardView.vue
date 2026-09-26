<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi } from '@/api/dashboard'
import { useProject } from '@/composables/useProject'
import { DEFECT_STATUS, SEVERITY, CYCLE_STATUS, formatDateTime, progressRate } from '@/constants/labels'
import KpiCard from '@/components/KpiCard.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import PriorityChip from '@/components/PriorityChip.vue'
import LabelChip from '@/components/LabelChip.vue'

const router = useRouter()
const { projectId } = useProject()
const data = ref(null)
const error = ref('')

async function load() {
  if (!projectId.value) return
  error.value = ''
  try {
    data.value = await dashboardApi.summary(projectId.value)
  } catch (e) {
    error.value = e.message
  }
}
watch(projectId, load, { immediate: true })

const cycle = computed(() => data.value?.currentCycle)
const kpis = computed(() => {
  const d = data.value
  if (!d) return []
  const c = cycle.value
  return [
    {
      label: '내 할일',
      value: d.myPendingExecutionCount + d.myOpenDefectCount,
      sub: `미수행 TC ${d.myPendingExecutionCount} · 담당 결함 ${d.myOpenDefectCount}`,
    },
    {
      label: '이번 차수 진행률',
      value: c ? `${progressRate(c)}%` : '-',
      sub: c ? `${c.cycleNo}차 ${c.name} · ${c.totalCount - c.notRunCount}/${c.totalCount} 수행` : '진행 중인 차수 없음',
    },
    {
      label: '미해결 결함',
      value: d.unresolvedDefectCount,
      sub: `치명 ${d.criticalDefectCount}건`,
    },
    {
      label: '실패 TC',
      value: c ? c.failCount : '-',
      sub: c ? `Block ${c.blockedCount}건 · 이번 차수` : '진행 중인 차수 없음',
    },
  ]
})

const openExecution = (e) => router.push({ path: `/cycles/${e.cycleId}`, query: { exec: e.id } })
</script>

<template>
  <p v-if="error" class="error-text">{{ error }}</p>

  <template v-if="data">
    <section class="kpi-row">
      <KpiCard v-for="k in kpis" :key="k.label" v-bind="k" />
    </section>

    <section v-if="cycle" class="card cycle-card clickable-card" @click="router.push(`/cycles/${cycle.id}`)">
      <div class="cycle-head">
        <div>
          <span class="cycle-no">{{ cycle.cycleNo }}차</span>
          <strong>{{ cycle.name }}</strong>
          <span class="chip chip-medium">{{ CYCLE_STATUS[cycle.status] }}</span>
        </div>
        <div class="legend">
          <span class="c-pass">성공 {{ cycle.passCount }}</span>
          <span class="c-fail">실패 {{ cycle.failCount }}</span>
          <span class="c-blocked">Block {{ cycle.blockedCount }}</span>
          <span class="c-notrun">미수행 {{ cycle.notRunCount }}</span>
        </div>
      </div>
      <ProgressBar :stats="cycle" />
    </section>

    <div class="lists">
      <section class="card">
        <div class="list-head">
          <div class="card-title">내 미수행 테스트 ({{ data.myPendingExecutionCount }})</div>
          <RouterLink to="/cycles" class="small">수행관리 →</RouterLink>
        </div>
        <table v-if="data.myExecutions.length" class="table">
          <tbody>
            <tr v-for="e in data.myExecutions" :key="e.id" class="clickable" @click="openExecution(e)">
              <td class="mono nowrap">{{ e.tcCode }}</td>
              <td>{{ e.tcTitle }}</td>
              <td class="nowrap"><PriorityChip :priority="e.tcPriority" /></td>
              <td class="muted small nowrap">{{ e.cycleNo }}차</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty">담당 미수행 테스트가 없습니다. 🎉</div>
        <p v-if="data.myPendingExecutionCount > data.myExecutions.length" class="muted small more">
          외 {{ data.myPendingExecutionCount - data.myExecutions.length }}건
        </p>
      </section>

      <section class="card">
        <div class="list-head">
          <div class="card-title">내 담당 결함 ({{ data.myOpenDefectCount }})</div>
          <RouterLink to="/defects" class="small">결함관리 →</RouterLink>
        </div>
        <table v-if="data.myDefects.length" class="table">
          <tbody>
            <tr v-for="d in data.myDefects" :key="d.id" class="clickable" @click="router.push(`/defects/${d.id}`)">
              <td class="mono nowrap">{{ d.defectCode }}</td>
              <td>{{ d.title }}</td>
              <td class="nowrap"><LabelChip :map="SEVERITY" :value="d.severity" /></td>
              <td class="nowrap"><LabelChip :map="DEFECT_STATUS" :value="d.status" /></td>
              <td class="muted small nowrap">{{ formatDateTime(d.updatedAt).slice(5, 10) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty">담당 미해결 결함이 없습니다.</div>
        <p v-if="data.myOpenDefectCount > data.myDefects.length" class="muted small more">
          외 {{ data.myOpenDefectCount - data.myDefects.length }}건
        </p>
      </section>
    </div>
  </template>
</template>

<style scoped>
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
.cycle-card {
  margin-bottom: var(--space-4);
}
.clickable-card {
  cursor: pointer;
  transition: border-color var(--transition);
}
.clickable-card:hover {
  border-color: var(--color-accent);
}
.cycle-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}
.cycle-head > div:first-child {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.cycle-no {
  color: var(--color-accent);
  font-weight: 700;
}
.legend {
  display: flex;
  gap: var(--space-3);
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.c-pass { color: var(--status-pass); }
.c-fail { color: var(--status-fail); }
.c-blocked { color: var(--status-blocked); }
.c-notrun { color: var(--status-notrun); }
.lists {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  align-items: start;
}
.list-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.small {
  font-size: var(--font-size-xs);
}
.nowrap {
  white-space: nowrap;
}
.more {
  margin: var(--space-3) 0 0;
  text-align: right;
}
</style>
