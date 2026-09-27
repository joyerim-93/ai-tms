import { http } from './http'

// 증빙 첨부 — base: 'executions'(테스트 수행 결과) | 'defects'(결함)
export const attachmentApi = (base) => ({
  list: (ownerId) => http(`/${base}/${ownerId}/attachments`),
  upload: (ownerId, files) => {
    const body = new FormData()
    files.forEach((f) => body.append('file', f))
    return http(`/${base}/${ownerId}/attachments`, { method: 'POST', body })
  },
  remove: (ownerId, id) => http(`/${base}/${ownerId}/attachments/${id}`, { method: 'DELETE' }),
  fileUrl: (ownerId, id, inline = false) => `/api/${base}/${ownerId}/attachments/${id}/file${inline ? '?inline=true' : ''}`,
})
