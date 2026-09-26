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

// 결함 상태 → 표시명 / 칩 클래스
export const DEFECT_STATUS = {
  NEW: { label: '신규', chip: 'chip-accent' },
  OPEN: { label: '접수', chip: 'chip-medium' },
  IN_PROGRESS: { label: '처리중', chip: 'chip-medium' },
  RESOLVED: { label: '해결', chip: 'chip-pass' },
  CLOSED: { label: '종료', chip: 'chip-muted' },
  REJECTED: { label: '반려', chip: 'chip-low' },
}

export const SEVERITY = {
  CRITICAL: { label: '치명', chip: 'chip-high' },
  MAJOR: { label: '주요', chip: 'chip-medium' },
  MINOR: { label: '경미', chip: 'chip-low' },
  TRIVIAL: { label: '사소', chip: 'chip-muted' },
}

export const formatDateTime = (value) => (value ? value.slice(0, 16).replace('T', ' ') : '-')

/** 진행률(%) = 수행 완료(미수행 제외) / 전체 */
export const progressRate = (c) =>
  c.totalCount ? Math.round(((c.totalCount - c.notRunCount) / c.totalCount) * 100) : 0
