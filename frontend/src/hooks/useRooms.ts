import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  roomsApi,
  type RoomListParams,
  type RoomObservationsDto,
  type RoomStatusDto,
  type RoomWriteDto,
} from '../api/rooms.api'

export const roomKeys = {
  all: ['rooms'] as const,
  list: (params: RoomListParams) => ['rooms', 'list', params] as const,
  detail: (id: number) => ['rooms', 'detail', id] as const,
}

export function useRooms(params: RoomListParams) {
  return useQuery({
    queryKey: roomKeys.list(params),
    queryFn: () => roomsApi.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useRoom(id: number) {
  return useQuery({
    queryKey: roomKeys.detail(id),
    queryFn: () => roomsApi.get(id),
    enabled: !!id,
  })
}

export function useCreateRoom() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RoomWriteDto) => roomsApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomKeys.all }),
  })
}

export function useUpdateRoom() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoomWriteDto }) =>
      roomsApi.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomKeys.all }),
  })
}

export function useSetRoomStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoomStatusDto }) =>
      roomsApi.setStatus(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomKeys.all }),
  })
}

export function useSetRoomObservations() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoomObservationsDto }) =>
      roomsApi.setObservations(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomKeys.all }),
  })
}

export type { RoomListParams, RoomWriteDto }
