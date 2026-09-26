import { http } from './http'

export const testCaseApi = {
  search: (params) => http('/test-cases', { params }),
  modules: () => http('/test-cases/modules'),
  get: (id) => http(`/test-cases/${id}`),
  create: (body) => http('/test-cases', { method: 'POST', body }),
  update: (id, body) => http(`/test-cases/${id}`, { method: 'PUT', body }),
  remove: (id) => http(`/test-cases/${id}`, { method: 'DELETE' }),
}
