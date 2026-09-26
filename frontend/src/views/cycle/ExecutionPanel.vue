<script setup>
import { onMounted, ref } from 'vue'
import { executionApi } from '@/api/cycles'
import { testCaseApi } from '@/api/testCases'
import { RESULT, formatDateTime } from '@/constants/labels'
import StatusBadge from '@/components/StatusBadge.vue'
import PriorityChip from '@/components/PriorityChip.vue'

// 우측 슬라이드 패널: TC 절차 확인 + 결과 입력 + 수행 이력
const props = defineProps({
  executionId: { type: Number, required: true },
  readonly: { type: Boolean, default: false }, // 종료된 차수
})
const emit = defineEmits(['close', 'recorded'])

const exec = ref(null)
const tc = ref(null)
const history = ref([])
const comment = ref('')
const saving = ref(false)
const error = ref('')

async function load() {
  exec.value = await executionApi.get(props.executionId)
  const [testCase, hist] = await Promise.all([
    testCaseApi.get(exec.value.testCaseId),
    executionApi.history(props.executionId),
  ])
  tc.value = testCase
  history.value = hist
}

async function record(result) {
  saving.value = true
  error.value = ''
  try {
    await executionApi.record(props.executionId, result, comment.value)
    comment.value = ''
    await load()
    emit('recorded')
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(() => load().catch((e) => (error.value = e.message)))
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <aside class="panel">
      <header class="panel-header">
        <div v-if="exec">
          <span class="mono muted">{{ exec.tcCode }}</span>
          <h3>{{ exec.tcTitle }}</h3>
        </div>
        <button class="btn btn-sm" @click="emit('close')">✕</button>
      </header>

      <div class="panel-body">
        <p v-if="error" class="error-text">{{ error }}</p>
        <template v-if="exec && tc">
          <div class="meta">
            <PriorityChip :priority="tc.priority" />
            <StatusBadge :status="exec.result" />
            <span class="muted">담당 {{ exec.assigneeName ?? '미지정' }}</span>
          </div>
          <p v-if="exec.tcVersion !== tc.version" class="notice">
            ⚠ 차수 등록 후 TC가 수정되었습니다 (등록 v{{ exec.tcVersion }} → 현재 v{{ tc.version }}). 아래는 현재 버전입니다.
          </p>

          <section v-if="tc.precondition" class="block">
            <div class="label">사전조건</div>
            <p class="pre">{{ tc.precondition }}</p>
          </section>

          <section class="block">
            <div class="label">테스트 단계</div>
            <ol v-if="tc.steps.length" class="steps">
              <li v-for="s in tc.steps" :key="s.id">
                <div class="pre">{{ s.action }}</div>
                <div v-if="s.expectedResult" class="expected pre">→ {{ s.expectedResult }}</div>
              </li>
            </ol>
            <p v-else class="muted">등록된 단계가 없습니다.</p>
          </section>

          <section v-if="!readonly" class="block record">
            <div class="label">결과 입력</div>
            <textarea v-model="comment" class="textarea" maxlength="2000" placeholder="코멘트 (선택)" />
            <div class="result-buttons">
              <button
                v-for="(label, key) in RESULT"
                :key="key"
                class="btn result-btn"
                :class="`result-${key.toLowerCase()}`"
                :disabled="saving"
                @click="record(key)"
              >
                {{ label }}
              </button>
            </div>
          </section>
          <p v-else class="muted">종료된 차수는 결과를 입력할 수 없습니다.</p>

          <section class="block">
            <div class="label">수행 이력 ({{ history.length }})</div>
            <ul v-if="history.length" class="history">
              <li v-for="h in history" :key="h.id">
                <div class="history-head">
                  <StatusBadge :status="h.result" />
                  <span>{{ h.executedByName }}</span>
                  <span class="muted">{{ formatDateTime(h.executedAt) }}</span>
                </div>
                <p v-if="h.comment" class="pre comment">{{ h.comment }}</p>
              </li>
            </ul>
            <p v-else class="muted">아직 수행 이력이 없습니다.</p>
          </section>
        </template>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: var(--bg-overlay);
}
.panel {
  position: absolute;
  inset: 0 0 0 auto;
  width: 520px;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  box-shadow: var(--shadow-overlay);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--space-5);
  border-bottom: 1px solid var(--border);
}
.panel-header h3 {
  margin-top: var(--space-1);
  font-size: var(--font-size-lg);
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-5);
}
.meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.notice {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--status-notrun);
  background: var(--status-notrun-bg);
  font-size: var(--font-size-sm);
}
.block {
  margin-top: var(--space-5);
}
.pre {
  margin: 0;
  white-space: pre-wrap;
}
.steps {
  margin: 0;
  padding-left: var(--space-5);
}
.steps li + li {
  margin-top: var(--space-3);
}
.expected {
  margin-top: var(--space-1);
  color: var(--text-secondary);
}
.result-buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-2);
  margin-top: var(--space-2);
}
.result-btn {
  justify-content: center;
  font-weight: 600;
}
.result-pass { color: var(--status-pass); background: var(--status-pass-bg); }
.result-fail { color: var(--status-fail); background: var(--status-fail-bg); }
.result-blocked { color: var(--status-blocked); background: var(--status-blocked-bg); }
.result-not_run { color: var(--status-notrun); background: var(--status-notrun-bg); }
.result-btn:hover { filter: brightness(0.95); }
.history {
  margin: 0;
  padding: 0;
  list-style: none;
}
.history li {
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border);
}
.history-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}
.comment {
  margin-top: var(--space-2);
  color: var(--text-secondary);
}
</style>
