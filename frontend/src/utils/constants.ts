import type {
  PaymentMethod,
  PaymentStatus,
  ReservationStatus,
  RoomStatus,
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
  FORBIDDEN: '/403',
} as const

export const STORAGE_TOKEN_KEY = 'hotel.token'

export const ROOM_STATUS_LABELS: Record<RoomStatus, string> = {
  AVAILABLE: 'Disponible',
  RESERVED: 'Reservada',
  OCCUPIED: 'Ocupada',
  CLEANING: 'Limpieza',
  MAINTENANCE: 'Mantenimiento',
  OUT_OF_SERVICE: 'Fuera de servicio',
}

export const ROOM_STATUS_BADGE: Record<RoomStatus, string> = {
  AVAILABLE: 'badge-green',
  RESERVED: 'badge-blue',
  OCCUPIED: 'badge-red',
  CLEANING: 'badge-yellow',
  MAINTENANCE: 'badge-orange',
  OUT_OF_SERVICE: 'badge-gray',
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
  PENDING: 'badge-gray',
  CONFIRMED: 'badge-blue',
  CHECKED_IN: 'badge-green',
  CHECKED_OUT: 'badge-purple',
  CANCELLED: 'badge-red',
  NO_SHOW: 'badge-orange',
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pendiente',
  COMPLETED: 'Completado',
  REFUNDED: 'Reembolsado',
  CANCELLED: 'Cancelado',
}

export const PAYMENT_STATUS_BADGE: Record<PaymentStatus, string> = {
  PENDING: 'badge-gray',
  COMPLETED: 'badge-green',
  REFUNDED: 'badge-orange',
  CANCELLED: 'badge-red',
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
