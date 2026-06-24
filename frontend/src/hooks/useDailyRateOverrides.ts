import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { dailyRateOverridesClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type { DailyRateOverrideCreateDto } from '../api/generated/schema'

export const dailyRateOverrideKeys = {
  all: ['daily-rate-overrides'] as const,
  list: (params: ListParams) => ['daily-rate-overrides', 'list', params] as const,
}

export function useDailyRateOverrides(params: ListParams = {}) {
  return useQuery({
    queryKey: dailyRateOverrideKeys.list(params),
    queryFn: () => dailyRateOverridesClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useCreateDailyRateOverride() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: DailyRateOverrideCreateDto) => dailyRateOverridesClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: dailyRateOverrideKeys.all }),
  })
}

export function useUpdateDailyRateOverride() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: DailyRateOverrideCreateDto }) =>
      dailyRateOverridesClient.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: dailyRateOverrideKeys.all }),
  })
}

export function useDeleteDailyRateOverride() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => dailyRateOverridesClient.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: dailyRateOverrideKeys.all }),
  })
}
