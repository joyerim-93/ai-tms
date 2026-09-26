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

export const formatDateTime = (value) => (value ? value.slice(0, 16).replace('T', ' ') : '-')

/** 진행률(%) = 수행 완료(미수행 제외) / 전체 */
export const progressRate = (c) =>
  c.totalCount ? Math.round(((c.totalCount - c.notRunCount) / c.totalCount) * 100) : 0
