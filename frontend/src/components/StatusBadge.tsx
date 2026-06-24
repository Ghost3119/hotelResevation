import clsx from 'clsx'
import type {
  HousekeepingPriority,
  HousekeepingStatus,
  PaymentStatus,
  PrivacyRequestStatus,
  ReservationStatus,
  RoomStatus,
} from '../api/types'
import { BADGE_BASE } from '../utils/constants'
import {
  HOUSEKEEPING_PRIORITY_BADGE,
  HOUSEKEEPING_PRIORITY_LABELS,
  HOUSEKEEPING_STATUS_BADGE,
  HOUSEKEEPING_STATUS_LABELS,
  PAYMENT_STATUS_BADGE,
  PAYMENT_STATUS_LABELS,
  PRIVACY_REQUEST_STATUS_BADGE,
  PRIVACY_REQUEST_STATUS_LABELS,
  RESERVATION_STATUS_BADGE,
  RESERVATION_STATUS_LABELS,
  ROOM_STATUS_BADGE,
  ROOM_STATUS_LABELS,
} from '../utils/constants'

export function RoomStatusBadge({ status }: { status: RoomStatus }) {
  return (
    <span className={clsx(BADGE_BASE, ROOM_STATUS_BADGE[status])}>
      {ROOM_STATUS_LABELS[status]}
    </span>
  )
}

export function ReservationStatusBadge({ status }: { status: ReservationStatus }) {
  return (
    <span className={clsx(BADGE_BASE, RESERVATION_STATUS_BADGE[status])}>
      {RESERVATION_STATUS_LABELS[status]}
    </span>
  )
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return (
    <span className={clsx(BADGE_BASE, PAYMENT_STATUS_BADGE[status])}>
      {PAYMENT_STATUS_LABELS[status]}
    </span>
  )
}

export function HousekeepingStatusBadge({ status }: { status: HousekeepingStatus }) {
  return (
    <span className={clsx(BADGE_BASE, HOUSEKEEPING_STATUS_BADGE[status])}>
      {HOUSEKEEPING_STATUS_LABELS[status]}
    </span>
  )
}

export function HousekeepingPriorityBadge({ priority }: { priority: HousekeepingPriority }) {
  return (
    <span className={clsx(BADGE_BASE, HOUSEKEEPING_PRIORITY_BADGE[priority])}>
      {HOUSEKEEPING_PRIORITY_LABELS[priority]}
    </span>
  )
}

export function PrivacyRequestStatusBadge({ status }: { status: PrivacyRequestStatus }) {
  return (
    <span className={clsx(BADGE_BASE, PRIVACY_REQUEST_STATUS_BADGE[status])}>
      {PRIVACY_REQUEST_STATUS_LABELS[status]}
    </span>
  )
}
