import { useQuery } from '@tanstack/react-query'
import { reservationsClient } from '../api/generated/client'

export const adjustmentKeys = {
  byReservation: (id: number) => ['reservations', 'adjustments', id] as const,
}

export function useAdjustments(reservationId: number) {
  return useQuery({
    queryKey: adjustmentKeys.byReservation(reservationId),
    queryFn: () => reservationsClient.adjustments(reservationId),
    enabled: !!reservationId,
  })
}
