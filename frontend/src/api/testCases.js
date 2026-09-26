import { http } from './http'

export const testCaseApi = {
  search: (params) => http('/test-cases', { params }),
  modules: () => http('/test-cases/modules'),
  get: (id) => http(`/test-cases/${id}`),
  create: (body) => http('/test-cases', { method: 'POST', body }),
  update: (id, body) => http(`/test-cases/${id}`, { method: 'PUT', body }),
  review: (id, reviewStatus) => http(`/test-cases/${id}/review`, { method: 'PATCH', body: { reviewStatus } }),
  remove: (id) => http(`/test-cases/${id}`, { method: 'DELETE' }),
}
