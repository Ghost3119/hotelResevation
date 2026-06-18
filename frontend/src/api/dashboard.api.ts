import { api } from './client'
import type { DashboardDto } from './types'

export interface DashboardParams {
  from?: string
  to?: string
}

export const dashboardApi = {
  get: (params: DashboardParams = {}) =>
    api.get<DashboardDto>('/dashboard', { params }).then((r) => r.data),
}
