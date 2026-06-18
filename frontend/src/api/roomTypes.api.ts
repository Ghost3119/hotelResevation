import { api } from './client'
import type { RoomTypeDto } from './types'

export interface RoomTypeWriteDto {
  name: string
  description?: string | null
  maxCapacity: number
  basePrice: number
  amenities: string[]
  active: boolean
}

export interface RoomTypeStatusDto {
  active: boolean
}

export interface RoomTypeListParams {
  active?: boolean
}

export const roomTypesApi = {
  list: (params: RoomTypeListParams = {}) =>
    api.get<RoomTypeDto[]>('/room-types', { params }).then((r) => r.data),
  get: (id: number) => api.get<RoomTypeDto>(`/room-types/${id}`).then((r) => r.data),
  create: (data: RoomTypeWriteDto) =>
    api.post<RoomTypeDto>('/room-types', data).then((r) => r.data),
  update: (id: number, data: RoomTypeWriteDto) =>
    api.put<RoomTypeDto>(`/room-types/${id}`, data).then((r) => r.data),
  setStatus: (id: number, data: RoomTypeStatusDto) =>
    api.patch<RoomTypeDto>(`/room-types/${id}/status`, data).then((r) => r.data),
}
