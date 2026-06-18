import { api } from './client'
import type { GuestDto, PageDto, ReservationDto } from './types'

export interface GuestWriteDto {
  firstName: string
  lastName: string
  email?: string | null
  phone?: string | null
  documentNumber: string
  nationality: string
}

export interface GuestListParams {
  page?: number
  size?: number
  search?: string
}

export const guestsApi = {
  list: (params: GuestListParams = {}) =>
    api.get<PageDto<GuestDto>>('/guests', { params }).then((r) => r.data),
  get: (id: number) => api.get<GuestDto>(`/guests/${id}`).then((r) => r.data),
  create: (data: GuestWriteDto) => api.post<GuestDto>('/guests', data).then((r) => r.data),
  update: (id: number, data: GuestWriteDto) =>
    api.put<GuestDto>(`/guests/${id}`, data).then((r) => r.data),
  reservations: (id: number) =>
    api.get<ReservationDto[]>(`/guests/${id}/reservations`).then((r) => r.data),
}
