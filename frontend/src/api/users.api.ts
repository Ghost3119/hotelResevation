import { api } from './client'
import type { PageDto, UserDto, UserRole } from './types'

export interface UserWriteDto {
  email: string
  password?: string
  fullName: string
  role: UserRole
  active: boolean
}

export interface UserStatusDto {
  active: boolean
}

export interface UserListParams {
  page?: number
  size?: number
  search?: string
}

export const usersApi = {
  list: (params: UserListParams = {}) =>
    api.get<PageDto<UserDto>>('/users', { params }).then((r) => r.data),
  get: (id: number) => api.get<UserDto>(`/users/${id}`).then((r) => r.data),
  create: (data: UserWriteDto) => api.post<UserDto>('/users', data).then((r) => r.data),
  update: (id: number, data: UserWriteDto) =>
    api.put<UserDto>(`/users/${id}`, data).then((r) => r.data),
  setStatus: (id: number, data: UserStatusDto) =>
    api.patch<UserDto>(`/users/${id}/status`, data).then((r) => r.data),
}
