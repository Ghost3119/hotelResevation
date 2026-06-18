export type UserRole = 'ADMIN' | 'RECEPCIONISTA'

export type RoomStatus =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'OCCUPIED'
  | 'CLEANING'
  | 'MAINTENANCE'
  | 'OUT_OF_SERVICE'

export type ReservationStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'CANCELLED'
  | 'NO_SHOW'

export type PaymentMethod = 'CASH' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'BANK_TRANSFER'

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'REFUNDED' | 'CANCELLED'

export interface LoginRequest {
  email: string
  password: string
}

export interface UserDto {
  id: number
  email: string
  fullName: string
  role: UserRole
  active: boolean
}

export interface AuthResponse {
  token: string
  type: string
  expiresIn: number
  user: UserDto
}

export interface PageDto<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface GuestDto {
  id: number
  firstName: string
  lastName: string
  email: string | null
  phone: string | null
  documentNumber: string
  nationality: string
  createdAt: string
}

export interface RoomTypeDto {
  id: number
  name: string
  description: string | null
  maxCapacity: number
  basePrice: number
  amenities: string[]
  active: boolean
}

export interface RoomDto {
  id: number
  number: string
  floor: number
  roomTypeId: number
  roomTypeName: string
  status: RoomStatus
  observations: string | null
}

export interface AvailabilityRoomDto {
  roomId: number
  number: string
  floor: number
  roomTypeId: number
  roomTypeName: string
  maxCapacity: number
  basePrice: number
}

export interface ReservationRoomDto {
  roomId: number
  roomNumber: string
  checkIn: string
  checkOut: string
}

export interface ReservationDto {
  id: number
  status: ReservationStatus
  guestId: number
  guestName: string
  checkIn: string
  checkOut: string
  nights: number
  adults: number
  children: number
  roomTypeId: number
  roomTypeName: string
  rooms: ReservationRoomDto[]
  nightlyPrice: number
  totalAmount: number
  paidAmount: number
  balance: number
  notes: string | null
  specialRequests: string | null
  checkInAt: string | null
  checkOutAt: string | null
  createdAt: string
}

export interface ReservationSummaryDto {
  id: number
  status: ReservationStatus
  guestId: number
  guestName: string
  checkIn: string
  checkOut: string
  nights: number
  totalAmount: number
  balance: number
  roomTypeName: string
}

export interface PaymentDto {
  id: number
  reservationId: number
  amount: number
  method: PaymentMethod
  status: PaymentStatus
  reference: string | null
  paidAt: string | null
}

export interface DashboardDto {
  arrivalsToday: number
  departuresToday: number
  occupiedRooms: number
  availableRooms: number
  cleaningRooms: number
  occupancyRate: number
  incomePeriod: number
  recentReservations: ReservationSummaryDto[]
}

export interface FieldError {
  field: string
  message: string
}

export interface ApiError {
  timestamp: string
  status: number
  error: string
  code: string
  message: string
  path: string
  fieldErrors: FieldError[]
}

export interface NormalizedError {
  code: string
  message: string
  status?: number
  fieldErrors: FieldError[]
}
