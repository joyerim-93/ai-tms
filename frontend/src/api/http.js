/** fetch 래퍼 — 실패 시 서버의 {message}를 담아 Error throw */
export async function http(path, { method = 'GET', params, body } = {}) {
  const url = new URL(`/api${path}`, window.location.origin)
  Object.entries(params ?? {}).forEach(([k, v]) => {
    if (v !== '' && v != null) url.searchParams.set(k, v)
  })

  const res = await fetch(url, {
    method,
    // FormData(파일 업로드)는 브라우저가 multipart 경계를 넣도록 Content-Type 을 지정하지 않음
    headers: body && !(body instanceof FormData) ? { 'Content-Type': 'application/json' } : undefined,
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || `요청 실패 (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}
