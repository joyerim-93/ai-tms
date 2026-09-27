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
  record: (id, result, comment, userName) =>
    http(`/executions/${id}/results`, { method: 'POST', body: { result, comment }, userName }),
}
