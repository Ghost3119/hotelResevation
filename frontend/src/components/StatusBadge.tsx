import clsx from 'clsx'
import type {
  PaymentStatus,
  ReservationStatus,
  RoomStatus,
} from '../api/types'
import {
  PAYMENT_STATUS_BADGE,
  PAYMENT_STATUS_LABELS,
  RESERVATION_STATUS_BADGE,
  RESERVATION_STATUS_LABELS,
  ROOM_STATUS_BADGE,
  ROOM_STATUS_LABELS,
} from '../utils/constants'

export function RoomStatusBadge({ status }: { status: RoomStatus }) {
  return (
    <span className={clsx('badge', ROOM_STATUS_BADGE[status])}>
      {ROOM_STATUS_LABELS[status]}
    </span>
  )
}

export function ReservationStatusBadge({ status }: { status: ReservationStatus }) {
  return (
    <span className={clsx('badge', RESERVATION_STATUS_BADGE[status])}>
      {RESERVATION_STATUS_LABELS[status]}
    </span>
  )
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return (
    <span className={clsx('badge', PAYMENT_STATUS_BADGE[status])}>
      {PAYMENT_STATUS_LABELS[status]}
    </span>
  )
}
