import { useQuery } from '@tanstack/react-query'
import { availabilityApi, type AvailabilityParams } from '../api/availability.api'

export function useAvailability(params: AvailabilityParams | null) {
  return useQuery({
    queryKey: ['availability', params],
    queryFn: () => availabilityApi.search(params as AvailabilityParams),
    enabled: !!params,
    staleTime: 0,
  })
}
