import { http } from './http'

export const projectApi = {
  list: () => http('/projects'),
  create: (body) => http('/projects', { method: 'POST', body }),
  members: (id) => http(`/projects/${id}/members`),
}

export const folderApi = {
  tree: (projectId) => http(`/projects/${projectId}/folders`),
  create: (projectId, body) => http(`/projects/${projectId}/folders`, { method: 'POST', body }),
}
