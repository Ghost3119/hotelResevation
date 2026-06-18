import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Input, Select } from '../components/FormField'
import { Amount } from '../components/Amount'
import { useAvailability } from '../hooks/useAvailability'
import { useRoomTypes } from '../hooks/useRoomTypes'
import type { AvailabilityParams } from '../api/availability.api'
import { todayISO } from '../utils/format'
import {
  BTN_PRIMARY,
  CARD,
  CARD_BODY,
  FORM_ALERT_ERROR,
} from '../utils/constants'

export function AvailabilityPage() {
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [guests, setGuests] = useState(1)
  const [roomTypeId, setRoomTypeId] = useState<number | ''>('')
  const [params, setParams] = useState<AvailabilityParams | null>(null)
  const [error, setError] = useState<string | null>(null)

  const roomTypesQ = useRoomTypes({})
  const availabilityQ = useAvailability(params)

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!checkIn || !checkOut) {
      setError('Indique las fechas de entrada y salida.')
      return
    }
    if (checkOut <= checkIn) {
      setError('La fecha de salida debe ser posterior a la de entrada.')
      return
    }
    if (checkIn < todayISO()) {
      setError('La fecha de entrada no puede ser pasada.')
      return
    }
    setParams({ checkIn, checkOut, guests, roomTypeId: roomTypeId ? Number(roomTypeId) : undefined })
  }

  return (
    <div>
      <PageHeader title="Disponibilidad" subtitle="Buscar habitaciones libres por fecha y capacidad" />

      <form className={`${CARD} ${CARD_BODY} mb-4 flex flex-wrap items-end gap-2.5`} onSubmit={onSubmit} noValidate>
        <Input
          label="Entrada"
          name="checkIn"
          type="date"
          value={checkIn}
          onChange={(e) => setCheckIn(e.target.value)}
          min={todayISO()}
          required
        />
        <Input
          label="Salida"
          name="checkOut"
          type="date"
          value={checkOut}
          onChange={(e) => setCheckOut(e.target.value)}
          min={checkIn || todayISO()}
          required
        />
        <Input
          label="Huéspedes"
          name="guests"
          type="number"
          min={1}
          value={guests}
          onChange={(e) => setGuests(Number(e.target.value))}
          required
        />
        <Select
          label="Tipo de habitación"
          name="roomTypeId"
          value={roomTypeId}
          onChange={(e) => setRoomTypeId(e.target.value ? Number(e.target.value) : '')}
        >
          <option value="">Todos</option>
          {roomTypesQ.data
            ?.filter((rt) => rt.active)
            .map((rt) => (
              <option key={rt.id} value={rt.id}>
                {rt.name}
              </option>
            ))}
        </Select>
        <button type="submit" className={BTN_PRIMARY}>
          Buscar
        </button>
      </form>

      {error && <div className={FORM_ALERT_ERROR} role="alert">{error}</div>}

      {!params && !error && (
        <EmptyState title="Indique criterios de búsqueda" message="Seleccione fechas y número de huéspedes para ver la disponibilidad." />
      )}

      {availabilityQ.isLoading && <LoadingState />}
      {availabilityQ.isError && <ErrorState error={availabilityQ.error} onRetry={() => availabilityQ.refetch()} />}

      {params && availabilityQ.data && availabilityQ.data.length === 0 && (
        <EmptyState title="Sin disponibilidad" message="No hay habitaciones libres que cumplan los criterios." />
      )}

      {params && availabilityQ.data && availabilityQ.data.length > 0 && (
        <>
          <h3 className="my-2.5 text-base font-semibold text-slate-900">
            {availabilityQ.data.length} habitación{availabilityQ.data.length === 1 ? '' : 'es'} disponible{availabilityQ.data.length === 1 ? '' : 's'}
          </h3>
          <div className="grid gap-3.5 [grid-template-columns:repeat(auto-fill,minmax(220px,1fr))]">
            {availabilityQ.data.map((room) => (
              <div key={room.roomId} className="flex flex-col gap-1.5 rounded-lg border border-slate-200 bg-white p-3.5 shadow-sm">
                <span className="text-xl font-bold text-slate-900">Hab. {room.number}</span>
                <span className="text-sm text-slate-500">Piso {room.floor} · {room.roomTypeName}</span>
                <span className="text-sm text-slate-500">Capacidad: {room.maxCapacity} huéspedes</span>
                <span className="font-semibold text-slate-900">
                  <Amount value={room.basePrice} /> / noche
                </span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
