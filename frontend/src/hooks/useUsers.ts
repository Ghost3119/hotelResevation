import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { usersApi, type UserListParams, type UserStatusDto, type UserWriteDto } from '../api/users.api'

export const userKeys = {
  all: ['users'] as const,
  list: (params: UserListParams) => ['users', 'list', params] as const,
  detail: (id: number) => ['users', 'detail', id] as const,
}

export function useUsers(params: UserListParams) {
  return useQuery({
    queryKey: userKeys.list(params),
    queryFn: () => usersApi.list(params),
    placeholderData: (prev) => prev,
  })
}

export function useCreateUser() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UserWriteDto) => usersApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: userKeys.all }),
  })
}

export function useUpdateUser() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UserWriteDto }) =>
      usersApi.update(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: userKeys.all }),
  })
}

export function useSetUserStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UserStatusDto }) =>
      usersApi.setStatus(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: userKeys.all }),
  })
}
