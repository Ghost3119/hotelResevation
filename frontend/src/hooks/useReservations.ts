import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  reservationsApi,
  type AssignRoomDto,
  type CheckInDto,
  type ReservationCreateDto,
  type ReservationListParams,
  type ReservationUpdateDto,
} from '../api/reservations.api'

export const reservationKeys = {
  all: ['reservations'] as const,
  list: (params: ReservationListParams) => ['reservations', 'list', params] as const,
  detail: (id: number) => ['reservations', 'detail', id] as const,
}

export function useReservations(params: ReservationListParams) {
  return useQuery({
    queryKey: reservationKeys.list(params),
    queryFn: () => reservationsApi.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useReservation(id: number) {
  return useQuery({
    queryKey: reservationKeys.detail(id),
    queryFn: () => reservationsApi.get(id),
    enabled: !!id,
  })
}

export function useCreateReservation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ReservationCreateDto) => reservationsApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: reservationKeys.all }),
  })
}

export function useUpdateReservation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ReservationUpdateDto }) =>
      reservationsApi.update(id, data),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: reservationKeys.all })
      qc.setQueryData(reservationKeys.detail(res.id), res)
    },
  })
}

export function useCancelReservation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => reservationsApi.cancel(id),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: reservationKeys.all })
      qc.setQueryData(reservationKeys.detail(res.id), res)
    },
  })
}

export function useAssignRoom() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: AssignRoomDto }) =>
      reservationsApi.assignRoom(id, data),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: reservationKeys.all })
      qc.setQueryData(reservationKeys.detail(res.id), res)
    },
  })
}

export function useCheckIn() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data?: CheckInDto }) =>
      reservationsApi.checkIn(id, data ?? {}),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: reservationKeys.all })
      qc.setQueryData(reservationKeys.detail(res.id), res)
    },
  })
}

export function useCheckOut() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => reservationsApi.checkOut(id),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: reservationKeys.all })
      qc.setQueryData(reservationKeys.detail(res.id), res)
    },
  })
}
