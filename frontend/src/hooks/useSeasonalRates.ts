import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { seasonalRatesClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type { SeasonalRateCreateDto } from '../api/generated/schema'

export const seasonalRateKeys = {
  all: ['seasonal-rates'] as const,
  list: (params: ListParams) => ['seasonal-rates', 'list', params] as const,
}

export function useSeasonalRates(params: ListParams = {}) {
  return useQuery({
    queryKey: seasonalRateKeys.list(params),
    queryFn: () => seasonalRatesClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useCreateSeasonalRate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: SeasonalRateCreateDto) => seasonalRatesClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: seasonalRateKeys.all }),
  })
}

export function useUpdateSeasonalRate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: SeasonalRateCreateDto }) =>
      seasonalRatesClient.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: seasonalRateKeys.all }),
  })
}

export function useDeleteSeasonalRate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => seasonalRatesClient.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: seasonalRateKeys.all }),
  })
}
