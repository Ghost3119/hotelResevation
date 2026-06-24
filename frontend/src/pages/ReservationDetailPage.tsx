import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { Amount } from '../components/Amount'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Input, Select, TextArea } from '../components/FormField'
import { PaymentStatusBadge, ReservationStatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/Toast'
import {
  useAssignRoom,
  useCancelReservation,
  useChangeRoom,
  useCheckIn,
  useCheckOut,
  useModifyStay,
  useNoShow,
  useReservation,
} from '../hooks/useReservations'
import { useCreatePayment, useReservationPayments, useSetPaymentStatus } from '../hooks/usePayments'
import { useAvailability } from '../hooks/useAvailability'
import { useNightlyRates } from '../hooks/useNightlyRates'
import { useAdjustments } from '../hooks/useAdjustments'
import { useRooms } from '../hooks/useRooms'
import type { NormalizedError, PaymentMethod, PaymentStatus } from '../api/types'
import type {
  ModifyStayDto,
  NoShowDto,
} from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatCurrency, formatDate, formatDateTime, todayISO } from '../utils/format'
import {
  ADJUSTMENT_TYPE_LABELS,
  PAYMENT_METHODS,
  PAYMENT_METHOD_LABELS,
  ROUTES,
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  CARD,
  CARD_BODY,
  DATA_TABLE,
  FORM_ALERT_ERROR,
  FORM_ACTIONS,
  TABLE_TH,
  TABLE_TD,
  TABLE_EMPTY_TD,
} from '../utils/constants'

const DETAIL_GRID = 'grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]'
const DETAIL_LABEL = 'text-xs uppercase tracking-wide text-slate-500'
const DETAIL_VALUE = 'font-semibold text-slate-900'

export function ReservationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const reservationId = Number(id)
  const toast = useToast()

  const { data: reservation, isLoading, isError, error, refetch } = useReservation(reservationId)
  const paymentsQ = useReservationPayments(reservationId)
  const nightlyQ = useNightlyRates(reservationId)
  const adjustmentsQ = useAdjustments(reservationId)
  const roomsQ = useRooms({ size: 200 })

  const assignMut = useAssignRoom()
  const cancelMut = useCancelReservation()
  const checkInMut = useCheckIn()
  const checkOutMut = useCheckOut()
  const createPaymentMut = useCreatePayment()
  const setPaymentStatusMut = useSetPaymentStatus()
  const modifyStayMut = useModifyStay()
  const changeRoomMut = useChangeRoom()
  const noShowMut = useNoShow()

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

  const [activeTab, setActiveTab] = useState<'payments' | 'nightly' | 'adjustments'>('payments')

  // Modify stay (ER-13)
  const [modifyOpen, setModifyOpen] = useState(false)
  const [modifyForm, setModifyForm] = useState({ newCheckIn: '', newCheckOut: '', reason: '' })
  const [modifyPending, setModifyPending] = useState<ModifyStayDto | null>(null)

  // Change room (ER-16)
  const [changeRoomOpen, setChangeRoomOpen] = useState(false)
  const [changeRoomForm, setChangeRoomForm] = useState({ newRoomId: '', reason: '' })
  const [changeRoomPending, setChangeRoomPending] = useState<{ newRoomId: number; reason: string | null } | null>(null)

  // No-show (ER-11)
  const [noShowOpen, setNoShowOpen] = useState(false)
  const [noShowReason, setNoShowReason] = useState('')

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
  // ER-13: modify stay only for PENDING/CONFIRMED with future checkIn
  const canModifyStay =
    (reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') &&
    reservation.checkIn > todayISO()
  // ER-16: change room only for CHECKED_IN
  const canChangeRoom = reservation.status === 'CHECKED_IN'
  // ER-11: manual no-show for CONFIRMED with checkIn <= today
  const canNoShow = reservation.status === 'CONFIRMED' && reservation.checkIn <= todayISO()

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

  const openModify = () => {
    setModifyForm({ newCheckIn: reservation.checkIn, newCheckOut: reservation.checkOut, reason: '' })
    setModifyPending(null)
    setModifyOpen(true)
  }

  const reviewModify = (e: FormEvent) => {
    e.preventDefault()
    if (!modifyForm.newCheckIn || !modifyForm.newCheckOut) return
    if (modifyForm.newCheckOut <= modifyForm.newCheckIn) {
      toast.error('La salida debe ser posterior a la entrada.')
      return
    }
    setModifyPending({
      newCheckIn: modifyForm.newCheckIn,
      newCheckOut: modifyForm.newCheckOut,
      reason: modifyForm.reason.trim() || null,
    })
    setModifyOpen(false)
  }

  const applyModify = async () => {
    if (!modifyPending) return
    try {
      const updated = await modifyStayMut.mutateAsync({ id: reservation.id, data: modifyPending })
      toast.success(`Estancia modificada. Nuevo total: ${formatCurrency(updated.totalAmount)}.`)
      setModifyPending(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const openChangeRoom = () => {
    setChangeRoomForm({ newRoomId: '', reason: '' })
    setChangeRoomPending(null)
    setChangeRoomOpen(true)
  }

  const reviewChangeRoom = (e: FormEvent) => {
    e.preventDefault()
    if (!changeRoomForm.newRoomId) return
    setChangeRoomPending({
      newRoomId: Number(changeRoomForm.newRoomId),
      reason: changeRoomForm.reason.trim() || null,
    })
    setChangeRoomOpen(false)
  }

  const applyChangeRoom = async () => {
    if (!changeRoomPending) return
    try {
      await changeRoomMut.mutateAsync({ id: reservation.id, data: changeRoomPending })
      toast.success('Habitación cambiada.')
      setChangeRoomPending(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const handleNoShow = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const data: NoShowDto = { reason: noShowReason.trim() || null }
      await noShowMut.mutateAsync({ id: reservation.id, data })
      toast.success('Reserva marcada como no-show.')
      setNoShowOpen(false)
      setNoShowReason('')
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
          <div className="flex flex-wrap gap-2">
            <Link to={ROUTES.RESERVATIONS} className={BTN_SECONDARY}>
              Volver
            </Link>
            {(reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') && (
              <button
                type="button"
                className={BTN_SECONDARY}
                onClick={() => {
                  setAssignRoomId('')
                  setAssignOpen(true)
                }}
              >
                Asignar habitación
              </button>
            )}
            <button type="button" className={BTN_PRIMARY} onClick={() => setPayOpen(true)}>
              Registrar pago
            </button>
            {canCheckIn && (
              <button
                type="button"
                className={BTN_PRIMARY}
                onClick={handleCheckIn}
                disabled={checkInMut.isPending}
              >
                Check-in
              </button>
            )}
            {canCheckOut && (
              <button
                type="button"
                className={BTN_PRIMARY}
                onClick={() => setCheckOutOpen(true)}
              >
                Check-out
              </button>
            )}
            {canModifyStay && (
              <button type="button" className={BTN_SECONDARY} onClick={openModify}>
                Modificar estancia
              </button>
            )}
            {canChangeRoom && (
              <button type="button" className={BTN_SECONDARY} onClick={openChangeRoom}>
                Cambiar habitación
              </button>
            )}
            {canNoShow && (
              <button
                type="button"
                className="inline-flex items-center gap-2 rounded-md border border-orange-600 bg-orange-50 px-4 py-2 text-sm font-medium text-orange-700 hover:bg-orange-100"
                onClick={() => setNoShowOpen(true)}
              >
                Marcar no-show
              </button>
            )}
            {canCancel && (
              <button
                type="button"
                className="inline-flex items-center gap-2 rounded-md border border-red-600 bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
                onClick={() => setCancelOpen(true)}
              >
                Cancelar reserva
              </button>
            )}
          </div>
        }
      />

      <div className={`${CARD} ${CARD_BODY}`}>
        <div className="mb-3 flex flex-wrap gap-2">
          <ReservationStatusBadge status={reservation.status} />
        </div>
        <div className={DETAIL_GRID}>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Huésped</span>
            <span className={DETAIL_VALUE}>{reservation.guestName}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Entrada</span>
            <span className={DETAIL_VALUE}>{formatDate(reservation.checkIn)}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Salida</span>
            <span className={DETAIL_VALUE}>{formatDate(reservation.checkOut)}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Noches</span>
            <span className={DETAIL_VALUE}>{reservation.nights}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Adultos / Niños</span>
            <span className={DETAIL_VALUE}>{reservation.adults} / {reservation.children}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Precio/noche</span>
            <span className={DETAIL_VALUE}><Amount value={reservation.nightlyPrice} /></span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Total</span>
            <span className={DETAIL_VALUE}><Amount value={reservation.totalAmount} /></span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Pagado</span>
            <span className={DETAIL_VALUE}><Amount value={reservation.paidAmount} /></span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Saldo</span>
            <span className={DETAIL_VALUE}><Amount value={reservation.balance} /></span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Check-in en</span>
            <span className={DETAIL_VALUE}>{formatDateTime(reservation.checkInAt)}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Check-out en</span>
            <span className={DETAIL_VALUE}>{formatDateTime(reservation.checkOutAt)}</span>
          </div>
          <div className="flex flex-col gap-0.5">
            <span className={DETAIL_LABEL}>Creada</span>
            <span className={DETAIL_VALUE}>{formatDateTime(reservation.createdAt)}</span>
          </div>
        </div>

        {reservation.notes && (
          <p className="mt-3 text-sm text-slate-500">
            <strong>Notas:</strong> {reservation.notes}
          </p>
        )}
        {reservation.specialRequests && (
          <p className="mt-1 text-sm text-slate-500">
            <strong>Peticiones especiales:</strong> {reservation.specialRequests}
          </p>
        )}
      </div>

      <h3 className="my-2.5 text-base font-semibold text-slate-900">Habitaciones asignadas</h3>
      <div className={DATA_TABLE}>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                <th scope="col" className={TABLE_TH}>Habitación</th>
                <th scope="col" className={TABLE_TH}>Entrada</th>
                <th scope="col" className={TABLE_TH}>Salida</th>
              </tr>
            </thead>
            <tbody>
              {reservation.rooms.length === 0 ? (
                <tr>
                  <td className={TABLE_EMPTY_TD} colSpan={3}>Sin habitaciones asignadas.</td>
                </tr>
              ) : (
                reservation.rooms.map((rr) => (
                  <tr key={rr.roomId}>
                    <td className={TABLE_TD}>{rr.roomNumber}</td>
                    <td className={TABLE_TD}>{formatDate(rr.checkIn)}</td>
                    <td className={TABLE_TD}>{formatDate(rr.checkOut)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div role="tablist" aria-label="Secciones de la reserva" className="mt-2 flex flex-wrap gap-1 border-b border-slate-200">
        {([
          ['payments', 'Pagos'],
          ['nightly', 'Tarifas por noche'],
          ['adjustments', 'Ajustes'],
        ] as const).map(([key, label]) => (
          <button
            key={key}
            role="tab"
            type="button"
            aria-selected={activeTab === key}
            onClick={() => setActiveTab(key)}
            className={
              activeTab === key
                ? 'border-b-2 border-blue-600 px-3 py-2 text-sm font-semibold text-blue-600'
                : 'border-b-2 border-transparent px-3 py-2 text-sm font-medium text-slate-500 hover:text-slate-800'
            }
          >
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'payments' && (
        <>
          {paymentsQ.isLoading && <LoadingState />}
          {paymentsQ.isError && <ErrorState error={paymentsQ.error} onRetry={() => paymentsQ.refetch()} />}
          {paymentsQ.data && (
            <div className={DATA_TABLE}>
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr>
                      <th scope="col" className={TABLE_TH}>ID</th>
                      <th scope="col" className={`${TABLE_TH} text-right`}>Importe</th>
                      <th scope="col" className={TABLE_TH}>Método</th>
                      <th scope="col" className={TABLE_TH}>Estado</th>
                      <th scope="col" className={TABLE_TH}>Referencia</th>
                      <th scope="col" className={TABLE_TH}>Fecha</th>
                      <th scope="col" className={TABLE_TH}>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paymentsQ.data.length === 0 ? (
                      <tr>
                        <td className={TABLE_EMPTY_TD} colSpan={7}>Sin pagos registrados.</td>
                      </tr>
                    ) : (
                      paymentsQ.data.map((p) => (
                        <tr key={p.id}>
                          <td className={TABLE_TD}>#{p.id}</td>
                          <td className={`${TABLE_TD} text-right`}><Amount value={p.amount} /></td>
                          <td className={TABLE_TD}>{PAYMENT_METHOD_LABELS[p.method]}</td>
                          <td className={TABLE_TD}><PaymentStatusBadge status={p.status} /></td>
                          <td className={TABLE_TD}>{p.reference ?? '—'}</td>
                          <td className={TABLE_TD}>{formatDateTime(p.paidAt)}</td>
                          <td className={TABLE_TD}>
                            <div className="flex flex-wrap gap-2">
                              {p.status === 'COMPLETED' && (
                                <>
                                  <button
                                    type="button"
                                    className={`${BTN_SECONDARY} ${BTN_SM}`}
                                    onClick={() => handlePaymentStatus(p.id, 'REFUNDED')}
                                  >
                                    Reembolsar
                                  </button>
                                  <button
                                    type="button"
                                    className={`${BTN_SECONDARY} ${BTN_SM}`}
                                    onClick={() => handlePaymentStatus(p.id, 'CANCELLED')}
                                  >
                                    Anular
                                  </button>
                                </>
                              )}
                              {p.status === 'PENDING' && (
                                <button
                                  type="button"
                                  className={`${BTN_SECONDARY} ${BTN_SM}`}
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
            </div>
          )}
        </>
      )}

      {activeTab === 'nightly' && (
        <>
          {nightlyQ.isLoading && <LoadingState />}
          {nightlyQ.isError && <ErrorState error={nightlyQ.error} onRetry={() => nightlyQ.refetch()} />}
          {nightlyQ.data && (
            <div className={DATA_TABLE}>
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr>
                      <th scope="col" className={TABLE_TH}>Noche</th>
                      <th scope="col" className={`${TABLE_TH} text-right`}>Tarifa base</th>
                      <th scope="col" className={`${TABLE_TH} text-right`}>Cargo extra</th>
                      <th scope="col" className={`${TABLE_TH} text-right`}>Descuento</th>
                      <th scope="col" className={`${TABLE_TH} text-right`}>Impuestos</th>
                      <th scope="col" className={`${TABLE_TH} text-right`}>Cargos</th>
                      <th scope="col" className={`${TABLE_TH} text-right`}>Total</th>
                      <th scope="col" className={TABLE_TH}>Incluida</th>
                    </tr>
                  </thead>
                  <tbody>
                    {nightlyQ.data.length === 0 ? (
                      <tr>
                        <td className={TABLE_EMPTY_TD} colSpan={8}>Sin tarifas por noche.</td>
                      </tr>
                    ) : (
                      nightlyQ.data.map((n) => (
                        <tr key={n.id}>
                          <td className={TABLE_TD}>{formatDate(n.nightDate)}</td>
                          <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.baseRate)}</td>
                          <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.extraPersonCharge)}</td>
                          <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.discountAmount)}</td>
                          <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.taxesAmount)}</td>
                          <td className={`${TABLE_TD} text-right`}>{formatCurrency(n.feesAmount)}</td>
                          <td className={`${TABLE_TD} text-right font-semibold`}>{formatCurrency(n.total)}</td>
                          <td className={TABLE_TD}>{n.included ? 'Sí' : 'No'}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}

      {activeTab === 'adjustments' && (
        <>
          {adjustmentsQ.isLoading && <LoadingState />}
          {adjustmentsQ.isError && <ErrorState error={adjustmentsQ.error} onRetry={() => adjustmentsQ.refetch()} />}
          {adjustmentsQ.data && (
            <div className={DATA_TABLE}>
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr>
                      <th scope="col" className={TABLE_TH}>Tipo</th>
                      <th scope="col" className={TABLE_TH}>Valor anterior</th>
                      <th scope="col" className={TABLE_TH}>Valor nuevo</th>
                      <th scope="col" className={TABLE_TH}>Motivo</th>
                      <th scope="col" className={TABLE_TH}>Usuario</th>
                      <th scope="col" className={TABLE_TH}>Fecha</th>
                    </tr>
                  </thead>
                  <tbody>
                    {adjustmentsQ.data.length === 0 ? (
                      <tr>
                        <td className={TABLE_EMPTY_TD} colSpan={6}>Sin ajustes registrados.</td>
                      </tr>
                    ) : (
                      adjustmentsQ.data.map((a) => (
                        <tr key={a.id}>
                          <td className={TABLE_TD}>{ADJUSTMENT_TYPE_LABELS[a.adjustmentType]}</td>
                          <td className={TABLE_TD}>{a.oldValue ?? '—'}</td>
                          <td className={TABLE_TD}>{a.newValue ?? '—'}</td>
                          <td className={TABLE_TD}>{a.reason ?? '—'}</td>
                          <td className={TABLE_TD}>{a.userId ? `#${a.userId}` : '—'}</td>
                          <td className={TABLE_TD}>{formatDateTime(a.createdAt)}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
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
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setAssignOpen(false)}>
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
            <div className={FORM_ALERT_ERROR} role="alert">{paySubmitError}</div>
          )}
          <div className={`${DETAIL_GRID} mb-3`}>
            <div className="flex flex-col gap-0.5">
              <span className={DETAIL_LABEL}>Total</span>
              <span className={DETAIL_VALUE}>{formatCurrency(reservation.totalAmount)}</span>
            </div>
            <div className="flex flex-col gap-0.5">
              <span className={DETAIL_LABEL}>Saldo pendiente</span>
              <span className={DETAIL_VALUE}>{formatCurrency(reservation.balance)}</span>
            </div>
          </div>
          <Input
            label="Importe (MXN)"
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
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setPayOpen(false)}>
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

      {/* Modificar estancia (ER-13) */}
      <Modal
        open={modifyOpen}
        title="Modificar estancia"
        onClose={() => setModifyOpen(false)}
        size="md"
      >
        <form onSubmit={reviewModify} noValidate>
          <p className="mb-3 text-sm text-slate-500">
            Nuevo rango de fechas. El total se recalculará a partir del motor tarifario vigente.
          </p>
          <div className="grid gap-3.5 sm:grid-cols-2">
            <Input
              label="Nueva entrada"
              name="newCheckIn"
              type="date"
              value={modifyForm.newCheckIn}
              onChange={(e) => setModifyForm({ ...modifyForm, newCheckIn: e.target.value })}
              required
            />
            <Input
              label="Nueva salida"
              name="newCheckOut"
              type="date"
              value={modifyForm.newCheckOut}
              min={modifyForm.newCheckIn}
              onChange={(e) => setModifyForm({ ...modifyForm, newCheckOut: e.target.value })}
              required
            />
          </div>
          <TextArea
            label="Motivo"
            name="modifyReason"
            rows={2}
            value={modifyForm.reason}
            onChange={(e) => setModifyForm({ ...modifyForm, reason: e.target.value })}
          />
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setModifyOpen(false)}>
              Cancelar
            </button>
            <button type="submit" className={BTN_PRIMARY}>
              Recalcular y revisar
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!modifyPending}
        title="Confirmar modificación de estancia"
        message={
          modifyPending
            ? `Nuevo rango: ${formatDate(modifyPending.newCheckIn)} → ${formatDate(modifyPending.newCheckOut)}. El total se recalculará para las noches del nuevo rango. ¿Confirmar?`
            : ''
        }
        confirmLabel="Sí, modificar"
        loading={modifyStayMut.isPending}
        onConfirm={applyModify}
        onCancel={() => setModifyPending(null)}
      />

      {/* Cambiar habitación (ER-16) */}
      <Modal
        open={changeRoomOpen}
        title="Cambiar habitación"
        onClose={() => setChangeRoomOpen(false)}
        size="md"
      >
        <form onSubmit={reviewChangeRoom} noValidate>
          <p className="mb-3 text-sm text-slate-500">
            Seleccione una habitación disponible del mismo tipo. La habitación actual pasará a limpieza.
          </p>
          <Select
            label="Nueva habitación"
            name="newRoomId"
            value={changeRoomForm.newRoomId}
            onChange={(e) => setChangeRoomForm({ ...changeRoomForm, newRoomId: e.target.value })}
            required
          >
            <option value="">Seleccione…</option>
            {(roomsQ.data?.content ?? [])
              .filter(
                (r) =>
                  r.roomTypeId === reservation.roomTypeId &&
                  r.status === 'AVAILABLE' &&
                  !reservation.rooms.some((rr) => rr.roomId === r.id),
              )
              .map((r) => (
                <option key={r.id} value={r.id}>
                  Hab. {r.number} · piso {r.floor}
                </option>
              ))}
          </Select>
          <TextArea
            label="Motivo"
            name="changeRoomReason"
            rows={2}
            value={changeRoomForm.reason}
            onChange={(e) => setChangeRoomForm({ ...changeRoomForm, reason: e.target.value })}
          />
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setChangeRoomOpen(false)}>
              Cancelar
            </button>
            <button type="submit" className={BTN_PRIMARY}>Revisar cambio</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!changeRoomPending}
        title="Confirmar cambio de habitación"
        message={
          changeRoomPending
            ? `¿Mover la reserva a la habitación #${changeRoomPending.newRoomId}? La habitación actual pasará a limpieza.`
            : ''
        }
        confirmLabel="Sí, cambiar"
        loading={changeRoomMut.isPending}
        onConfirm={applyChangeRoom}
        onCancel={() => setChangeRoomPending(null)}
      />

      {/* No-show (ER-11) */}
      <Modal
        open={noShowOpen}
        title="Marcar como no-show"
        onClose={() => setNoShowOpen(false)}
        size="sm"
      >
        <form onSubmit={handleNoShow} noValidate>
          <p className="mb-3 text-sm text-slate-500">
            La reserva pasará a NO_SHOW y se liberará la habitación. Se aplicará la penalización de
            no-show según la política asociada.
          </p>
          <TextArea
            label="Motivo"
            name="noShowReason"
            rows={2}
            value={noShowReason}
            onChange={(e) => setNoShowReason(e.target.value)}
          />
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setNoShowOpen(false)}>
              Cancelar
            </button>
            <button
              type="submit"
              className="inline-flex items-center gap-2 rounded-md border border-orange-600 bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-50"
              disabled={noShowMut.isPending}
            >
              {noShowMut.isPending ? 'Marcando…' : 'Sí, marcar no-show'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
