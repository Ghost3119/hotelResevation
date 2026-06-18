import { api } from './client'
import type { PageDto, ReservationDto, ReservationStatus } from './types'

export interface ReservationCreateDto {
  guestId: number
  checkIn: string
  checkOut: string
  adults: number
  children: number
  roomTypeId: number
  roomId?: number | null
  notes?: string | null
  specialRequests?: string | null
}

export interface ReservationUpdateDto {
  checkIn: string
  checkOut: string
  adults: number
  children: number
  notes?: string | null
  specialRequests?: string | null
}

export interface AssignRoomDto {
  roomId: number
}

export interface CheckInDto {
  roomId?: number | null
}

export interface ReservationListParams {
  page?: number
  size?: number
  status?: ReservationStatus
  checkIn?: string
  guestId?: number
}

export const reservationsApi = {
  list: (params: ReservationListParams = {}) =>
    api.get<PageDto<ReservationDto>>('/reservations', { params }).then((r) => r.data),
  get: (id: number) =>
    api.get<ReservationDto>(`/reservations/${id}`).then((r) => r.data),
  create: (data: ReservationCreateDto) =>
    api.post<ReservationDto>('/reservations', data).then((r) => r.data),
  update: (id: number, data: ReservationUpdateDto) =>
    api.put<ReservationDto>(`/reservations/${id}`, data).then((r) => r.data),
  cancel: (id: number) =>
    api.post<ReservationDto>(`/reservations/${id}/cancel`).then((r) => r.data),
  assignRoom: (id: number, data: AssignRoomDto) =>
    api.post<ReservationDto>(`/reservations/${id}/assign-room`, data).then((r) => r.data),
  checkIn: (id: number, data: CheckInDto = {}) =>
    api.post<ReservationDto>(`/reservations/${id}/check-in`, data).then((r) => r.data),
  checkOut: (id: number) =>
    api.post<ReservationDto>(`/reservations/${id}/check-out`).then((r) => r.data),
}
