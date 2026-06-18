import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { Input, Select, TextArea } from '../components/FormField'
import { ErrorState } from '../components/ErrorState'
import { useToast } from '../components/Toast'
import { useDebounce } from '../hooks/useDebounce'
import { useGuests } from '../hooks/useGuests'
import { useRoomTypes } from '../hooks/useRoomTypes'
import { useAvailability } from '../hooks/useAvailability'
import { useCreateReservation } from '../hooks/useReservations'
import type { NormalizedError } from '../api/types'
import type { ReservationCreateDto } from '../api/reservations.api'
import { fullName, todayISO } from '../utils/format'
import { getFieldErrors } from '../utils/error'
import { ROUTES } from '../utils/constants'

export function ReservationFormPage() {
  const navigate = useNavigate()
  const toast = useToast()

  const [guestSearch, setGuestSearch] = useState('')
  const debouncedSearch = useDebounce(guestSearch, 250)
  const [guestId, setGuestId] = useState<number | null>(null)
  const [guestLabel, setGuestLabel] = useState('')
  const [pickerOpen, setPickerOpen] = useState(false)

  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [adults, setAdults] = useState(1)
  const [children, setChildren] = useState(0)
  const [roomTypeId, setRoomTypeId] = useState<number | ''>('')
  const [roomId, setRoomId] = useState<number | ''>('')

  const [notes, setNotes] = useState('')
  const [specialRequests, setSpecialRequests] = useState('')

  const [clientErrors, setClientErrors] = useState<Record<string, string>>({})
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const guestsQ = useGuests({ page: 0, size: 10, search: debouncedSearch || undefined })
  const roomTypesQ = useRoomTypes({})
  const createMut = useCreateReservation()

  const availabilityParams = useMemo(() => {
    if (!checkIn || !checkOut || !roomTypeId) return null
    return { checkIn, checkOut, guests: adults + children, roomTypeId: Number(roomTypeId) }
  }, [checkIn, checkOut, roomTypeId, adults, children])

  const availabilityQ = useAvailability(availabilityParams)

  const roomTypes = roomTypesQ.data ?? []

  const validate = (): boolean => {
    const errs: Record<string, string> = {}
    if (!guestId) errs.guestId = 'Seleccione un huésped.'
    if (!checkIn) errs.checkIn = 'Indique la fecha de entrada.'
    if (!checkOut) errs.checkOut = 'Indique la fecha de salida.'
    if (checkIn && checkOut && checkOut <= checkIn) {
      errs.checkOut = 'La salida debe ser posterior a la entrada.'
    }
    if (checkIn && checkIn < todayISO()) errs.checkIn = 'La entrada no puede ser pasada.'
    if (adults < 1) errs.adults = 'Debe haber al menos un adulto.'
    if (children < 0) errs.children = 'Valor no válido.'
    if (!roomTypeId) errs.roomTypeId = 'Seleccione un tipo de habitación.'
    const totalGuests = adults + children
    const rt = roomTypes.find((t) => t.id === Number(roomTypeId))
    if (rt && totalGuests > rt.maxCapacity) {
      errs.adults = `Capacidad máxima: ${rt.maxCapacity} huéspedes.`
    }
    setClientErrors(errs)
    return Object.keys(errs).length === 0
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    if (!validate()) return
    const payload: ReservationCreateDto = {
      guestId: guestId as number,
      checkIn,
      checkOut,
      adults,
      children,
      roomTypeId: Number(roomTypeId),
      roomId: roomId ? Number(roomId) : null,
      notes: notes.trim() || null,
      specialRequests: specialRequests.trim() || null,
    }
    try {
      const created = await createMut.mutateAsync(payload)
      toast.success('Reserva creada.')
      navigate(ROUTES.reservationDetail(created.id), { replace: true })
    } catch (err) {
      const normalized = err as NormalizedError
      setFieldErrors(getFieldErrors(err))
      setSubmitError(normalized.message)
    }
  }

  const mergedErrors = { ...fieldErrors, ...clientErrors }

  return (
    <div>
      <PageHeader
        title="Nueva reserva"
        actions={
          <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>
            Volver
          </button>
        }
      />

      <form className="card card-body" onSubmit={onSubmit} noValidate style={{ maxWidth: 820 }}>
        {submitError && (
          <div className="form-alert form-alert-error" role="alert">{submitError}</div>
        )}

        <div className="form-field">
          <label className="form-label" htmlFor="guestSearch">Huésped</label>
          <input
            id="guestSearch"
            className="input"
            value={guestSearch}
            placeholder={guestLabel || 'Buscar por nombre, correo o documento…'}
            onChange={(e) => {
              setGuestSearch(e.target.value)
              setPickerOpen(true)
              setGuestId(null)
              setGuestLabel('')
            }}
            onFocus={() => setPickerOpen(true)}
            onBlur={() => setTimeout(() => setPickerOpen(false), 150)}
            autoComplete="off"
          />
          {guestId && (
            <span className="form-hint">
              Seleccionado: <strong>{guestLabel}</strong>
            </span>
          )}
          {mergedErrors.guestId && <span className="form-error">{mergedErrors.guestId}</span>}
          {pickerOpen && guestSearch && (
            <div className="card" style={{ marginTop: 6, maxHeight: 220, overflowY: 'auto' }}>
              {guestsQ.isLoading && <div className="state state-empty">Buscando…</div>}
              {guestsQ.data && guestsQ.data.content.length === 0 && (
                <div className="state state-empty">Sin coincidencias.</div>
              )}
              {guestsQ.data?.content.map((g) => (
                <button
                  key={g.id}
                  type="button"
                  className="btn btn-ghost"
                  style={{ width: '100%', justifyContent: 'flex-start', textAlign: 'left' }}
                  onClick={() => {
                    setGuestId(g.id)
                    setGuestLabel(fullName(g.firstName, g.lastName))
                    setGuestSearch('')
                    setPickerOpen(false)
                  }}
                >
                  <strong>{fullName(g.firstName, g.lastName)}</strong>{' '}
                  <span className="muted">· {g.documentNumber} · {g.email ?? 'sin correo'}</span>
                </button>
              ))}
            </div>
          )}
          {!guestId && !pickerOpen && (
            <LinkLikeBtn onClick={() => navigate(ROUTES.GUESTS)} label="¿No existe? Crear huésped" />
          )}
        </div>

        <div className="form-row">
          <Input
            label="Entrada"
            name="checkIn"
            type="date"
            value={checkIn}
            onChange={(e) => setCheckIn(e.target.value)}
            error={mergedErrors.checkIn}
            min={todayISO()}
            required
          />
          <Input
            label="Salida"
            name="checkOut"
            type="date"
            value={checkOut}
            onChange={(e) => setCheckOut(e.target.value)}
            error={mergedErrors.checkOut}
            min={checkIn || todayISO()}
            required
          />
        </div>

        <div className="form-row">
          <Input
            label="Adultos"
            name="adults"
            type="number"
            min={1}
            value={adults}
            onChange={(e) => setAdults(Number(e.target.value))}
            error={mergedErrors.adults}
            required
          />
          <Input
            label="Niños"
            name="children"
            type="number"
            min={0}
            value={children}
            onChange={(e) => setChildren(Number(e.target.value))}
            error={mergedErrors.children}
          />
          <Select
            label="Tipo de habitación"
            name="roomTypeId"
            value={roomTypeId}
            onChange={(e) => {
              setRoomTypeId(e.target.value ? Number(e.target.value) : '')
              setRoomId('')
            }}
            error={mergedErrors.roomTypeId}
            required
          >
            <option value="">Seleccione…</option>
            {roomTypes
              .filter((rt) => rt.active)
              .map((rt) => (
                <option key={rt.id} value={rt.id}>
                  {rt.name} (cap. {rt.maxCapacity})
                </option>
              ))}
          </Select>
        </div>

        {availabilityParams && (
          <Select
            label="Habitación (opcional, dejar vacío para asignación automática en check-in)"
            name="roomId"
            value={roomId}
            onChange={(e) => setRoomId(e.target.value ? Number(e.target.value) : '')}
            wrapperClassName="form-row"
          >
            <option value="">Sin asignar</option>
            {availabilityQ.isLoading && <option disabled>Cargando…</option>}
            {availabilityQ.data?.map((room) => (
              <option key={room.roomId} value={room.roomId}>
                Hab. {room.number} · piso {room.floor}
              </option>
            ))}
          </Select>
        )}

        <TextArea
          label="Notas"
          name="notes"
          rows={2}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          error={mergedErrors.notes}
        />
        <TextArea
          label="Peticiones especiales"
          name="specialRequests"
          rows={2}
          value={specialRequests}
          onChange={(e) => setSpecialRequests(e.target.value)}
          error={mergedErrors.specialRequests}
        />

        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={createMut.isPending}>
            Crear reserva
          </button>
        </div>
      </form>

      {roomTypesQ.isError && <ErrorState error={roomTypesQ.error} />}
    </div>
  )
}

function LinkLikeBtn({ onClick, label }: { onClick: () => void; label: string }) {
  return (
    <button type="button" onClick={onClick} style={{ background: 'none', border: 'none', color: 'var(--color-primary)', padding: 0, fontSize: '0.82rem' }}>
      {label}
    </button>
  )
}
