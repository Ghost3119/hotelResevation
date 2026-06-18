import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  roomTypesApi,
  type RoomTypeListParams,
  type RoomTypeStatusDto,
  type RoomTypeWriteDto,
} from '../api/roomTypes.api'

export const roomTypeKeys = {
  all: ['room-types'] as const,
  list: (params: RoomTypeListParams) => ['room-types', 'list', params] as const,
  detail: (id: number) => ['room-types', 'detail', id] as const,
}

export function useRoomTypes(params: RoomTypeListParams = {}) {
  return useQuery({
    queryKey: roomTypeKeys.list(params),
    queryFn: () => roomTypesApi.list(params),
  })
}

export function useRoomType(id: number) {
  return useQuery({
    queryKey: roomTypeKeys.detail(id),
    queryFn: () => roomTypesApi.get(id),
    enabled: !!id,
  })
}

export function useCreateRoomType() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RoomTypeWriteDto) => roomTypesApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomTypeKeys.all }),
  })
}

export function useUpdateRoomType() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoomTypeWriteDto }) =>
      roomTypesApi.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomTypeKeys.all }),
  })
}

export function useSetRoomTypeStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoomTypeStatusDto }) =>
      roomTypesApi.setStatus(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomTypeKeys.all }),
  })
}
