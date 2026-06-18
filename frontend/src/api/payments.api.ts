import { api } from './client'
import type { PaymentDto, PaymentMethod, PaymentStatus } from './types'

export interface PaymentCreateDto {
  amount: number
  method: PaymentMethod
  reference?: string | null
}

export interface PaymentStatusDto {
  status: PaymentStatus
}

export const paymentsApi = {
  listByReservation: (reservationId: number) =>
    api.get<PaymentDto[]>(`/reservations/${reservationId}/payments`).then((r) => r.data),
  create: (reservationId: number, data: PaymentCreateDto) =>
    api.post<PaymentDto>(`/reservations/${reservationId}/payments`, data).then((r) => r.data),
  setStatus: (id: number, data: PaymentStatusDto) =>
    api.patch<PaymentDto>(`/payments/${id}/status`, data).then((r) => r.data),
}
