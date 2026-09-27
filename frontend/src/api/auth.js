import { http } from './http'

export const authApi = {
  me: () => http('/auth/me'),
  login: (username, password) => http('/auth/login', { method: 'POST', body: { username, password } }),
  register: (username, password, displayName) =>
    http('/auth/register', { method: 'POST', body: { username, password, displayName } }),
  logout: () => http('/auth/logout', { method: 'POST' }),
}
