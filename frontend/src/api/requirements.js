import { http } from './http'

export const requirementApi = {
  list: (projectId) => http('/requirements', { params: { projectId } }),
  atomics: (projectId) => http('/requirements/atomics', { params: { projectId } }),
  get: (id) => http(`/requirements/${id}`),
  create: (body) => http('/requirements', { method: 'POST', body }),
  recommend: (id) => http(`/requirements/${id}/recommend`, { method: 'POST' }),
}

export const ruleCatalogApi = {
  list: () => http('/rule-catalog'),
}
