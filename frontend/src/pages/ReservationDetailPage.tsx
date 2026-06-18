import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { Amount } from '../components/Amount'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Input, Select } from '../components/FormField'
import { PaymentStatusBadge, ReservationStatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/Toast'
import { useAssignRoom, useCancelReservation, useCheckIn, useCheckOut, useReservation } from '../hooks/useReservations'
import { useCreatePayment, useReservationPayments, useSetPaymentStatus } from '../hooks/usePayments'
import { useAvailability } from '../hooks/useAvailability'
import type { NormalizedError, PaymentMethod, PaymentStatus } from '../api/types'
import { getFieldErrors } from '../utils/error'
import { formatCurrency, formatDate, formatDateTime, todayISO } from '../utils/format'
import {
  PAYMENT_METHODS,
  PAYMENT_METHOD_LABELS,
  ROUTES,
} from '../utils/constants'

export function ReservationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const reservationId = Number(id)
  const toast = useToast()

  const { data: reservation, isLoading, isError, error, refetch } = useReservation(reservationId)
  const paymentsQ = useReservationPayments(reservationId)

  const assignMut = useAssignRoom()
  const cancelMut = useCancelReservation()
  const checkInMut = useCheckIn()
  const checkOutMut = useCheckOut()
  const createPaymentMut = useCreatePayment()
  const setPaymentStatusMut = useSetPaymentStatus()

  const [assignOpen, setAssignOpen] = useState(false)
  const [assignRoomId, setAssignRoomId] = useState<number | ''>('')

  const [cancelOpen, setCancelOpen] = useState(false)

  const [checkOutOpen, setCheckOutOpen] = useState(false)

  const [payOpen, setPayOpen] = useState(false)
  const [payAmount, setPayAmount] = useState('')
  const [payMethod, setPayMethod] = useState<PaymentMethod>('CASH')
  const [payReference, setPayReference] = useState('')
  const [payErrors, setPayErrors] = useState<Record<string, string>>({})
  const [paySubmitError, setPaySubmitError] = useState<string | null>(null)

  const availabilityParams =
    reservation && (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED')
      ? {
          checkIn: reservation.checkIn,
          checkOut: reservation.checkOut,
          guests: reservation.adults + reservation.children,
          roomTypeId: reservation.roomTypeId,
        }
      : null
  const availabilityQ = useAvailability(availabilityParams)

  if (isLoading) return <LoadingState />
  if (isError) return <ErrorState error={error} onRetry={() => refetch()} />
  if (!reservation) return <ErrorState message="No se encontró la reserva." />

  const canCancel =
    (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') &&
    reservation.checkIn > todayISO()
  const canCheckIn = reservation.status === 'CONFIRMED'
  const canCheckOut = reservation.status === 'CHECKED_IN'

  const handleAssign = async (e: FormEvent) => {
    e.preventDefault()
    if (!assignRoomId) return
    try {
      await assignMut.mutateAsync({ id: reservation.id, data: { roomId: Number(assignRoomId) } })
      toast.success('Habitación asignada.')
      setAssignOpen(false)
      setAssignRoomId('')
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const handleCancel = async () => {
    try {
      await cancelMut.mutateAsync(reservation.id)
      toast.success('Reserva cancelada.')
      setCancelOpen(false)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const handleCheckIn = async () => {
    try {
      await checkInMut.mutateAsync({ id: reservation.id, data: {} })
      toast.success('Check-in realizado.')
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const handleCheckOut = async () => {
    try {
      await checkOutMut.mutateAsync(reservation.id)
      toast.success('Check-out realizado.')
      setCheckOutOpen(false)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const handlePay = async (e: FormEvent) => {
    e.preventDefault()
    setPayErrors({})
    setPaySubmitError(null)
    const amount = Number(payAmount)
    if (!payAmount || Number.isNaN(amount)) {
      setPayErrors({ amount: 'Indique un importe válido.' })
      return
    }
    if (amount <= 0) {
      setPayErrors({ amount: 'El importe debe ser mayor que cero.' })
      return
    }
    try {
      await createPaymentMut.mutateAsync({
        reservationId: reservation.id,
        data: { amount, method: payMethod, reference: payReference.trim() || null },
      })
      toast.success('Pago registrado.')
      setPayOpen(false)
      setPayAmount('')
      setPayReference('')
    } catch (err) {
      const normalized = err as NormalizedError
      setPayErrors(getFieldErrors(err))
      setPaySubmitError(normalized.message)
    }
  }

  const handlePaymentStatus = async (paymentId: number, status: PaymentStatus) => {
    try {
      await setPaymentStatusMut.mutateAsync({
        id: paymentId,
        data: { status },
        reservationId: reservation.id,
      })
      toast.success('Estado de pago actualizado.')
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  return (
    <div>
      <PageHeader
        title={`Reserva #${reservation.id}`}
        subtitle={`${reservation.guestName} · ${reservation.roomTypeName}`}
        actions={
          <div className="flex-gap">
            <Link to={ROUTES.RESERVATIONS} className="btn btn-secondary">
              Volver
            </Link>
            {(reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setAssignRoomId('')
                  setAssignOpen(true)
                }}
              >
                Asignar habitación
              </button>
            )}
            <button type="button" className="btn btn-primary" onClick={() => setPayOpen(true)}>
              Registrar pago
            </button>
            {canCheckIn && (
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleCheckIn}
                disabled={checkInMut.isPending}
              >
                Check-in
              </button>
            )}
            {canCheckOut && (
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setCheckOutOpen(true)}
              >
                Check-out
              </button>
            )}
            {canCancel && (
              <button
                type="button"
                className="btn btn-danger"
                onClick={() => setCancelOpen(true)}
              >
                Cancelar reserva
              </button>
            )}
          </div>
        }
      />

      <div className="card card-body">
        <div className="flex-gap" style={{ marginBottom: 12 }}>
          <ReservationStatusBadge status={reservation.status} />
        </div>
        <div className="detail-grid">
          <div className="detail-item">
            <span className="detail-label">Huésped</span>
            <span className="detail-value">{reservation.guestName}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Entrada</span>
            <span className="detail-value">{formatDate(reservation.checkIn)}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Salida</span>
            <span className="detail-value">{formatDate(reservation.checkOut)}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Noches</span>
            <span className="detail-value">{reservation.nights}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Adultos / Niños</span>
            <span className="detail-value">{reservation.adults} / {reservation.children}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Precio/noche</span>
            <span className="detail-value"><Amount value={reservation.nightlyPrice} /></span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Total</span>
            <span className="detail-value"><Amount value={reservation.totalAmount} /></span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Pagado</span>
            <span className="detail-value"><Amount value={reservation.paidAmount} /></span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Saldo</span>
            <span className="detail-value"><Amount value={reservation.balance} /></span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Check-in en</span>
            <span className="detail-value">{formatDateTime(reservation.checkInAt)}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Check-out en</span>
            <span className="detail-value">{formatDateTime(reservation.checkOutAt)}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Creada</span>
            <span className="detail-value">{formatDateTime(reservation.createdAt)}</span>
          </div>
        </div>

        {reservation.notes && (
          <p className="muted" style={{ marginTop: 12 }}>
            <strong>Notas:</strong> {reservation.notes}
          </p>
        )}
        {reservation.specialRequests && (
          <p className="muted" style={{ marginTop: 4 }}>
            <strong>Peticiones especiales:</strong> {reservation.specialRequests}
          </p>
        )}
      </div>

      <h3 className="section-title">Habitaciones asignadas</h3>
      <div className="data-table">
        <table>
          <thead>
            <tr>
              <th>Habitación</th>
              <th>Entrada</th>
              <th>Salida</th>
            </tr>
          </thead>
          <tbody>
            {reservation.rooms.length === 0 ? (
              <tr className="data-table-empty-row">
                <td colSpan={3}>Sin habitaciones asignadas.</td>
              </tr>
            ) : (
              reservation.rooms.map((rr) => (
                <tr key={rr.roomId}>
                  <td>{rr.roomNumber}</td>
                  <td>{formatDate(rr.checkIn)}</td>
                  <td>{formatDate(rr.checkOut)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <h3 className="section-title">Pagos</h3>
      {paymentsQ.isLoading && <LoadingState />}
      {paymentsQ.isError && <ErrorState error={paymentsQ.error} onRetry={() => paymentsQ.refetch()} />}
      {paymentsQ.data && (
        <div className="data-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th className="text-right">Importe</th>
                <th>Método</th>
                <th>Estado</th>
                <th>Referencia</th>
                <th>Fecha</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {paymentsQ.data.length === 0 ? (
                <tr className="data-table-empty-row">
                  <td colSpan={7}>Sin pagos registrados.</td>
                </tr>
              ) : (
                paymentsQ.data.map((p) => (
                  <tr key={p.id}>
                    <td>#{p.id}</td>
                    <td className="text-right"><Amount value={p.amount} /></td>
                    <td>{PAYMENT_METHOD_LABELS[p.method]}</td>
                    <td><PaymentStatusBadge status={p.status} /></td>
                    <td>{p.reference ?? '—'}</td>
                    <td>{formatDateTime(p.paidAt)}</td>
                    <td>
                      <div className="flex-gap">
                        {p.status === 'COMPLETED' && (
                          <>
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => handlePaymentStatus(p.id, 'REFUNDED')}
                            >
                              Reembolsar
                            </button>
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => handlePaymentStatus(p.id, 'CANCELLED')}
                            >
                              Anular
                            </button>
                          </>
                        )}
                        {p.status === 'PENDING' && (
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => handlePaymentStatus(p.id, 'COMPLETED')}
                          >
                            Completar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Assign room modal */}
      <Modal
        open={assignOpen}
        title="Asignar habitación"
        onClose={() => setAssignOpen(false)}
        size="md"
      >
        <form onSubmit={handleAssign} noValidate>
          {availabilityQ.isLoading && <LoadingState />}
          {availabilityQ.isError && <ErrorState error={availabilityQ.error} />}
          {availabilityQ.data && (
            <Select
              label="Habitación disponible"
              name="roomId"
              value={assignRoomId}
              onChange={(e) => setAssignRoomId(e.target.value ? Number(e.target.value) : '')}
              required
            >
              <option value="">Seleccione…</option>
              {availabilityQ.data.map((room) => (
                <option key={room.roomId} value={room.roomId}>
                  Hab. {room.number} · piso {room.floor} · {room.roomTypeName}
                </option>
              ))}
            </Select>
          )}
          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setAssignOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={assignMut.isPending} label="Asignar" />
          </div>
        </form>
      </Modal>

      {/* Payment modal */}
      <Modal
        open={payOpen}
        title="Registrar pago"
        onClose={() => setPayOpen(false)}
        size="md"
      >
        <form onSubmit={handlePay} noValidate>
          {paySubmitError && (
            <div className="form-alert form-alert-error" role="alert">{paySubmitError}</div>
          )}
          <div className="detail-grid" style={{ marginBottom: 12 }}>
            <div className="detail-item">
              <span className="detail-label">Total</span>
              <span className="detail-value">{formatCurrency(reservation.totalAmount)}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Saldo pendiente</span>
              <span className="detail-value">{formatCurrency(reservation.balance)}</span>
            </div>
          </div>
          <Input
            label="Importe (EUR)"
            name="amount"
            type="number"
            step="0.01"
            min="0.01"
            value={payAmount}
            onChange={(e) => setPayAmount(e.target.value)}
            error={payErrors.amount}
            required
          />
          <Select
            label="Método de pago"
            name="method"
            value={payMethod}
            onChange={(e) => setPayMethod(e.target.value as PaymentMethod)}
            required
          >
            {PAYMENT_METHODS.map((m) => (
              <option key={m} value={m}>
                {PAYMENT_METHOD_LABELS[m]}
              </option>
            ))}
          </Select>
          <Input
            label="Referencia (opcional)"
            name="reference"
            value={payReference}
            onChange={(e) => setPayReference(e.target.value)}
            error={payErrors.reference}
          />
          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={() => setPayOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createPaymentMut.isPending} label="Registrar pago" />
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={cancelOpen}
        title="Cancelar reserva"
        message={`¿Confirmar la cancelación de la reserva #${reservation.id}? Esta acción libera la habitación asignada.`}
        confirmLabel="Sí, cancelar"
        destructive
        loading={cancelMut.isPending}
        onConfirm={handleCancel}
        onCancel={() => setCancelOpen(false)}
      />

      <ConfirmDialog
        open={checkOutOpen}
        title="Check-out"
        message={
          reservation.balance > 0
            ? `La reserva tiene un saldo pendiente de ${formatCurrency(reservation.balance)}. ¿Confirmar check-out de todas formas?`
            : `¿Confirmar el check-out de la reserva #${reservation.id}? La habitación pasará a limpieza.`
        }
        confirmLabel="Confirmar check-out"
        loading={checkOutMut.isPending}
        onConfirm={handleCheckOut}
        onCancel={() => setCheckOutOpen(false)}
      />
    </div>
  )
}
