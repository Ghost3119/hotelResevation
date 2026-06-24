import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { taxesClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type { TaxOrFeeCreateDto } from '../api/generated/schema'

export const taxKeys = {
  all: ['taxes-and-fees'] as const,
  list: (params: ListParams) => ['taxes-and-fees', 'list', params] as const,
}

export function useTaxes(params: ListParams = {}) {
  return useQuery({
    queryKey: taxKeys.list(params),
    queryFn: () => taxesClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useCreateTax() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: TaxOrFeeCreateDto) => taxesClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: taxKeys.all }),
  })
}

export function useUpdateTax() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: TaxOrFeeCreateDto }) =>
      taxesClient.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: taxKeys.all }),
  })
}
