<script setup>
import { formatRelative } from '@/constants/labels'
import StatusBadge from '@/components/StatusBadge.vue'

// 최근 이슈 리스트 카드 — issues: defect 목록 응답(items)
defineProps({
  title: { type: String, default: '최근 등록된 이슈' },
  issues: { type: Array, required: true },
  moreTo: { type: String, default: '/defects' },
})
</script>

<template>
  <section class="card list-card">
    <header class="card-head">
      <h3>{{ title }}</h3>
      <RouterLink :to="moreTo" class="more">전체보기</RouterLink>
    </header>

    <ul v-if="issues.length" class="issues">
      <li v-for="d in issues" :key="d.id">
        <RouterLink :to="`/defects/${d.id}`" class="issue">
          <span class="code">{{ d.defectCode }}</span>
          <span class="body">
            <span class="issue-title">{{ d.title }}</span>
            <span class="meta">담당 {{ d.assigneeName ?? '미지정' }} · {{ formatRelative(d.createdAt) }}</span>
          </span>
          <StatusBadge :status="d.status" />
        </RouterLink>
      </li>
    </ul>
    <p v-else class="empty">등록된 이슈가 없습니다.</p>
  </section>
</template>

<style scoped>
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: var(--space-3);
}
.card-head h3 {
  font-size: var(--font-size-lg);
}
.more {
  font-size: var(--font-size-xs);
}
.issues {
  margin: 0;
  padding: 0;
  list-style: none;
}
.issues li + li {
  border-top: 1px solid var(--border);
}
.issue {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  color: inherit;
}
.issue:hover .issue-title {
  color: var(--accent);
}
.code {
  flex-shrink: 0;
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);
  background: var(--accent-soft);
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
}
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.issue-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  transition: color var(--transition);
}
.meta {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
</style>
