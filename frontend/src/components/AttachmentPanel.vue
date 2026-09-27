<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { attachmentApi } from '@/api/attachments'

// 증빙 첨부(스크린샷/로그) — 파일 선택 · 드래그앤드롭 · 클립보드 붙여넣기(Ctrl+V)
// ownerId 가 있으면 바로 업로드/삭제(서버 목록), 없으면(등록 화면) 선택한 파일을 pending 에 모아 두었다가 부모가 저장 후 업로드
const props = defineProps({
  base: { type: String, required: true },                 // 'executions' | 'defects'
  ownerId: { type: [Number, String], default: null },
  readonly: { type: Boolean, default: false },            // 종료된 차수 등: 목록만
  emphasize: { type: Boolean, default: false },           // 실패 결과 등 첨부를 유도할 때
  emphasizeText: { type: String, default: '증빙(스크린샷·로그)을 첨부하면 원인 분석이 빨라집니다.' },
  pending: { type: Array, default: () => [] },            // ownerId 없을 때의 선택 파일 (v-model:pending)
})
const emit = defineEmits(['update:pending', 'changed'])

const api = computed(() => attachmentApi(props.base))
const items = ref([])
const dragging = ref(false)
const busy = ref(false)
const error = ref('')
const input = ref(null)

async function load() {
  if (!props.ownerId) {
    items.value = []
    return
  }
  try {
    items.value = await api.value.list(props.ownerId)
  } catch (e) {
    error.value = e.message
  }
}
watch(() => [props.base, props.ownerId], load, { immediate: true })

async function addFiles(fileList) {
  const files = [...fileList].filter(Boolean)
  if (!files.length) return
  error.value = ''
  if (!props.ownerId) {
    emit('update:pending', [...props.pending, ...files])
    return
  }
  busy.value = true
  try {
    await api.value.upload(props.ownerId, files)
    await load()
    emit('changed')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

const onPick = (e) => {
  addFiles(e.target.files)
  e.target.value = ''
}
const onDrop = (e) => {
  dragging.value = false
  if (!props.readonly) addFiles(e.dataTransfer.files)
}
// 캡처한 스크린샷을 Ctrl+V 로 바로 붙여넣기
function onPaste(e) {
  if (props.readonly) return
  const files = [...(e.clipboardData?.files ?? [])].map((f, i) =>
    f.name && f.name !== 'image.png' ? f : new File([f], `screenshot-${Date.now()}-${i + 1}.${(f.type.split('/')[1] || 'png').replace('jpeg', 'jpg')}`, { type: f.type }),
  )
  if (files.length) {
    e.preventDefault()
    addFiles(files)
  }
}

async function remove(a) {
  if (!window.confirm(`'${a.fileName}' 첨부를 삭제할까요?`)) return
  error.value = ''
  try {
    await api.value.remove(props.ownerId, a.id)
    await load()
    emit('changed')
  } catch (e) {
    error.value = e.message
  }
}
const removePending = (i) => emit('update:pending', props.pending.filter((_, idx) => idx !== i))

// pending 이미지 미리보기 URL (메모리 해제 관리)
const previews = new Map()
const previewUrl = (file) => {
  if (!previews.has(file)) previews.set(file, URL.createObjectURL(file))
  return previews.get(file)
}
onBeforeUnmount(() => previews.forEach((u) => URL.revokeObjectURL(u)))

const isImage = (file) => file.type.startsWith('image/') && !file.type.includes('svg')
const size = (n) => (n < 1024 ? `${n} B` : n < 1024 * 1024 ? `${(n / 1024).toFixed(1)} KB` : `${(n / 1024 / 1024).toFixed(1)} MB`)
const icon = (name) => {
  const ext = name.split('.').pop().toLowerCase()
  if (ext === 'pdf') return '📕'
  if (['txt', 'log', 'json'].includes(ext)) return '📝'
  if (['zip'].includes(ext)) return '🗜️'
  if (['xlsx', 'csv'].includes(ext)) return '📊'
  return '📎'
}
const count = computed(() => (props.ownerId ? items.value.length : props.pending.length))
</script>

<template>
  <div class="attach" :class="{ emphasize: emphasize && !readonly && !count }">
    <div class="head">
      <span class="label">증빙 첨부 <span class="muted">({{ count }})</span></span>
      <button v-if="!readonly" type="button" class="btn btn-sm" :disabled="busy" @click="input.click()">📎 첨부</button>
      <input ref="input" type="file" multiple hidden @change="onPick" />
    </div>

    <div v-if="emphasize && !readonly && !count" class="nudge">❗ {{ emphasizeText }}</div>

    <ul v-if="count" class="list">
      <li v-for="a in items" :key="a.id" class="item">
        <a :href="api.fileUrl(ownerId, a.id)" download class="file" :title="`${a.fileName} 다운로드`">
          <img v-if="a.image" :src="api.fileUrl(ownerId, a.id, true)" :alt="a.fileName" class="thumb" />
          <span v-else class="icon">{{ icon(a.fileName) }}</span>
          <span class="meta">
            <span class="name">{{ a.fileName }}</span>
            <span class="muted small">{{ size(a.fileSize) }} · {{ a.uploadedByName }}</span>
          </span>
        </a>
        <button v-if="!readonly" type="button" class="x" title="삭제" @click="remove(a)">✕</button>
      </li>
      <li v-for="(f, i) in pending" v-show="!ownerId" :key="`p${i}`" class="item">
        <span class="file">
          <img v-if="isImage(f)" :src="previewUrl(f)" :alt="f.name" class="thumb" />
          <span v-else class="icon">{{ icon(f.name) }}</span>
          <span class="meta">
            <span class="name">{{ f.name }}</span>
            <span class="muted small">{{ size(f.size) }} · 저장 시 업로드</span>
          </span>
        </span>
        <button type="button" class="x" title="제거" @click="removePending(i)">✕</button>
      </li>
    </ul>

    <div
      v-if="!readonly"
      class="drop"
      :class="{ over: dragging }"
      tabindex="0"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
      @paste="onPaste"
    >
      {{ busy ? '업로드 중…' : '파일을 끌어다 놓거나, 여기를 클릭한 뒤 Ctrl+V 로 스크린샷을 붙여넣으세요' }}
    </div>
    <p v-else-if="!count" class="muted small">첨부된 파일이 없습니다.</p>
    <p v-if="error" class="error-text">{{ error }}</p>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}
.head .label {
  margin: 0;
}
.nudge {
  margin-bottom: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--result-fail-bg);
  color: var(--result-fail-text);
  font-size: var(--font-size-sm);
  font-weight: 600;
}
.emphasize .drop {
  border-color: var(--result-fail-text);
  animation: attach-pulse 1.6s ease-in-out infinite;
}
@keyframes attach-pulse {
  50% {
    background: var(--result-fail-bg);
  }
}
.list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0 0 var(--space-2);
  padding: 0;
  list-style: none;
}
.item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}
.file {
  display: flex;
  flex: 1;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
  color: var(--text-primary);
}
a.file:hover .name {
  color: var(--accent);
  text-decoration: underline;
}
.thumb {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  object-fit: cover;
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
}
.icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  font-size: var(--font-size-xl);
}
.meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--font-size-sm);
}
.small {
  font-size: var(--font-size-xs);
}
.x {
  padding: var(--space-1) var(--space-2);
  border: none;
  background: none;
  color: var(--text-muted);
  cursor: pointer;
}
.x:hover {
  color: var(--result-fail-text);
}
.drop {
  padding: var(--space-3);
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  text-align: center;
  outline: none;
}
.drop:hover,
.drop:focus,
.drop.over {
  border-color: var(--accent);
  background: var(--accent-soft);
}
</style>
