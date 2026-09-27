import { http } from './http'

export const cycleApi = {
  list: (projectId) => http('/cycles', { params: { projectId } }),
  get: (id) => http(`/cycles/${id}`),
  create: (body) => http('/cycles', { method: 'POST', body }),
  update: (id, body) => http(`/cycles/${id}`, { method: 'PUT', body }),
  remove: (id) => http(`/cycles/${id}`, { method: 'DELETE' }),

  executions: (id, params) => http(`/cycles/${id}/executions`, { params }),
  addExecutions: (id, testCaseIds, assigneeId) =>
    http(`/cycles/${id}/executions`, { method: 'POST', body: { testCaseIds, assigneeId } }),
  assign: (id, executionIds, assigneeId) =>
    http(`/cycles/${id}/executions/assignee`, { method: 'PUT', body: { executionIds, assigneeId } }),
  removeExecution: (id, executionId) => http(`/cycles/${id}/executions/${executionId}`, { method: 'DELETE' }),
}

export const executionApi = {
  get: (id) => http(`/executions/${id}`),
  history: (id) => http(`/executions/${id}/history`),
  record: (id, result, comment) => http(`/executions/${id}/results`, { method: 'POST', body: { result, comment } }),
  // 임시저장 — result 는 아직 선택 전(null)이어도 코멘트만 저장 가능. 확정 결과·이력에는 반영되지 않음
  saveDraft: (id, result, comment) => http(`/executions/${id}/draft`, { method: 'POST', body: { result, comment } }),
}
