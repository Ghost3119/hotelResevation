import { useQuery } from '@tanstack/react-query'
import { dashboardApi, type DashboardParams } from '../api/dashboard.api'

export function useDashboard(params: DashboardParams) {
  return useQuery({
    queryKey: ['dashboard', params],
    queryFn: () => dashboardApi.get(params),
  })
}
