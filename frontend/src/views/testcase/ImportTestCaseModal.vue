<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { testCaseApi } from '@/api/testCases'
import { TC_SOURCE } from '@/constants/labels'
import BaseModal from '@/components/BaseModal.vue'
import LabelChip from '@/components/LabelChip.vue'
import PriorityChip from '@/components/PriorityChip.vue'

// 다른 프로젝트에서 가져오기 — 선택한 TC를 현재 프로젝트에 새 row로 복제 (origin_project_id 기록)
const props = defineProps({
  projectId: { type: Number, required: true },
  folderId: { type: Number, default: null },     // 넣을 폴더 (null = 미분류)
  folderLabel: { type: String, default: '미분류' },
})
const emit = defineEmits(['close', 'imported'])

const filter = reactive({ keyword: '' })
const items = ref([])
const total = ref(0)
const selected = ref(new Set())
const error = ref('')
const saving = ref(false)

const allChecked = computed(() => items.value.length > 0 && items.value.every((tc) => selected.value.has(tc.id)))

async function search() {
  error.value = ''
  try {
    const res = await testCaseApi.search({
      ...filter,
      keywordInProjectName: true, // 키워드를 프로젝트명에도 적용
      excludeProjectId: props.projectId,
      reviewStatus: 'APPROVED',
      status: 'ACTIVE',
      size: 100,
    })
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
  selected.value = allChecked.value ? new Set() : new Set(items.value.map((tc) => tc.id))
}

async function submit() {
  saving.value = true
  error.value = ''
  try {
    const imported = await testCaseApi.importFrom(props.projectId, [...selected.value], props.folderId)
    emit('imported', imported.length)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(search)
</script>

<template>
  <BaseModal title="다른 프로젝트에서 가져오기" width="900px" @close="emit('close')">
    <form class="filters" @submit.prevent="search">
      <input v-model="filter.keyword" class="input" placeholder="프로젝트명 / 코드 / 제목 / 태그" />
      <button class="btn btn-primary">검색</button>
    </form>
    <p class="muted hint">
      다른 프로젝트의 승인된 테스트케이스입니다. 선택한 항목은 현재 프로젝트의 <strong>{{ folderLabel }}</strong>에 새 케이스로
      복제되며 원본 프로젝트가 기록됩니다. (최대 100건{{ total > 100 ? ` / 전체 ${total}건` : '' }})
    </p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <table class="table">
      <thead>
        <tr>
          <th style="width: 36px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
          <th style="width: 180px">프로젝트</th>
          <th style="width: 100px">코드</th>
          <th>제목</th>
          <th style="width: 80px">출처</th>
          <th style="width: 70px">우선순위</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tc in items" :key="tc.id" class="clickable" @click="toggle(tc.id)">
          <td><input type="checkbox" :checked="selected.has(tc.id)" @click.stop @change="toggle(tc.id)" /></td>
          <td class="small">{{ tc.projectName }}<span v-if="tc.folderName" class="muted"> / {{ tc.folderName }}</span></td>
          <td class="mono">{{ tc.tcCode }}</td>
          <td>{{ tc.title }}</td>
          <td><LabelChip :map="TC_SOURCE" :value="tc.source" /></td>
          <td><PriorityChip :priority="tc.priority" /></td>
        </tr>
      </tbody>
    </table>
    <div v-if="!items.length" class="empty">가져올 수 있는 테스트케이스가 없습니다.</div>

    <template #footer>
      <span class="muted small">{{ selected.size }}건 선택</span>
      <div class="actions">
        <button class="btn" @click="emit('close')">취소</button>
        <button class="btn btn-primary" :disabled="!selected.size || saving" @click="submit">가져오기</button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
.filters {
  display: flex;
  gap: var(--space-2);
}
.hint {
  margin: var(--space-2) 0;
  font-size: var(--font-size-xs);
}
.small {
  font-size: var(--font-size-xs);
}
.actions {
  display: flex;
  gap: var(--space-2);
}
</style>
