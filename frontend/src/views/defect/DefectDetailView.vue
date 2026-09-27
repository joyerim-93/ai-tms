<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defectApi } from '@/api/defects'
import { DEFECT_STATUS, SEVERITY, formatDateTime } from '@/constants/labels'
import LabelChip from '@/components/LabelChip.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import PriorityChip from '@/components/PriorityChip.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const defect = ref(null)
const comments = ref([])
const statusComment = ref('')
const newComment = ref('')
const busy = ref(false)
const error = ref('')

async function load() {
  const [d, c] = await Promise.all([defectApi.get(id), defectApi.comments(id)])
  defect.value = d
  comments.value = c
}

async function run(fn) {
  busy.value = true
  error.value = ''
  try {
    await fn()
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

const changeStatus = (status) =>
  run(async () => {
    await defectApi.changeStatus(id, status, statusComment.value)
    statusComment.value = ''
  })

const addComment = () =>
  run(async () => {
    await defectApi.addComment(id, newComment.value)
    newComment.value = ''
  })

onMounted(() => load().catch((e) => (error.value = e.message)))
</script>

<template>
  <div class="page-actions">
    <button class="btn" @click="router.push('/defects')">목록</button>
    <button v-if="defect" class="btn btn-primary" @click="router.push(`/defects/${id}/edit`)">수정</button>
  </div>
  <p v-if="error" class="error-text">{{ error }}</p>

  <div v-if="defect" class="layout">
    <div class="main-col">
      <section class="card">
        <div class="title-row">
          <span class="mono muted">{{ defect.defectCode }}</span>
          <StatusBadge :status="defect.status" />
          <LabelChip :map="SEVERITY" :value="defect.severity" />
          <PriorityChip :priority="defect.priority" />
        </div>
        <h2 class="title">{{ defect.title }}</h2>
        <p v-if="defect.description" class="pre">{{ defect.description }}</p>
        <p v-else class="muted">상세 내용이 없습니다.</p>
      </section>

      <section class="card">
        <div class="card-title">첨부파일</div>
        <AttachmentPanel base="defects" :owner-id="defect.id" />
      </section>

      <section class="card">
        <div class="card-title">이력 · 코멘트 ({{ comments.length }})</div>
        <ul v-if="comments.length" class="timeline">
          <li v-for="c in comments" :key="c.id" :class="{ 'status-change': c.statusTo }">
            <div class="timeline-head">
              <strong>{{ c.authorName }}</strong>
              <template v-if="c.statusTo">
                <StatusBadge :status="c.statusFrom" />
                →
                <StatusBadge :status="c.statusTo" />
              </template>
              <span class="muted small">{{ formatDateTime(c.createdAt) }}</span>
            </div>
            <p v-if="c.content" class="pre content">{{ c.content }}</p>
          </li>
        </ul>
        <p v-else class="muted">아직 이력이 없습니다.</p>

        <form class="comment-form" @submit.prevent="addComment">
          <textarea v-model="newComment" class="textarea" maxlength="4000" placeholder="코멘트 입력" />
          <button class="btn" :disabled="busy || !newComment.trim()">코멘트 등록</button>
        </form>
      </section>
    </div>

    <aside class="side-col">
      <section class="card">
        <div class="card-title">상태 변경</div>
        <textarea
          v-model="statusComment"
          class="textarea"
          maxlength="4000"
          placeholder="변경 사유 (선택)"
        />
        <div class="status-buttons">
          <button
            v-for="s in defect.nextStatuses"
            :key="s"
            class="btn"
            :disabled="busy"
            @click="changeStatus(s)"
          >
            → {{ DEFECT_STATUS[s] }}
          </button>
        </div>
      </section>

      <section class="card">
        <dl class="info">
          <dt>담당자</dt><dd>{{ defect.assigneeName ?? '미지정' }}</dd>
          <dt>보고자</dt><dd>{{ defect.reporterName }}</dd>
          <dt>등록일</dt><dd>{{ formatDateTime(defect.createdAt) }}</dd>
          <dt>수정일</dt><dd>{{ formatDateTime(defect.updatedAt) }}</dd>
          <dt>연결 TC</dt>
          <dd>
            <template v-if="defect.tcCode">
              <RouterLink :to="`/test-cases/${defect.testCaseId}`" class="mono">{{ defect.tcCode }}</RouterLink>
              {{ defect.tcTitle }}
            </template>
            <span v-else class="muted">-</span>
          </dd>
          <dt>차수</dt>
          <dd>
            <RouterLink v-if="defect.cycleId" :to="`/cycles/${defect.cycleId}`">
              {{ defect.cycleNo }}차 {{ defect.cycleName }}
            </RouterLink>
            <span v-else class="muted">-</span>
          </dd>
        </dl>
      </section>
    </aside>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  gap: var(--space-4);
  align-items: flex-start;
}
.main-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.side-col {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.title {
  margin: var(--space-2) 0 var(--space-4);
  font-size: var(--font-size-xl);
}
.pre {
  margin: 0;
  white-space: pre-wrap;
}
.small {
  font-size: var(--font-size-xs);
}
.timeline {
  margin: 0;
  padding: 0;
  list-style: none;
}
.timeline li {
  padding: var(--space-3) 0 var(--space-3) var(--space-3);
  border-left: 2px solid var(--border);
}
.timeline li.status-change {
  border-left-color: var(--accent);
}
.timeline-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.content {
  margin-top: var(--space-2);
  color: var(--text-secondary);
}
.comment-form {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-4);
}
.status-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}
.info {
  display: grid;
  grid-template-columns: 70px 1fr;
  gap: var(--space-3) var(--space-2);
  margin: 0;
}
.info dt {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}
.info dd {
  margin: 0;
}
</style>
