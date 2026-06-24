import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { privacyClient } from '../api/generated/client'
import type { ListParams } from '../api/generated/client'
import type {
  PrivacyRequestCreateDto,
  PrivacyRequestUpdateDto,
} from '../api/generated/schema'

export const privacyKeys = {
  all: ['privacy-requests'] as const,
  list: (params: ListParams) => ['privacy-requests', 'list', params] as const,
  detail: (id: number) => ['privacy-requests', 'detail', id] as const,
  logs: (params: ListParams) => ['personal-data-access-logs', params] as const,
}

export function usePrivacyRequests(params: ListParams = {}) {
  return useQuery({
    queryKey: privacyKeys.list(params),
    queryFn: () => privacyClient.list(params),
    placeholderData: (prev) => prev,
  })
}

export function usePrivacyRequest(id: number) {
  return useQuery({
    queryKey: privacyKeys.detail(id),
    queryFn: () => privacyClient.get(id),
    enabled: !!id,
  })
}

export function useCreatePrivacyRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: PrivacyRequestCreateDto) => privacyClient.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: privacyKeys.all }),
  })
}

export function useUpdatePrivacyRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PrivacyRequestUpdateDto }) =>
      privacyClient.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: privacyKeys.all }),
  })
}

export function useExportPrivacyRequest() {
  return useMutation({
    mutationFn: (id: number) => privacyClient.export(id),
  })
}

export function useAnonymizePrivacyRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => privacyClient.anonymize(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: privacyKeys.all }),
  })
}

export function usePersonalDataAccessLogs(params: ListParams = {}) {
  return useQuery({
    queryKey: privacyKeys.logs(params),
    queryFn: () => privacyClient.accessLogs(params),
  })
}
