import { api } from './client'
import type { PageDto, RoomDto, RoomStatus } from './types'

export interface RoomWriteDto {
  number: string
  floor: number
  roomTypeId: number
  status: RoomStatus
  observations?: string | null
}

export interface RoomStatusDto {
  status: RoomStatus
}

export interface RoomObservationsDto {
  observations: string | null
}

export interface RoomListParams {
  page?: number
  size?: number
  floor?: number
  roomTypeId?: number
  status?: RoomStatus
}

export const roomsApi = {
  list: (params: RoomListParams = {}) =>
    api.get<PageDto<RoomDto>>('/rooms', { params }).then((r) => r.data),
  get: (id: number) => api.get<RoomDto>(`/rooms/${id}`).then((r) => r.data),
  create: (data: RoomWriteDto) => api.post<RoomDto>('/rooms', data).then((r) => r.data),
  update: (id: number, data: RoomWriteDto) =>
    api.put<RoomDto>(`/rooms/${id}`, data).then((r) => r.data),
  setStatus: (id: number, data: RoomStatusDto) =>
    api.patch<RoomDto>(`/rooms/${id}/status`, data).then((r) => r.data),
  setObservations: (id: number, data: RoomObservationsDto) =>
    api.patch<RoomDto>(`/rooms/${id}/observations`, data).then((r) => r.data),
}
