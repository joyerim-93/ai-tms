<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { testCaseApi } from '@/api/testCases'
import { cycleApi } from '@/api/cycles'
import BaseModal from '@/components/BaseModal.vue'
import PriorityChip from '@/components/PriorityChip.vue'
import UserCombo from '@/components/UserCombo.vue'
import { userApi } from '@/api/users'
import { useAuthStore } from '@/stores/authStore'

const props = defineProps({
  cycleId: { type: Number, required: true },
  projectId: { type: Number, required: true }, // 차수 소속 프로젝트 — 같은 프로젝트 TC만 등록 가능
  users: { type: Array, default: () => [] },   // 가입된 사용자 전체 (담당자 콤보) — [{ id, displayName }]
  registeredTcIds: { type: Set, default: () => new Set() },
})
const emit = defineEmits(['close', 'added'])

const filter = reactive({ keyword: '', module: '' })
const modules = ref([])
const items = ref([])
const total = ref(0)
const selected = ref(new Set())
const auth = useAuthStore()
// 담당자 기본값 = 로그인 사용자, 그 자리에서 다른 사람으로 변경(선택 또는 직접 입력) 가능
const assigneeId = ref(auth.user?.id ?? null)
const error = ref('')
const saving = ref(false)

const selectable = computed(() => items.value.filter((tc) => !props.registeredTcIds.has(tc.id)))
const allChecked = computed(
  () => selectable.value.length > 0 && selectable.value.every((tc) => selected.value.has(tc.id)),
)

async function search() {
  error.value = ''
  try {
    const res = await testCaseApi.search({ ...filter, projectId: props.projectId, status: 'ACTIVE', reviewStatus: 'APPROVED', size: 100 })
    items.value = res.items
    total.value = res.total
  } catch (e) {
    error.value = e.message
  }
}

function toggle(id) {
  const next = new Set(selected.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selected.value = next
}

function toggleAll() {
  const next = new Set(selected.value)
  selectable.value.forEach((tc) => (allChecked.value ? next.delete(tc.id) : next.add(tc.id)))
  selected.value = next
}

async function submit() {
  saving.value = true
  error.value = ''
  try {
    const { added } = await cycleApi.addExecutions(props.cycleId, [...selected.value], assigneeId.value)
    emit('added', added)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  search()
  modules.value = await testCaseApi.modules(props.projectId).catch(() => [])
})
</script>

<template>
  <BaseModal title="테스트케이스 추가" width="860px" @close="emit('close')">
    <form class="filters" @submit.prevent="search">
      <input v-model="filter.keyword" class="input" placeholder="코드 / 제목 / 태그" />
      <select v-model="filter.module" class="select module">
        <option value="">전체 모듈</option>
        <option v-for="m in modules" :key="m" :value="m">{{ m }}</option>
      </select>
      <button class="btn btn-primary">검색</button>
    </form>
    <p class="muted hint">사용 중이며 검토 승인된 TC만 표시됩니다. 최대 100건{{ total > 100 ? ` / 전체 ${total}건 — 검색어로 좁혀 주세요` : '' }}</p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <table class="table">
      <thead>
        <tr>
          <th style="width: 36px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
          <th style="width: 110px">코드</th>
          <th>제목</th>
          <th style="width: 120px">모듈</th>
          <th style="width: 80px">우선순위</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="tc in items"
          :key="tc.id"
          :class="registeredTcIds.has(tc.id) ? 'registered' : 'clickable'"
          @click="!registeredTcIds.has(tc.id) && toggle(tc.id)"
        >
          <td>
            <input
              type="checkbox"
              :checked="selected.has(tc.id) || registeredTcIds.has(tc.id)"
              :disabled="registeredTcIds.has(tc.id)"
              @click.stop
              @change="toggle(tc.id)"
            />
          </td>
          <td class="mono">{{ tc.tcCode }}</td>
          <td>
            {{ tc.title }}
            <span v-if="registeredTcIds.has(tc.id)" class="chip chip-muted">등록됨</span>
          </td>
          <td>{{ tc.module ?? '-' }}</td>
          <td><PriorityChip :priority="tc.priority" /></td>
        </tr>
      </tbody>
    </table>
    <div v-if="!items.length" class="empty">검색 결과가 없습니다.</div>

    <template #footer>
      <label class="assignee">
        담당자
        <UserCombo v-model="assigneeId" :users="users" placeholder="미지정" />
      </label>
      <div>
        <button class="btn" @click="emit('close')">취소</button>
        <button class="btn btn-primary" :disabled="!selected.size || saving" @click="submit">
          {{ selected.size }}건 추가
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
.filters {
  display: flex;
  gap: var(--space-2);
}
.filters .module {
  width: 160px;
}
.hint {
  margin: var(--space-2) 0;
  font-size: var(--font-size-xs);
}
.registered {
  color: var(--text-muted);
}
.assignee {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-secondary);
  white-space: nowrap;
}
.assignee .select {
  width: 180px;
}
.modal-footer .btn + .btn {
  margin-left: var(--space-2);
}
</style>
