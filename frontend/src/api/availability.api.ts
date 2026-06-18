import { api } from './client'
import type { AvailabilityRoomDto } from './types'

export interface AvailabilityParams {
  checkIn: string
  checkOut: string
  guests: number
  roomTypeId?: number
}

export const availabilityApi = {
  search: (params: AvailabilityParams) =>
    api.get<AvailabilityRoomDto[]>('/availability', { params }).then((r) => r.data),
}
