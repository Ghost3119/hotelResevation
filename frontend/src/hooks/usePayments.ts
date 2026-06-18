import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { paymentsApi, type PaymentCreateDto, type PaymentStatusDto } from '../api/payments.api'
import { reservationKeys } from './useReservations'

export const paymentKeys = {
  byReservation: (id: number) => ['payments', 'reservation', id] as const,
}

export function useReservationPayments(reservationId: number) {
  return useQuery({
    queryKey: paymentKeys.byReservation(reservationId),
    queryFn: () => paymentsApi.listByReservation(reservationId),
    enabled: !!reservationId,
  })
}

export function useCreatePayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ reservationId, data }: { reservationId: number; data: PaymentCreateDto }) =>
      paymentsApi.create(reservationId, data),
    onSuccess: (pay) => {
      qc.invalidateQueries({ queryKey: paymentKeys.byReservation(pay.reservationId) })
      qc.invalidateQueries({ queryKey: reservationKeys.detail(pay.reservationId) })
      qc.invalidateQueries({ queryKey: reservationKeys.all })
    },
  })
}

export function useSetPaymentStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PaymentStatusDto; reservationId: number }) =>
      paymentsApi.setStatus(id, data),
    onSuccess: (pay, vars) => {
      qc.invalidateQueries({ queryKey: paymentKeys.byReservation(vars.reservationId) })
      qc.invalidateQueries({ queryKey: reservationKeys.detail(vars.reservationId) })
      qc.invalidateQueries({ queryKey: reservationKeys.all })
    },
  })
}
