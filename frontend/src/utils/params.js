// 파라미터화 TC — 단계 텍스트의 {변수} 치환 (백엔드는 원본 텍스트 + paramValues JSON만 내려줌)

const PLACEHOLDER = /\{([\p{L}_][\p{L}\p{N}_]*)\}/gu

/** 표시용 값: 숫자는 천 단위 구분 (9999 → 9,999) */
export const formatParam = (v) => (typeof v === 'number' ? v.toLocaleString() : String(v))

/**
 * {변수} → 값 치환. {expected}는 데이터 행의 expectedResultOverride.
 * 값이 없는 변수는 그대로 둠(편집 중 누락을 눈으로 확인할 수 있도록).
 */
export function substitute(text, params = {}, expected = null) {
  if (!text) return text
  return text.replace(PLACEHOLDER, (whole, name) => {
    if (name === 'expected' && expected != null) return expected
    return Object.hasOwn(params, name) ? formatParam(params[name]) : whole
  })
}

/** 단계들(action/expectedResult)에 쓰인 변수명 — 데이터셋 컬럼 제안용 ({expected} 제외) */
export function extractVariables(steps = []) {
  const names = new Set()
  for (const s of steps) {
    for (const text of [s.action, s.expectedResult]) {
      for (const m of (text ?? '').matchAll(PLACEHOLDER)) {
        if (m[1] !== 'expected') names.add(m[1])
      }
    }
  }
  return [...names]
}

/** 셀 입력 문자열 → 저장 값 (숫자 모양이면 숫자로) */
export const parseCell = (text) => (/^-?\d+(\.\d+)?$/.test(text.trim()) ? Number(text.trim()) : text)

/** 템플릿 표시용 '{name}' (Vue 보간 안에서 `}}` 충돌 방지) */
export const braced = (name) => `{${name}}`

// ── 테스트 단계 — 등록/수정 폼은 표가 아니라 '테스트 단계'/'기대결과' 단일 텍스트 영역 2개로 입력받고,
//    내부적으로 줄 단위 test_step 행으로 변환해 그대로 저장(백엔드 엑셀 업로드의 파싱 규칙과 동일하게 맞춤).
const STEP_NUMBER = /^\s*\d+\s*[.)]\s*/

/** 텍스트를 줄 단위로 — 앞의 '1. '/'2) ' 번호는 제거, 빈 줄은 무시 */
function lines(text) {
  return (text ?? '')
    .split(/\r?\n/)
    .map((l) => l.replace(STEP_NUMBER, '').trim())
    .filter(Boolean)
}

/** steps[] → 텍스트 영역 프리필(수정 시). 단계 없으면 빈 문자열 */
export const stepsToText = (steps = []) => steps.map((s) => s.action).join('\n')
export const expectedToText = (steps = []) => steps.map((s) => s.expectedResult ?? '').join('\n')

/**
 * '테스트 단계'/'기대결과' 텍스트 2개 → steps[] (저장용).
 * 줄 수가 같으면 단계별로 짝짓고, 다르면 기대결과 전체를 마지막 단계에 몰아넣음.
 */
export function pairSteps(stepsText, expectedText) {
  const actions = lines(stepsText)
  const expecteds = lines(expectedText)
  if (actions.length === 0) return []
  const paired = actions.length === expecteds.length
  return actions.map((action, i) => ({
    action,
    expectedResult: paired ? expecteds[i] : i === actions.length - 1 ? expecteds.join('\n') || null : null,
  }))
}
