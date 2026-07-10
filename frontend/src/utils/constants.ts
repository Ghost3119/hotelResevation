import type {
  AdjustmentType,
  BlockType,
  DiscountType,
  HousekeepingPriority,
  HousekeepingStatus,
  PaymentMethod,
  PaymentStatus,
  PenaltyType,
  PriceMode,
  PrivacyRequestStatus,
  PrivacyRequestType,
  ReservationStatus,
  RoomStatus,
  SeasonType,
  TaxAppliesTo,
  TaxType,
  UserRole,
} from '../api/types'

export const ROUTES = {
  LOGIN: '/login',
  DASHBOARD: '/dashboard',
  GUESTS: '/guests',
  guestReservations: (id: number | string) => `/guests/${id}/reservations`,
  RESERVATIONS: '/reservations',
  RESERVATION_NEW: '/reservations/new',
  reservationDetail: (id: number | string) => `/reservations/${id}`,
  AVAILABILITY: '/availability',
  ROOMS: '/rooms',
  ROOM_TYPES: '/room-types',
  USERS: '/users',
  PAYMENTS: '/payments',
  QUOTE: '/quote',
  RATE_PLANS: '/rate-plans',
  SEASONAL_RATES: '/seasonal-rates',
  TAXES_AND_FEES: '/taxes-and-fees',
  CANCELLATION_POLICIES: '/cancellation-policies',
  ROOM_BLOCKS: '/room-blocks',
  RESERVATION_GROUPS: '/reservation-groups',
  HOUSEKEEPING: '/housekeeping',
  OCCUPANCY_CALENDAR: '/occupancy-calendar',
  PRIVACY_REQUESTS: '/privacy-requests',
  FORBIDDEN: '/403',
} as const

export const ROOM_STATUS_LABELS: Record<RoomStatus, string> = {
  AVAILABLE: 'Disponible',
  RESERVED: 'Reservada',
  OCCUPIED: 'Ocupada',
  CLEANING: 'Limpieza',
  MAINTENANCE: 'Mantenimiento',
  OUT_OF_SERVICE: 'Fuera de servicio',
}

export const ROOM_STATUS_BADGE: Record<RoomStatus, string> = {
  AVAILABLE: 'bg-green-100 text-green-800',
  RESERVED: 'bg-blue-100 text-blue-800',
  OCCUPIED: 'bg-red-100 text-red-800',
  CLEANING: 'bg-yellow-100 text-yellow-800',
  MAINTENANCE: 'bg-orange-100 text-orange-800',
  OUT_OF_SERVICE: 'bg-gray-200 text-gray-800',
}

export const RESERVATION_STATUS_LABELS: Record<ReservationStatus, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  CHECKED_IN: 'Check-in realizado',
  CHECKED_OUT: 'Check-out realizado',
  CANCELLED: 'Cancelada',
  NO_SHOW: 'No show',
}

export const RESERVATION_STATUS_BADGE: Record<ReservationStatus, string> = {
  PENDING: 'bg-gray-200 text-gray-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  CHECKED_IN: 'bg-green-100 text-green-800',
  CHECKED_OUT: 'bg-purple-100 text-purple-800',
  CANCELLED: 'bg-red-100 text-red-800',
  NO_SHOW: 'bg-orange-100 text-orange-800',
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pendiente',
  COMPLETED: 'Completado',
  REFUNDED: 'Reembolsado',
  CANCELLED: 'Cancelado',
}

export const PAYMENT_STATUS_BADGE: Record<PaymentStatus, string> = {
  PENDING: 'bg-gray-200 text-gray-800',
  COMPLETED: 'bg-green-100 text-green-800',
  REFUNDED: 'bg-orange-100 text-orange-800',
  CANCELLED: 'bg-red-100 text-red-800',
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  CREDIT_CARD: 'Tarjeta de crédito',
  DEBIT_CARD: 'Tarjeta de débito',
  BANK_TRANSFER: 'Transferencia bancaria',
}

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  RECEPCIONISTA: 'Recepcionista',
  MANAGER: 'Gerente',
  HOUSEKEEPING: 'Limpieza',
  PRIVACY_OFFICER: 'Oficial de privacidad',
}

export const ROOM_STATUSES: RoomStatus[] = [
  'AVAILABLE',
  'RESERVED',
  'OCCUPIED',
  'CLEANING',
  'MAINTENANCE',
  'OUT_OF_SERVICE',
]

export const RESERVATION_STATUSES: ReservationStatus[] = [
  'PENDING',
  'CONFIRMED',
  'CHECKED_IN',
  'CHECKED_OUT',
  'CANCELLED',
  'NO_SHOW',
]

export const PAYMENT_METHODS: PaymentMethod[] = [
  'CASH',
  'CREDIT_CARD',
  'DEBIT_CARD',
  'BANK_TRANSFER',
]

export const PAYMENT_STATUSES: PaymentStatus[] = [
  'PENDING',
  'COMPLETED',
  'REFUNDED',
  'CANCELLED',
]

export const PAGE_SIZE_DEFAULT = 10

/* ---------- Shared Tailwind class strings (UI primitives) ---------- */

export const BTN =
  'inline-flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap'
export const BTN_PRIMARY = `${BTN} bg-blue-600 text-white hover:bg-blue-700 border border-blue-600`
export const BTN_SECONDARY = `${BTN} bg-white text-slate-700 border border-slate-300 hover:bg-slate-50`
export const BTN_DANGER = `${BTN} bg-red-600 text-white hover:bg-red-700 border border-red-600`
export const BTN_GHOST = `${BTN} bg-transparent text-slate-600 border border-transparent hover:bg-slate-100`
export const BTN_SM = 'px-2.5 py-1 text-xs'
export const BTN_BLOCK = 'w-full justify-center'

export const CARD = 'rounded-lg border border-slate-200 bg-white shadow-sm'
export const CARD_BODY = 'p-4'
export const CARD_HEADER =
  'flex items-center justify-between border-b border-slate-200 px-4 py-3'

export const INPUT =
  'w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 bg-white focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-slate-50 disabled:text-slate-500'
export const INPUT_ERROR = 'border-red-500'

export const FORM_FIELD = 'flex flex-col gap-1 mb-3.5'
export const FORM_LABEL = 'text-sm font-medium text-slate-600'
export const FORM_ERROR = 'text-xs text-red-600'
export const FORM_HINT = 'text-xs text-slate-500'
export const FORM_ALERT_ERROR =
  'mb-3 rounded-md border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700'
export const FORM_ALERT_SUCCESS =
  'mb-3 rounded-md border border-green-200 bg-green-50 px-3 py-2.5 text-sm text-green-700'
export const FORM_ROW =
  'grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(180px,1fr))]'
export const FORM_ACTIONS = 'mt-2 flex justify-end gap-2'

export const BADGE_BASE =
  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap'

export const DATA_TABLE =
  'overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm'
export const TABLE_TH =
  'border-b border-slate-200 bg-slate-50 px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 whitespace-nowrap'
export const TABLE_TD = 'border-b border-slate-200 px-3 py-2.5 align-middle text-slate-900'
export const TABLE_EMPTY_TD = 'px-3 py-7 text-center text-slate-500'
export const ROW_CLICKABLE = 'cursor-pointer hover:bg-slate-50'

/* ---------- Evolved domain labels (ER-1..ER-30) ---------- */

export const WEEKDAY_LABELS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'] as const

export const HOUSEKEEPING_STATUSES: HousekeepingStatus[] = [
  'DIRTY',
  'CLEANING',
  'INSPECTED',
  'READY',
]

export const HOUSEKEEPING_STATUS_LABELS: Record<HousekeepingStatus, string> = {
  DIRTY: 'Sucio',
  CLEANING: 'Limpiando',
  INSPECTED: 'Inspeccionado',
  READY: 'Listo',
}

export const HOUSEKEEPING_STATUS_BADGE: Record<HousekeepingStatus, string> = {
  DIRTY: 'bg-red-100 text-red-800',
  CLEANING: 'bg-yellow-100 text-yellow-800',
  INSPECTED: 'bg-blue-100 text-blue-800',
  READY: 'bg-green-100 text-green-800',
}

export const HOUSEKEEPING_PRIORITIES: HousekeepingPriority[] = [
  'LOW',
  'NORMAL',
  'HIGH',
  'URGENT',
]

export const HOUSEKEEPING_PRIORITY_LABELS: Record<HousekeepingPriority, string> = {
  LOW: 'Baja',
  NORMAL: 'Normal',
  HIGH: 'Alta',
  URGENT: 'Urgente',
}

export const HOUSEKEEPING_PRIORITY_BADGE: Record<HousekeepingPriority, string> = {
  LOW: 'bg-gray-100 text-gray-700',
  NORMAL: 'bg-slate-200 text-slate-700',
  HIGH: 'bg-orange-100 text-orange-800',
  URGENT: 'bg-red-100 text-red-800',
}

export const SEASON_TYPES: SeasonType[] = ['ALTA', 'MEDIA', 'BAJA']
export const SEASON_TYPE_LABELS: Record<SeasonType, string> = {
  ALTA: 'Alta',
  MEDIA: 'Media',
  BAJA: 'Baja',
}

export const PRICE_MODES: PriceMode[] = ['MULTIPLIER', 'ABSOLUTE']
export const PRICE_MODE_LABELS: Record<PriceMode, string> = {
  MULTIPLIER: 'Multiplicador',
  ABSOLUTE: 'Precio absoluto',
}

export const TAX_TYPES: TaxType[] = ['TAX_PERCENT', 'FEE_FIXED']
export const TAX_TYPE_LABELS: Record<TaxType, string> = {
  TAX_PERCENT: 'Impuesto (%)',
  FEE_FIXED: 'Cargo fijo',
}

export const TAX_APPLIES_TO: TaxAppliesTo[] = ['ROOM_RATE', 'TOTAL', 'PER_NIGHT']
export const TAX_APPLIES_TO_LABELS: Record<TaxAppliesTo, string> = {
  ROOM_RATE: 'Tarifa de habitación',
  TOTAL: 'Total',
  PER_NIGHT: 'Por noche',
}

export const DISCOUNT_TYPES: DiscountType[] = ['PERCENTAGE', 'FIXED']
export const DISCOUNT_TYPE_LABELS: Record<DiscountType, string> = {
  PERCENTAGE: 'Porcentaje',
  FIXED: 'Importe fijo',
}

export const PENALTY_TYPES: PenaltyType[] = ['NONE', 'PERCENTAGE', 'FIXED', 'FIRST_NIGHT']
export const PENALTY_TYPE_LABELS: Record<PenaltyType, string> = {
  NONE: 'Sin penalización',
  PERCENTAGE: 'Porcentaje',
  FIXED: 'Importe fijo',
  FIRST_NIGHT: 'Primera noche',
}

export const BLOCK_TYPES: BlockType[] = ['MAINTENANCE', 'OPERATIONAL']
export const BLOCK_TYPE_LABELS: Record<BlockType, string> = {
  MAINTENANCE: 'Mantenimiento',
  OPERATIONAL: 'Operativo',
}

export const ADJUSTMENT_TYPE_LABELS: Record<AdjustmentType, string> = {
  EXTEND: 'Extensión',
  REDUCE: 'Reducción',
  CHANGE_DATES: 'Cambio de fechas',
  CHANGE_ROOM: 'Cambio de habitación',
  CANCEL: 'Cancelación',
  NO_SHOW: 'No-show',
  PENALTY: 'Penalización',
}

export const PRIVACY_REQUEST_TYPES: PrivacyRequestType[] = ['EXPORT', 'RECTIFY', 'DELETE']
export const PRIVACY_REQUEST_TYPE_LABELS: Record<PrivacyRequestType, string> = {
  EXPORT: 'Exportación',
  RECTIFY: 'Rectificación',
  DELETE: 'Eliminación',
}

export const PRIVACY_REQUEST_STATUSES: PrivacyRequestStatus[] = [
  'PENDING',
  'IN_PROGRESS',
  'COMPLETED',
  'REJECTED',
]
export const PRIVACY_REQUEST_STATUS_LABELS: Record<PrivacyRequestStatus, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Completada',
  REJECTED: 'Rechazada',
}
export const PRIVACY_REQUEST_STATUS_BADGE: Record<PrivacyRequestStatus, string> = {
  PENDING: 'bg-gray-200 text-gray-800',
  IN_PROGRESS: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
}

export const ROLE_GROUPS = {
  CONFIG: ['ADMIN', 'MANAGER'] as UserRole[],
  OPERATION_HOUSEKEEPING: ['ADMIN', 'MANAGER', 'HOUSEKEEPING'] as UserRole[],
  PRIVACY: ['PRIVACY_OFFICER'] as UserRole[],
} as const
