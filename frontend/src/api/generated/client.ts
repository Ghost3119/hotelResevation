/**
 * Generated typed API client for the Hotel Manager backend.
 *
 * Built on top of the existing axios `api` instance (`src/api/client.ts`, which
 * is configured with `withCredentials: true`, the auth interceptor and the
 * refresh-token rotation logic). Functions are grouped by resource, mirroring
 * the schema in `./schema.ts`.
 *
 * Regenerate the schema with `npm run api:generate`; this client is hand-ish
 * but typed against the generated schema so it stays in sync.
 */
import { api } from '../client'
import type {
  PageDto,
} from './schema'
import type {
  AssignRoomDto,
  AuthResponse,
  AvailabilityRoomDto,
  CancellationPolicyCreateDto,
  CancellationPolicyDto,
  ChangeRoomDto,
  CheckInDto,
  DailyRateOverrideCreateDto,
  DailyRateOverrideDto,
  DashboardDto,
  GuestDto,
  GuestFullDto,
  GuestFullExportDto,
  GuestWriteDto,
  HousekeepingStatusUpdateDto,
  HousekeepingTaskCreateDto,
  HousekeepingTaskDto,
  ModifyStayDto,
  NoShowDto,
  PaymentCreateDto,
  PaymentDto,
  PaymentStatusDto,
  PersonalDataAccessLogDto,
  PrivacyRequestCreateDto,
  PrivacyRequestDto,
  PrivacyRequestUpdateDto,
  PromotionRuleCreateDto,
  PromotionRuleDto,
  QuoteRequest,
  QuoteResultDto,
  RatePlanCreateDto,
  RatePlanDto,
  RatePlanStatusDto,
  ReservationAdjustmentDto,
  ReservationCreateDto,
  ReservationDto,
  ReservationGroupCreateDto,
  ReservationGroupDto,
  ReservationNightlyRateDto,
  ReservationUpdateDto,
  RoomBlockCreateDto,
  RoomBlockDto,
  RoomDto,
  RoomObservationsDto,
  RoomStatusDto,
  RoomTypeDto,
  RoomTypeWriteDto,
  RoomWriteDto,
  SeasonalRateCreateDto,
  SeasonalRateDto,
  StatusDto,
  TaxOrFeeCreateDto,
  TaxOrFeeDto,
  UserDto,
  UserWriteDto,
} from './schema'

const unwrap = <T>(p: Promise<{ data: T }>) => p.then((r) => r.data)

export const authClient = {
  login: (data: { email: string; password: string }) =>
    unwrap<AuthResponse>(api.post('/auth/login', data)),
  refresh: () => unwrap<{ token: string; type: string; expiresIn: number }>(api.post('/auth/refresh')),
  logout: () => api.post('/auth/logout'),
  me: () => unwrap<UserDto>(api.get('/auth/me')),
}

export interface ListParams { page?: number; size?: number; [k: string]: any }

export const usersClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<UserDto>>(api.get('/users', { params })),
  get: (id: number) => unwrap<UserDto>(api.get(`/users/${id}`)),
  create: (data: UserWriteDto) => unwrap<UserDto>(api.post('/users', data)),
  update: (id: number, data: UserWriteDto) => unwrap<UserDto>(api.put(`/users/${id}`, data)),
  setStatus: (id: number, data: StatusDto) => unwrap<UserDto>(api.patch(`/users/${id}/status`, data)),
}

export const guestsClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<GuestDto>>(api.get('/guests', { params })),
  get: (id: number) => unwrap<GuestDto>(api.get(`/guests/${id}`)),
  create: (data: GuestWriteDto) => unwrap<GuestDto>(api.post('/guests', data)),
  update: (id: number, data: GuestWriteDto) => unwrap<GuestDto>(api.put(`/guests/${id}`, data)),
  reservations: (id: number) => unwrap<ReservationDto[]>(api.get(`/guests/${id}/reservations`)),
  getFull: (id: number, justification: string) =>
    unwrap<GuestFullDto>(api.get(`/guests/${id}/full`, { params: { justification } })),
}

export const roomTypesClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<RoomTypeDto>>(api.get('/room-types', { params })),
  get: (id: number) => unwrap<RoomTypeDto>(api.get(`/room-types/${id}`)),
  create: (data: RoomTypeWriteDto) => unwrap<RoomTypeDto>(api.post('/room-types', data)),
  update: (id: number, data: RoomTypeWriteDto) => unwrap<RoomTypeDto>(api.put(`/room-types/${id}`, data)),
  setStatus: (id: number, data: StatusDto) => unwrap<RoomTypeDto>(api.patch(`/room-types/${id}/status`, data)),
}

export const roomsClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<RoomDto>>(api.get('/rooms', { params })),
  get: (id: number) => unwrap<RoomDto>(api.get(`/rooms/${id}`)),
  create: (data: RoomWriteDto) => unwrap<RoomDto>(api.post('/rooms', data)),
  update: (id: number, data: RoomWriteDto) => unwrap<RoomDto>(api.put(`/rooms/${id}`, data)),
  setStatus: (id: number, data: RoomStatusDto) => unwrap<RoomDto>(api.patch(`/rooms/${id}/status`, data)),
  setObservations: (id: number, data: RoomObservationsDto) =>
    unwrap<RoomDto>(api.patch(`/rooms/${id}/observations`, data)),
}

export const availabilityClient = {
  search: (params: { checkIn: string; checkOut: string; guests: number; roomTypeId?: number }) =>
    unwrap<AvailabilityRoomDto[]>(api.get('/availability', { params })),
  quote: (data: QuoteRequest) => unwrap<QuoteResultDto>(api.post('/availability/quote', data)),
}

export const reservationsClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<ReservationDto>>(api.get('/reservations', { params })),
  get: (id: number) => unwrap<ReservationDto>(api.get(`/reservations/${id}`)),
  create: (data: ReservationCreateDto) => unwrap<ReservationDto>(api.post('/reservations', data)),
  update: (id: number, data: ReservationUpdateDto) => unwrap<ReservationDto>(api.put(`/reservations/${id}`, data)),
  cancel: (id: number) => unwrap<ReservationDto>(api.post(`/reservations/${id}/cancel`)),
  assignRoom: (id: number, data: AssignRoomDto) => unwrap<ReservationDto>(api.post(`/reservations/${id}/assign-room`, data)),
  checkIn: (id: number, data: CheckInDto = {}) => unwrap<ReservationDto>(api.post(`/reservations/${id}/check-in`, data)),
  checkOut: (id: number) => unwrap<ReservationDto>(api.post(`/reservations/${id}/check-out`)),
  modifyStay: (id: number, data: ModifyStayDto) => unwrap<ReservationDto>(api.put(`/reservations/${id}/modify-stay`, data)),
  noShow: (id: number, data: NoShowDto = {}) => unwrap<ReservationDto>(api.post(`/reservations/${id}/no-show`, data)),
  changeRoom: (id: number, data: ChangeRoomDto) => unwrap<ReservationDto>(api.post(`/reservations/${id}/change-room`, data)),
  nightlyRates: (id: number) => unwrap<ReservationNightlyRateDto[]>(api.get(`/reservations/${id}/nightly-rates`)),
  adjustments: (id: number) => unwrap<ReservationAdjustmentDto[]>(api.get(`/reservations/${id}/adjustments`)),
}

export const paymentsClient = {
  listByReservation: (reservationId: number) =>
    unwrap<PaymentDto[]>(api.get(`/reservations/${reservationId}/payments`)),
  create: (reservationId: number, data: PaymentCreateDto) =>
    unwrap<PaymentDto>(api.post(`/reservations/${reservationId}/payments`, data)),
  setStatus: (id: number, data: PaymentStatusDto) => unwrap<PaymentDto>(api.patch(`/payments/${id}/status`, data)),
}

export const dashboardClient = {
  get: (params: { from?: string; to?: string } = {}) => unwrap<DashboardDto>(api.get('/dashboard', { params })),
}

export const ratePlansClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<RatePlanDto>>(api.get('/rate-plans', { params })),
  get: (id: number) => unwrap<RatePlanDto>(api.get(`/rate-plans/${id}`)),
  create: (data: RatePlanCreateDto) => unwrap<RatePlanDto>(api.post('/rate-plans', data)),
  update: (id: number, data: RatePlanCreateDto) => unwrap<RatePlanDto>(api.put(`/rate-plans/${id}`, data)),
  setStatus: (id: number, data: RatePlanStatusDto) => unwrap<RatePlanDto>(api.patch(`/rate-plans/${id}/status`, data)),
}

export const seasonalRatesClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<SeasonalRateDto>>(api.get('/seasonal-rates', { params })),
  create: (data: SeasonalRateCreateDto) => unwrap<SeasonalRateDto>(api.post('/seasonal-rates', data)),
  update: (id: number, data: SeasonalRateCreateDto) => unwrap<SeasonalRateDto>(api.put(`/seasonal-rates/${id}`, data)),
  delete: (id: number) => api.delete(`/seasonal-rates/${id}`),
}

export const dailyRateOverridesClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<DailyRateOverrideDto>>(api.get('/daily-rate-overrides', { params })),
  create: (data: DailyRateOverrideCreateDto) => unwrap<DailyRateOverrideDto>(api.post('/daily-rate-overrides', data)),
  update: (id: number, data: DailyRateOverrideCreateDto) =>
    unwrap<DailyRateOverrideDto>(api.put(`/daily-rate-overrides/${id}`, data)),
  delete: (id: number) => api.delete(`/daily-rate-overrides/${id}`),
}

export const promotionsClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<PromotionRuleDto>>(api.get('/promotion-rules', { params })),
  create: (data: PromotionRuleCreateDto) => unwrap<PromotionRuleDto>(api.post('/promotion-rules', data)),
  update: (id: number, data: PromotionRuleCreateDto) => unwrap<PromotionRuleDto>(api.put(`/promotion-rules/${id}`, data)),
}

export const taxesClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<TaxOrFeeDto>>(api.get('/taxes-and-fees', { params })),
  create: (data: TaxOrFeeCreateDto) => unwrap<TaxOrFeeDto>(api.post('/taxes-and-fees', data)),
  update: (id: number, data: TaxOrFeeCreateDto) => unwrap<TaxOrFeeDto>(api.put(`/taxes-and-fees/${id}`, data)),
}

export const cancellationPoliciesClient = {
  list: () => unwrap<CancellationPolicyDto[]>(api.get('/cancellation-policies')),
  create: (data: CancellationPolicyCreateDto) =>
    unwrap<CancellationPolicyDto>(api.post('/cancellation-policies', data)),
  update: (id: number, data: CancellationPolicyCreateDto) =>
    unwrap<CancellationPolicyDto>(api.put(`/cancellation-policies/${id}`, data)),
}

export const reservationGroupsClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<ReservationGroupDto>>(api.get('/reservation-groups', { params })),
  get: (id: number) => unwrap<ReservationGroupDto>(api.get(`/reservation-groups/${id}`)),
  create: (data: ReservationGroupCreateDto) => unwrap<ReservationGroupDto>(api.post('/reservation-groups', data)),
  cancel: (id: number) => unwrap<{ cancelledCount: number }>(api.post(`/reservation-groups/${id}/cancel`)),
}

export const roomBlocksClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<RoomBlockDto>>(api.get('/room-blocks', { params })),
  create: (data: RoomBlockCreateDto) => unwrap<RoomBlockDto>(api.post('/room-blocks', data)),
  update: (id: number, data: RoomBlockCreateDto) => unwrap<RoomBlockDto>(api.put(`/room-blocks/${id}`, data)),
  release: (id: number) => unwrap<RoomBlockDto>(api.post(`/room-blocks/${id}/release`)),
}

export const housekeepingClient = {
  list: (params: { status?: string; roomId?: number } = {}) =>
    unwrap<HousekeepingTaskDto[]>(api.get('/housekeeping-tasks', { params })),
  create: (data: HousekeepingTaskCreateDto) => unwrap<HousekeepingTaskDto>(api.post('/housekeeping-tasks', data)),
  updateStatus: (id: number, data: HousekeepingStatusUpdateDto) =>
    unwrap<HousekeepingTaskDto>(api.patch(`/housekeeping-tasks/${id}/status`, data)),
}

export const privacyClient = {
  list: (params: ListParams = {}) => unwrap<PageDto<PrivacyRequestDto>>(api.get('/privacy-requests', { params })),
  get: (id: number) => unwrap<PrivacyRequestDto>(api.get(`/privacy-requests/${id}`)),
  create: (data: PrivacyRequestCreateDto) => unwrap<PrivacyRequestDto>(api.post('/privacy-requests', data)),
  update: (id: number, data: PrivacyRequestUpdateDto) => unwrap<PrivacyRequestDto>(api.put(`/privacy-requests/${id}`, data)),
  export: (id: number) => unwrap<GuestFullExportDto>(api.post(`/privacy-requests/${id}/export`)),
  anonymize: (id: number) => unwrap<PrivacyRequestDto>(api.post(`/privacy-requests/${id}/anonymize`)),
  accessLogs: (params: ListParams = {}) =>
    unwrap<PageDto<PersonalDataAccessLogDto>>(api.get('/personal-data-access-logs', { params })),
}

export const adminClient = {
  markNoShows: () => unwrap<{ markedCount: number }>(api.post('/admin/mark-no-shows')),
}

export const generatedApi = {
  auth: authClient,
  users: usersClient,
  guests: guestsClient,
  roomTypes: roomTypesClient,
  rooms: roomsClient,
  availability: availabilityClient,
  reservations: reservationsClient,
  payments: paymentsClient,
  dashboard: dashboardClient,
  ratePlans: ratePlansClient,
  seasonalRates: seasonalRatesClient,
  dailyRateOverrides: dailyRateOverridesClient,
  promotions: promotionsClient,
  taxes: taxesClient,
  cancellationPolicies: cancellationPoliciesClient,
  reservationGroups: reservationGroupsClient,
  roomBlocks: roomBlocksClient,
  housekeeping: housekeepingClient,
  privacy: privacyClient,
  admin: adminClient,
}
