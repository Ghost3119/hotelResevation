import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { promotionsClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type { PromotionRuleCreateDto } from '../api/generated/schema'

export const promotionKeys = {
  all: ['promotions'] as const,
  list: (params: ListParams) => ['promotions', 'list', params] as const,
}

export function usePromotions(params: ListParams = {}) {
  return useQuery({
    queryKey: promotionKeys.list(params),
    queryFn: () => promotionsClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useCreatePromotion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: PromotionRuleCreateDto) => promotionsClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: promotionKeys.all }),
  })
}

export function useUpdatePromotion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PromotionRuleCreateDto }) =>
      promotionsClient.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: promotionKeys.all }),
  })
}
