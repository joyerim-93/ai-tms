import { http } from './http'

export const defectApi = {
  search: (params) => http('/defects', { params }),
  get: (id) => http(`/defects/${id}`),
  create: (body) => http('/defects', { method: 'POST', body }),
  update: (id, body) => http(`/defects/${id}`, { method: 'PUT', body }),
  changeStatus: (id, status, comment) => http(`/defects/${id}/status`, { method: 'POST', body: { status, comment } }),
  comments: (id) => http(`/defects/${id}/comments`),
  addComment: (id, content) => http(`/defects/${id}/comments`, { method: 'POST', body: { content } }),
}
