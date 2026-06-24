import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancellationPoliciesClient } from '../api/generated/client'
import type { CancellationPolicyCreateDto } from '../api/generated/schema'

export const cancellationPolicyKeys = {
  all: ['cancellation-policies'] as const,
}

export function useCancellationPolicies() {
  return useQuery({
    queryKey: cancellationPolicyKeys.all,
    queryFn: () => cancellationPoliciesClient.list(),
  })
}

export function useCreateCancellationPolicy() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CancellationPolicyCreateDto) => cancellationPoliciesClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: cancellationPolicyKeys.all }),
  })
}

export function useUpdateCancellationPolicy() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CancellationPolicyCreateDto }) =>
      cancellationPoliciesClient.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: cancellationPolicyKeys.all }),
  })
}
