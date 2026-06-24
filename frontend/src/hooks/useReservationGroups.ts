import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { reservationGroupsClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type { ReservationGroupCreateDto } from '../api/generated/schema'

export const reservationGroupKeys = {
  all: ['reservation-groups'] as const,
  list: (params: ListParams) => ['reservation-groups', 'list', params] as const,
  detail: (id: number) => ['reservation-groups', 'detail', id] as const,
}

export function useReservationGroups(params: ListParams = {}) {
  return useQuery({
    queryKey: reservationGroupKeys.list(params),
    queryFn: () => reservationGroupsClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useReservationGroup(id: number) {
  return useQuery({
    queryKey: reservationGroupKeys.detail(id),
    queryFn: () => reservationGroupsClient.get(id),
    enabled: !!id,
  })
}

export function useCreateReservationGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ReservationGroupCreateDto) => reservationGroupsClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: reservationGroupKeys.all }),
  })
}

export function useCancelReservationGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => reservationGroupsClient.cancel(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: reservationGroupKeys.all }),
  })
}
