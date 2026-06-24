import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { housekeepingClient } from '../api/generated/client'
import type {
  HousekeepingTaskCreateDto,
  HousekeepingStatusUpdateDto,
} from '../api/generated/schema'

export interface HousekeepingListParams {
  status?: string
  roomId?: number
}

export const housekeepingKeys = {
  all: ['housekeeping-tasks'] as const,
  list: (params: HousekeepingListParams) => ['housekeeping-tasks', 'list', params] as const,
}

export function useHousekeepingTasks(params: HousekeepingListParams = {}) {
  return useQuery({
    queryKey: housekeepingKeys.list(params),
    queryFn: () => housekeepingClient.list(params),
  })
}

export function useCreateHousekeepingTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: HousekeepingTaskCreateDto) => housekeepingClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: housekeepingKeys.all }),
  })
}

export function useUpdateHousekeepingStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: HousekeepingStatusUpdateDto }) =>
      housekeepingClient.updateStatus(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: housekeepingKeys.all }),
  })
}
