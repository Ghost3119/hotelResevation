import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { roomBlocksClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type { RoomBlockCreateDto } from '../api/generated/schema'

export const roomBlockKeys = {
  all: ['room-blocks'] as const,
  list: (params: ListParams) => ['room-blocks', 'list', params] as const,
}

export function useRoomBlocks(params: ListParams = {}) {
  return useQuery({
    queryKey: roomBlockKeys.list(params),
    queryFn: () => roomBlocksClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useCreateRoomBlock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RoomBlockCreateDto) => roomBlocksClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomBlockKeys.all }),
  })
}

export function useUpdateRoomBlock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RoomBlockCreateDto }) =>
      roomBlocksClient.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomBlockKeys.all }),
  })
}

export function useReleaseRoomBlock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => roomBlocksClient.release(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: roomBlockKeys.all }),
  })
}
