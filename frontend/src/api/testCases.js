import { http } from './http'

export const testCaseApi = {
  search: (params) => http('/test-cases', { params }),
  modules: (projectId) => http('/test-cases/modules', { params: { projectId } }),
  get: (id) => http(`/test-cases/${id}`),
  create: (body) => http('/test-cases', { method: 'POST', body }),
  update: (id, body) => http(`/test-cases/${id}`, { method: 'PUT', body }),
  review: (id, reviewStatus) => http(`/test-cases/${id}/review`, { method: 'PATCH', body: { reviewStatus } }),
  importFrom: (projectId, testCaseIds, folderId) =>
    http('/test-cases/import', { method: 'POST', body: { projectId, testCaseIds, folderId } }),
  // 엑셀 대량 업로드 → { successCount, failureCount, failures: [{ row, reason }] }
  uploadExcel: (projectId, file) => {
    const body = new FormData()
    body.append('file', file)
    return http(`/test-cases/import?projectId=${projectId}`, { method: 'POST', body })
  },
  excelTemplateUrl: '/api/test-cases/import/template',
  runs: (id) => http(`/test-cases/${id}/runs`),
  replaceRequirements: (id, atomicRequirementIds) =>
    http(`/test-cases/${id}/requirements`, { method: 'PUT', body: { atomicRequirementIds } }),
  // 데이터셋 행 (행 단위 CRUD)
  datasets: (id) => http(`/test-cases/${id}/datasets`),
  createDataset: (id, body) => http(`/test-cases/${id}/datasets`, { method: 'POST', body }),
  updateDataset: (id, rowId, body) => http(`/test-cases/${id}/datasets/${rowId}`, { method: 'PUT', body }),
  deleteDataset: (id, rowId) => http(`/test-cases/${id}/datasets/${rowId}`, { method: 'DELETE' }),
  remove: (id) => http(`/test-cases/${id}`, { method: 'DELETE' }),
}
