<script setup>
import { computed, ref, watch } from 'vue'
import { formatParam, parseCell, braced } from '@/utils/params'

/**
 * 스프레드시트형 데이터셋 테이블 (Zephyr "Test Data").
 * 컬럼 = 변수명, 행 = 데이터 행. editable 이면 셀 인라인 편집(포커스 아웃 시 저장) / 행 추가·삭제 / 변수 열 추가·삭제.
 * 저장은 부모가 API로 처리: emit update(id, body) / create(body) / remove(id)
 * 읽기 전용(차수 화면 등)에서는 highlightId 행을 강조.
 */
const props = defineProps({
  rows: { type: Array, required: true },               // TestCaseDataset[] (paramValues는 객체)
  suggestedVariables: { type: Array, default: () => [] }, // 단계에 쓰인 {변수}
  editable: { type: Boolean, default: false },
  highlightId: { type: Number, default: null },
})
const emit = defineEmits(['update', 'create', 'remove'])

const extraColumns = ref([]) // 아직 값이 없는 새 변수 열
const newColumn = ref('')
const columnError = ref('')

const columns = computed(() => {
  const set = new Set(props.suggestedVariables)
  props.rows.forEach((r) => Object.keys(r.paramValues ?? {}).forEach((k) => set.add(k)))
  extraColumns.value.forEach((c) => set.add(c))
  return [...set]
})
const missingInRows = computed(() => props.suggestedVariables.filter((v) => props.rows.some((r) => !(v in (r.paramValues ?? {})))))

// 편집 버퍼 (id → { rowLabel, values, expected }) — 서버 값이 바뀌면 다시 채움
const drafts = ref({})
watch(
  () => props.rows,
  (rows) => {
    drafts.value = Object.fromEntries(
      rows.map((r) => [
        r.id,
        {
          rowLabel: r.rowLabel,
          expected: r.expectedResultOverride ?? '',
          values: Object.fromEntries(Object.entries(r.paramValues ?? {}).map(([k, v]) => [k, String(v)])),
        },
      ]),
    )
  },
  { immediate: true, deep: true },
)

function toBody(d, dropColumn = null) {
  const paramValues = {}
  for (const [k, v] of Object.entries(d.values)) {
    if (k !== dropColumn && v !== '') paramValues[k] = parseCell(v)
  }
  return { rowLabel: d.rowLabel.trim() || '이름 없음', paramValues, expectedResultOverride: d.expected || null }
}

/** 포커스 아웃 시 바뀐 경우에만 저장 */
function commit(row) {
  const d = drafts.value[row.id]
  const body = toBody(d)
  const before = {
    rowLabel: row.rowLabel,
    paramValues: row.paramValues ?? {},
    expectedResultOverride: row.expectedResultOverride ?? null,
  }
  if (JSON.stringify(body) !== JSON.stringify(before)) emit('update', row.id, body)
}

function addRow() {
  const paramValues = {}
  emit('create', { rowLabel: `데이터 ${props.rows.length + 1}`, paramValues, expectedResultOverride: null })
}

function addColumn() {
  const name = newColumn.value.trim()
  columnError.value = ''
  if (!/^[\p{L}_][\p{L}\p{N}_]*$/u.test(name)) {
    columnError.value = '변수명은 문자·숫자·_ 만 (첫 글자 숫자 불가)'
    return
  }
  if (name === 'expected') {
    columnError.value = "'expected'는 기대결과 열로 예약되어 있습니다"
    return
  }
  if (!columns.value.includes(name)) extraColumns.value.push(name)
  newColumn.value = ''
}

function removeColumn(name) {
  extraColumns.value = extraColumns.value.filter((c) => c !== name)
  props.rows.forEach((r) => {
    if (name in (r.paramValues ?? {})) emit('update', r.id, toBody(drafts.value[r.id], name))
  })
}

const display = (row, col) => (col in (row.paramValues ?? {}) ? formatParam(row.paramValues[col]) : '')
</script>

<template>
  <div class="dataset">
    <p v-if="editable && missingInRows.length" class="hint warn">
      단계에 쓰인 변수 {{ missingInRows.map(braced).join(', ') }} 값이 비어 있는 행이 있습니다.
    </p>

    <div class="scroll">
      <table class="sheet">
        <thead>
          <tr>
            <th class="num">#</th>
            <th class="label-col">데이터 행</th>
            <th v-for="c in columns" :key="c" class="var-col">
              <span class="var">{{ c }}</span>
              <button
                v-if="editable"
                type="button"
                class="col-remove"
                :title="`${c} 열 삭제`"
                @click="removeColumn(c)"
              >
                ✕
              </button>
            </th>
            <th class="expected-col" title="{expected} 치환값">기대결과 <span class="var">{expected}</span></th>
            <th v-if="editable" class="actions" />
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in rows" :key="r.id" :class="{ highlight: r.id === highlightId }">
            <td class="num">{{ i + 1 }}</td>
            <template v-if="editable && drafts[r.id]">
              <td><input v-model="drafts[r.id].rowLabel" class="cell" maxlength="200" @blur="commit(r)" /></td>
              <td v-for="c in columns" :key="c">
                <input v-model="drafts[r.id].values[c]" class="cell mono" @blur="commit(r)" />
              </td>
              <td><input v-model="drafts[r.id].expected" class="cell" maxlength="2000" @blur="commit(r)" /></td>
              <td class="actions">
                <button type="button" class="btn btn-sm btn-danger" title="행 삭제" @click="emit('remove', r.id)">✕</button>
              </td>
            </template>
            <template v-else>
              <td>{{ r.rowLabel }}</td>
              <td v-for="c in columns" :key="c" class="mono">{{ display(r, c) }}</td>
              <td class="muted">{{ r.expectedResultOverride ?? '' }}</td>
            </template>
          </tr>
          <tr v-if="!rows.length">
            <td :colspan="columns.length + (editable ? 4 : 3)" class="empty-row">데이터 행이 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="editable" class="toolbar">
      <button type="button" class="btn btn-sm" @click="addRow">+ 행 추가</button>
      <form class="add-col" @submit.prevent="addColumn">
        <input v-model="newColumn" class="input col-input" placeholder="변수명 (예: amount)" maxlength="50" />
        <button class="btn btn-sm">+ 변수 열</button>
      </form>
      <span v-if="columnError" class="error-text small">{{ columnError }}</span>
      <span class="muted small">셀을 수정하고 다른 곳을 클릭하면 저장됩니다.</span>
    </div>
  </div>
</template>

<style scoped>
.scroll {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}
.sheet {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}
.sheet th,
.sheet td {
  padding: 0;
  border-right: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  text-align: left;
  white-space: nowrap;
}
.sheet th:last-child,
.sheet td:last-child {
  border-right: none;
}
.sheet tbody tr:last-child td {
  border-bottom: none;
}
.sheet th {
  padding: var(--space-2) var(--space-3);
  background: var(--surface-page);
  color: var(--text-secondary);
  font-weight: 600;
}
.sheet td:not(:has(input)) {
  padding: var(--space-2) var(--space-3);
}
.num {
  width: 40px;
  color: var(--text-muted);
  text-align: center !important;
}
.label-col {
  min-width: 180px;
}
.var-col {
  min-width: 120px;
}
.expected-col {
  min-width: 240px;
}
.var {
  font-family: var(--font-mono);
  color: var(--accent);
  font-weight: 500;
}
.col-remove {
  margin-left: var(--space-1);
  padding: 0;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  cursor: pointer;
}
.col-remove:hover {
  color: var(--result-fail-text);
}
.cell {
  width: 100%;
  height: 34px;
  padding: 0 var(--space-3);
  border: 2px solid transparent;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
}
.cell:hover {
  background: var(--surface-hover);
}
.cell:focus {
  outline: none;
  border-color: var(--accent);
  background: var(--surface-card);
}
.actions {
  width: 48px;
  text-align: center !important;
}
tr.highlight td {
  background: var(--accent-soft);
}
.empty-row {
  padding: var(--space-5) !important;
  color: var(--text-muted);
  text-align: center !important;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-3);
}
.add-col {
  display: flex;
  gap: var(--space-2);
}
.col-input {
  width: 180px;
  height: 28px;
}
.hint {
  margin: 0 0 var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
}
.warn {
  color: var(--badge-progress-text);
  background: var(--badge-progress-bg);
}
.small {
  font-size: var(--font-size-xs);
}
</style>
