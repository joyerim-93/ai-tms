import { http } from './http'

export const authApi = {
  me: () => http('/auth/me'),
  login: (loginId, password) => http('/auth/login', { method: 'POST', body: { loginId, password } }),
  logout: () => http('/auth/logout', { method: 'POST' }),
}
