<script setup>
import { ref } from 'vue'
import { testCaseApi } from '@/api/testCases'
import BaseModal from '@/components/BaseModal.vue'

// 테스트케이스 엑셀 대량 업로드 — 파일 선택/드래그앤드롭 → 업로드 → 결과(성공 N건 / 실패 M건 + 실패 사유)를 팝업 안에 표시
const props = defineProps({
  projectId: { type: Number, required: true },
})
const emit = defineEmits(['close', 'uploaded'])

const file = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const result = ref(null)
const error = ref('')
const input = ref(null)

function pick(f) {
  error.value = ''
  result.value = null
  if (f && !f.name.toLowerCase().endsWith('.xlsx')) {
    file.value = null
    error.value = '.xlsx 파일만 업로드할 수 있습니다.'
    return
  }
  file.value = f ?? null
}
const onDrop = (e) => {
  dragging.value = false
  pick(e.dataTransfer.files[0])
}
const onChange = (e) => pick(e.target.files[0])

async function upload() {
  if (!file.value) return
  uploading.value = true
  error.value = ''
  try {
    result.value = await testCaseApi.uploadExcel(props.projectId, file.value)
    if (result.value.successCount > 0) emit('uploaded', result.value.successCount) // 뒤의 목록·폴더 트리 새로고침
    file.value = null
    if (input.value) input.value.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <BaseModal title="엑셀 업로드" width="640px" @close="emit('close')">
    <p class="muted hint">
      양식(제목 · 스텝 · 기대결과 · 폴더경로 · 기법)에 맞춰 작성한 .xlsx 파일을 올리면 테스트케이스가 <strong>검토대기(DRAFT)</strong>로 한꺼번에
      등록됩니다. 없는 폴더는 자동으로 만들어집니다.
      <a :href="testCaseApi.excelTemplateUrl" download class="template-link">📄 템플릿 다운로드</a>
    </p>

    <div
      class="drop"
      :class="{ over: dragging }"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
      @click="input.click()"
    >
      <input ref="input" type="file" accept=".xlsx" hidden @change="onChange" />
      <template v-if="file">
        <strong>📎 {{ file.name }}</strong>
        <span class="muted small">{{ (file.size / 1024).toFixed(1) }} KB · 클릭하면 다른 파일 선택</span>
      </template>
      <template v-else>
        <strong>여기에 .xlsx 파일을 끌어다 놓거나 클릭해서 선택</strong>
        <span class="muted small">한 번에 최대 1000행 · 5MB</span>
      </template>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div v-if="result" class="result">
      <div class="summary">
        <span class="ok">성공 {{ result.successCount }}건</span>
        <span class="sep">/</span>
        <span :class="result.failureCount ? 'fail' : 'muted'">실패 {{ result.failureCount }}건</span>
      </div>
      <ul v-if="result.failures.length" class="failures">
        <li v-for="f in result.failures" :key="f.row"><span class="row-no">{{ f.row }}행</span> {{ f.reason }}</li>
      </ul>
      <p v-if="result.successCount" class="muted small">
        등록된 테스트케이스는 검토대기 상태입니다. 상세에서 승인해야 테스트 차수에 넣을 수 있습니다.
      </p>
    </div>

    <template #footer>
      <span />
      <div class="actions">
        <button class="btn" @click="emit('close')">{{ result ? '닫기' : '취소' }}</button>
        <button class="btn btn-primary" :disabled="!file || uploading" @click="upload">
          {{ uploading ? '업로드 중…' : '업로드' }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
.hint {
  margin: 0 0 var(--space-3);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}
.template-link {
  margin-left: var(--space-2);
  color: var(--accent);
  font-weight: 600;
  white-space: nowrap;
}
.drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-6) var(--space-4);
  border: 2px dashed var(--border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  text-align: center;
  cursor: pointer;
}
.drop:hover,
.drop.over {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.small {
  font-size: var(--font-size-xs);
}
.result {
  margin-top: var(--space-4);
}
.summary {
  display: flex;
  gap: var(--space-2);
  font-size: var(--font-size-lg);
  font-weight: 700;
}
.ok {
  color: var(--result-success-text);
}
.fail {
  color: var(--result-fail-text);
}
.failures {
  max-height: 220px;
  margin: var(--space-2) 0;
  padding: var(--space-2) var(--space-3);
  overflow-y: auto;
  list-style: none;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  font-size: var(--font-size-sm);
}
.failures li {
  padding: 2px 0;
}
.row-no {
  display: inline-block;
  min-width: 48px;
  color: var(--result-fail-text);
  font-weight: 600;
}
.actions {
  display: flex;
  gap: var(--space-2);
}
</style>
