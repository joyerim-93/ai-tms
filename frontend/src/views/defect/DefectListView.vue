<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { defectApi } from '@/api/defects'
import { projectApi } from '@/api/projects'
import { useProject } from '@/composables/useProject'
import { DEFECT_STATUS, SEVERITY, formatDateTime } from '@/constants/labels'
import LabelChip from '@/components/LabelChip.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import PriorityChip from '@/components/PriorityChip.vue'

const router = useRouter()
const { projectId } = useProject()

// status: '' 전체 | 'UNRESOLVED' 미해결 묶음 | 개별 상태
const filter = reactive({ keyword: '', status: 'UNRESOLVED', severity: '', assigneeId: '' })
const page = ref(1)
const size = 20
const result = ref({ items: [], total: 0 })
const members = ref([])
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(result.value.total / size)))

async function load(p = 1) {
  if (!projectId.value) return
  page.value = p
  error.value = ''
  const { status, ...rest } = filter
  try {
    result.value = await defectApi.search({
      ...rest,
      projectId: projectId.value,
      status: status === 'UNRESOLVED' ? '' : status,
      unresolved: status === 'UNRESOLVED',
      page: p,
      size,
    })
  } catch (e) {
    error.value = e.message
  }
}

watch(
  projectId,
  async (id) => {
    if (!id) return
    load()
    members.value = await projectApi.members(id).catch(() => [])
  },
  { immediate: true },
)
</script>

<template>
  <div class="page-actions">
    <button class="btn btn-primary" :disabled="!projectId" @click="router.push('/defects/new')">+ 이슈 등록</button>
  </div>

  <form class="card filters" @submit.prevent="load()">
    <input v-model="filter.keyword" class="input keyword" placeholder="이슈 코드 / 제목" />
    <select v-model="filter.status" class="select" @change="load()">
      <option value="UNRESOLVED">미해결 (신규·열림·진행중)</option>
      <option value="">전체 상태</option>
      <option v-for="(label, key) in DEFECT_STATUS" :key="key" :value="key">{{ label }}</option>
    </select>
    <select v-model="filter.severity" class="select" @change="load()">
      <option value="">전체 심각도</option>
      <option v-for="(s, key) in SEVERITY" :key="key" :value="key">{{ s.label }}</option>
    </select>
    <select v-model="filter.assigneeId" class="select" @change="load()">
      <option value="">전체 담당자</option>
      <option v-for="m in members" :key="m.userId" :value="m.userId">{{ m.name }}</option>
    </select>
    <button class="btn btn-primary">검색</button>
  </form>

  <section class="card">
    <div class="list-header">총 <strong>{{ result.total }}</strong>건</div>
    <p v-if="error" class="error-text">{{ error }}</p>

    <table class="table">
      <thead>
        <tr>
          <th style="width: 90px">코드</th>
          <th>제목</th>
          <th style="width: 70px">심각도</th>
          <th style="width: 80px">우선순위</th>
          <th style="width: 80px">상태</th>
          <th style="width: 90px">담당자</th>
          <th style="width: 90px">보고자</th>
          <th style="width: 150px">연결 TC</th>
          <th style="width: 140px">수정일</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in result.items" :key="d.id" class="clickable" @click="router.push(`/defects/${d.id}`)">
          <td class="mono">{{ d.defectCode }}</td>
          <td>{{ d.title }}</td>
          <td><LabelChip :map="SEVERITY" :value="d.severity" /></td>
          <td><PriorityChip :priority="d.priority" /></td>
          <td><StatusBadge :status="d.status" /></td>
          <td>{{ d.assigneeName ?? '-' }}</td>
          <td>{{ d.reporterName }}</td>
          <td class="small">
            <template v-if="d.tcCode"><span class="mono">{{ d.tcCode }}</span> · {{ d.cycleNo }}차</template>
            <span v-else class="muted">-</span>
          </td>
          <td class="muted small">{{ formatDateTime(d.updatedAt) }}</td>
        </tr>
      </tbody>
    </table>
    <div v-if="!result.items.length" class="empty">조건에 맞는 이슈가 없습니다.</div>

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
.filters .keyword {
  flex: 1;
}
.filters .select {
  width: 190px;
}
.list-header {
  margin-bottom: var(--space-3);
  color: var(--text-secondary);
}
.small {
  font-size: var(--font-size-xs);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
