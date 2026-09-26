<script setup>
import { passRate } from '@/constants/labels'

// 테스트 차수별 진행률 카드 — rounds: 차수 목록 응답(결과별 집계 포함)
// 진행률 = 통과 / 전체 (docs/dashboard-reference 기준)
defineProps({
  title: { type: String, default: '테스트 차수별 진행률' },
  rounds: { type: Array, required: true },
  moreTo: { type: String, default: '/cycles' },
})
</script>

<template>
  <section class="card list-card">
    <header class="card-head">
      <h3>{{ title }}</h3>
      <RouterLink :to="moreTo" class="more">전체보기</RouterLink>
    </header>

    <ul v-if="rounds.length" class="rounds">
      <li v-for="r in rounds" :key="r.id">
        <RouterLink :to="`/cycles/${r.id}`" class="round">
          <div class="round-head">
            <span class="name">{{ r.name }}</span>
            <span class="rate">{{ passRate(r) }}%</span>
          </div>
          <div class="track"><div class="fill" :style="{ width: `${passRate(r)}%` }" /></div>
          <div class="summary">
            전체 {{ r.totalCount }} · 통과 {{ r.passCount }} · 실패 {{ r.failCount }} · 보류 {{ r.blockedCount }} · 미실행
            {{ r.notRunCount }}
          </div>
        </RouterLink>
      </li>
    </ul>
    <p v-else class="empty">진행 중인 테스트 차수가 없습니다.</p>
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
.rounds {
  margin: 0;
  padding: 0;
  list-style: none;
}
.rounds li + li {
  border-top: 1px solid var(--border);
}
.round {
  display: block;
  padding: var(--space-4) 0;
  color: inherit;
}
.round-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: var(--space-2);
}
.name {
  font-weight: 500;
}
.round:hover .name {
  color: var(--accent);
}
.rate {
  color: var(--accent);
  font-size: var(--font-size-lg);
  font-weight: 700;
}
.track {
  height: 6px;
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  overflow: hidden;
}
.fill {
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--accent);
  transition: width 0.4s ease;
}
.summary {
  margin-top: var(--space-2);
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}
</style>
