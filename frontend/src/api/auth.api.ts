import { api } from './client'
import type { AuthResponse, LoginRequest, RefreshResponse, UserDto } from './types'

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', data).then((r) => r.data),
  me: () => api.get<UserDto>('/auth/me').then((r) => r.data),
  refresh: () => api.post<RefreshResponse>('/auth/refresh').then((r) => r.data),
  logout: () => api.post('/auth/logout'),
}
