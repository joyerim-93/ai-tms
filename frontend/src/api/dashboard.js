import { http } from './http'

export const dashboardApi = {
  summary: (projectId) => http('/dashboard', { params: { projectId } }),
}
