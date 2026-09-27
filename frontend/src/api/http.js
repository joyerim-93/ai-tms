/** fetch 래퍼 — 실패 시 서버의 {message}를 담아 Error throw. 세션 쿠키 인증 + CSRF(XSRF-TOKEN 쿠키 → X-XSRF-TOKEN 헤더) */
let onUnauthorized = () => {}
/** 로그인이 필요하다는 401 을 받았을 때 호출할 핸들러 등록 (로그인/me 요청 제외) */
export const setUnauthorizedHandler = (fn) => (onUnauthorized = fn)

function csrfToken() {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  return m ? decodeURIComponent(m[1]) : ''
}

export async function http(path, { method = 'GET', params, body } = {}) {
  const url = new URL(`/api${path}`, window.location.origin)
  Object.entries(params ?? {}).forEach(([k, v]) => {
    if (v !== '' && v != null) url.searchParams.set(k, v)
  })

  const headers = {}
  // FormData(파일 업로드)는 브라우저가 multipart 경계를 넣도록 Content-Type 을 지정하지 않음
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') {
    // 로그아웃 시 서버가 CSRF 쿠키를 지우므로, 없으면 GET 한 번으로 새 토큰을 받은 뒤 보냄 (로그아웃 직후 로그인·회원가입)
    let token = csrfToken()
    if (!token) {
      await fetch('/api/auth/me', { credentials: 'include' }).catch(() => {})
      token = csrfToken()
    }
    if (token) headers['X-XSRF-TOKEN'] = token
  }

  const res = await fetch(url, {
    method,
    headers,
    credentials: 'include', // 세션 쿠키 — 프록시(같은 출처)와 프론트-백엔드 직접 호출(CORS) 모두 동작
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    if (res.status === 401 && !path.startsWith('/auth/')) onUnauthorized()
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || `요청 실패 (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}
