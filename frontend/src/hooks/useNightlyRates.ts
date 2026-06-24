import { useQuery } from '@tanstack/react-query'
import { reservationsClient } from '../api/generated/client'
import { reservationKeys } from './useReservations'

export const nightlyRateKeys = {
  byReservation: (id: number) => ['reservations', 'nightly-rates', id] as const,
}

export function useNightlyRates(reservationId: number) {
  return useQuery({
    queryKey: nightlyRateKeys.byReservation(reservationId),
    queryFn: () => reservationsClient.nightlyRates(reservationId),
    enabled: !!reservationId,
  })
}

// Re-export so consumers can invalidate the reservation detail alongside.
export { reservationKeys }
