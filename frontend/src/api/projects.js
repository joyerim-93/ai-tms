import { http } from './http'

export const projectApi = {
  list: () => http('/projects'),
  members: (id) => http(`/projects/${id}/members`),
}
