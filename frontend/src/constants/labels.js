// 백엔드 enum ↔ 화면 표시명

export const PRIORITY = {
  HIGH: '높음',
  MEDIUM: '보통',
  LOW: '낮음',
}

export const TC_STATUS = {
  ACTIVE: '사용',
  DEPRECATED: '폐기',
}

export const CYCLE_STATUS = {
  PLANNED: '계획',
  IN_PROGRESS: '진행중',
  CLOSED: '종료',
}

// StatusBadge와 동일한 표시명 (셀렉트 옵션용)
export const RESULT = {
  PASS: '성공',
  FAIL: '실패',
  BLOCKED: 'Block',
  NOT_RUN: '미수행',
}

// 이슈(결함) 상태 표시명 — 색상은 StatusBadge 가 담당
export const DEFECT_STATUS = {
  NEW: '신규',
  OPEN: '열림',
  IN_PROGRESS: '진행중',
  RESOLVED: '해결됨',
  CLOSED: '종료',
  REJECTED: '반려',
}

export const SEVERITY = {
  CRITICAL: { label: '치명', chip: 'chip-high' },
  MAJOR: { label: '주요', chip: 'chip-medium' },
  MINOR: { label: '경미', chip: 'chip-low' },
  TRIVIAL: { label: '사소', chip: 'chip-muted' },
}

export const formatDateTime = (value) => (value ? value.slice(0, 16).replace('T', ' ') : '-')

/** "10분 전", "3시간 전", "어제", "5일 전", 그 이상은 날짜 */
export function formatRelative(value) {
  if (!value) return '-'
  const diffMin = Math.floor((Date.now() - new Date(value).getTime()) / 60000)
  if (diffMin < 1) return '방금'
  if (diffMin < 60) return `${diffMin}분 전`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}시간 전`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay === 1) return '어제'
  if (diffDay < 7) return `${diffDay}일 전`
  return value.slice(0, 10)
}

/** 통과율(%) = 통과 / 전체 — 대시보드 '테스트 차수별 진행률' 기준 */
export const passRate = (c) => (c.totalCount ? Math.round((c.passCount / c.totalCount) * 100) : 0)

/** 진행률(%) = 수행 완료(미수행 제외) / 전체 */
export const progressRate = (c) =>
  c.totalCount ? Math.round(((c.totalCount - c.notRunCount) / c.totalCount) * 100) : 0

// ── AI 추천 / 요구사항
export const TC_SOURCE = {
  MANUAL: { label: '직접작성', chip: 'chip-muted' },
  RULE: { label: '규칙', chip: 'chip-accent' },
  RAG: { label: 'RAG', chip: 'chip-medium' },
  LLM: { label: 'AI 생성', chip: 'chip-high' },
}

export const REVIEW_STATUS = {
  DRAFT: { label: '검토대기', chip: 'chip-medium' },
  APPROVED: { label: '승인', chip: 'chip-pass' },
  REJECTED: { label: '반려', chip: 'chip-muted' },
}

export const TECHNIQUE = {
  BOUNDARY_VALUE: '경계값 분석',
  EQUIVALENCE_PARTITION: '동등 분할',
  DECISION_TABLE: '디시전 테이블',
  EXPLORATORY: '탐색적',
}

export const REQUIREMENT_TYPE = {
  AMOUNT_RANGE: '금액 범위',
  RATE_RANGE: '비율 범위',
  PERIOD_CONDITION: '기간 조건',
  BOOLEAN_FLAG: '여부 플래그',
}
