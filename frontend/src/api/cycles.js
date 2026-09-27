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
  // 자동저장 — result/comment 모두 선택. result 생략 시 기존 확정 결과 유지, 실제로 바뀔 때만 이력에 기록됨
  patch: (id, body) => http(`/executions/${id}`, { method: 'PATCH', body }),
}
