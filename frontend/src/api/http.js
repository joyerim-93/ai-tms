/** fetch 래퍼 — 실패 시 서버의 {message}를 담아 Error throw */
import { readStoredUserName } from '@/utils/userName'

/**
 * userName: 이 요청만 다른 이름으로 기록하고 싶을 때(폼에서 작성자/실행자/보고자를 직접 고친 경우). 없으면 저장된 현재 사용자 이름.
 * 이름은 X-User-Name 헤더(URL 인코딩)로 전달 — 표시용이며 인증이 아님.
 */
export async function http(path, { method = 'GET', params, body, userName } = {}) {
  const url = new URL(`/api${path}`, window.location.origin)
  Object.entries(params ?? {}).forEach(([k, v]) => {
    if (v !== '' && v != null) url.searchParams.set(k, v)
  })

  const res = await fetch(url, {
    method,
    // FormData(파일 업로드)는 브라우저가 multipart 경계를 넣도록 Content-Type 을 지정하지 않음
    headers: {
      ...(body && !(body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
      ...userHeader(userName),
    },
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || `요청 실패 (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}

function userHeader(override) {
  const name = (override || readStoredUserName()).trim()
  return name ? { 'X-User-Name': encodeURIComponent(name) } : {}
}
