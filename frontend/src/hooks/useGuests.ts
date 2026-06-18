import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { guestsApi, type GuestListParams, type GuestWriteDto } from '../api/guests.api'
import type { ReservationDto } from '../api/types'

export const guestKeys = {
  all: ['guests'] as const,
  list: (params: GuestListParams) => ['guests', 'list', params] as const,
  detail: (id: number) => ['guests', 'detail', id] as const,
  reservations: (id: number) => ['guests', 'reservations', id] as const,
}

export function useGuests(params: GuestListParams) {
  return useQuery({
    queryKey: guestKeys.list(params),
    queryFn: () => guestsApi.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useGuest(id: number) {
  return useQuery({
    queryKey: guestKeys.detail(id),
    queryFn: () => guestsApi.get(id),
    enabled: !!id,
  })
}

export function useGuestReservations(id: number) {
  return useQuery({
    queryKey: guestKeys.reservations(id),
    queryFn: () => guestsApi.reservations(id),
    enabled: !!id,
  })
}

export function useCreateGuest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: GuestWriteDto) => guestsApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: guestKeys.all }),
  })
}

export function useUpdateGuest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: GuestWriteDto }) =>
      guestsApi.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: guestKeys.all }),
  })
}

export type { ReservationDto }
