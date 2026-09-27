import { http } from './http'

export const requirementApi = {
  list: (projectId) => http('/requirements', { params: { projectId } }),
  atomics: (projectId) => http('/requirements/atomics', { params: { projectId } }),
  get: (id) => http(`/requirements/${id}`),
  create: (body) => http('/requirements', { method: 'POST', body }),
  // AI 추천 요청 → 잡(RUNNING) 즉시 반환. 진행/결과는 recommendationJobApi 로 폴링
  recommend: (id) => http(`/requirements/${id}/recommend`, { method: 'POST' }),
}

export const recommendationJobApi = {
  get: (jobId) => http(`/recommendation-jobs/${jobId}`),
  latest: (requirementId) => http('/recommendation-jobs/latest', { params: { requirementId } }), // 없으면 null
}

export const ruleCatalogApi = {
  list: () => http('/rule-catalog'),
}
