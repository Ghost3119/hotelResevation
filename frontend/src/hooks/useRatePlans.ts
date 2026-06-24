import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ratePlansClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type {
  RatePlanCreateDto,
  RatePlanDto,
  RatePlanStatusDto,
} from '../api/generated/schema'

export const ratePlanKeys = {
  all: ['rate-plans'] as const,
  list: (params: ListParams) => ['rate-plans', 'list', params] as const,
  detail: (id: number) => ['rate-plans', 'detail', id] as const,
}

export function useRatePlans(params: ListParams = {}) {
  return useQuery({
    queryKey: ratePlanKeys.list(params),
    queryFn: () => ratePlansClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useRatePlan(id: number) {
  return useQuery({
    queryKey: ratePlanKeys.detail(id),
    queryFn: () => ratePlansClient.get(id),
    enabled: !!id,
  })
}

export function useCreateRatePlan() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RatePlanCreateDto) => ratePlansClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ratePlanKeys.all }),
  })
}

export function useUpdateRatePlan() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RatePlanCreateDto }) =>
      ratePlansClient.update(id, data),
    onSuccess: (rp: RatePlanDto) => {
      qc.invalidateQueries({ queryKey: ratePlanKeys.all })
      qc.setQueryData(ratePlanKeys.detail(rp.id), rp)
    },
  })
}

export function useSetRatePlanActive() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RatePlanStatusDto }) =>
      ratePlansClient.setStatus(id, data),
    onSuccess: (rp: RatePlanDto) => {
      qc.invalidateQueries({ queryKey: ratePlanKeys.all })
      qc.setQueryData(ratePlanKeys.detail(rp.id), rp)
    },
  })
}
