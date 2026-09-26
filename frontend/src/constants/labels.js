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

export const formatDateTime = (value) => (value ? value.slice(0, 16).replace('T', ' ') : '-')
