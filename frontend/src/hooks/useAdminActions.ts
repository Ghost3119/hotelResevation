import { useMutation, useQueryClient } from '@tanstack/react-query'
import { adminClient } from '../api/generated/client'
import { reservationKeys } from './useReservations'

export function useMarkNoShows() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => adminClient.markNoShows(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: reservationKeys.all })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
