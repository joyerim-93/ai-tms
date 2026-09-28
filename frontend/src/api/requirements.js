import { http } from './http'

export const requirementApi = {
  list: (projectId) => http('/requirements', { params: { projectId } }),
  atomics: (projectId) => http('/requirements/atomics', { params: { projectId } }),
  get: (id) => http(`/requirements/${id}`),
  create: (body) => http('/requirements', { method: 'POST', body }),
  // AI 추천 요청 → 잡(RUNNING) 즉시 반환. 진행/결과는 recommendationJobApi 로 폴링
  recommend: (id) => http(`/requirements/${id}/recommend`, { method: 'POST' }),
  // 원자 요구사항 자동 분해(LLM, 동기 — 호출 1회라 잡 없이 바로 결과). 이미 있으면 재호출 없이 그대로 반환
  decompose: (id) => http(`/requirements/${id}/decompose`, { method: 'POST' }),
}

export const recommendationJobApi = {
  get: (jobId) => http(`/recommendation-jobs/${jobId}`),
  latest: (requirementId) => http('/recommendation-jobs/latest', { params: { requirementId } }), // 없으면 null
}

export const ruleCatalogApi = {
  list: () => http('/rule-catalog'),
}

// AI 생성(LLM) 추천 on/off — 화면 토글. 서버 재기동하면 설정 파일 기본값(app.ai.llm.enabled)으로 돌아감
export const llmSettingsApi = {
  get: () => http('/llm-settings'),
  update: (enabled) => http('/llm-settings', { method: 'PATCH', body: { enabled } }),
}
