import { useMemo, useState } from 'react'
import {
  addDays,
  addWeeks,
  eachDayOfInterval,
  format,
  isWithinInterval,
  parseISO,
  startOfWeek,
} from 'date-fns'
import { es } from 'date-fns/locale'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { useRooms } from '../hooks/useRooms'
import { useReservations } from '../hooks/useReservations'
import { useRoomBlocks } from '../hooks/useRoomBlocks'
import type { ReservationDto, RoomDto } from '../api/types'
import type { RoomBlockDto } from '../api/generated/schema'
import {
  BTN_SECONDARY,
  CARD,
  CARD_BODY,
  ROOM_STATUS_LABELS,
} from '../utils/constants'

type CellState = 'AVAILABLE' | 'RESERVED' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE' | 'BLOCKED'

const CELL_COLORS: Record<CellState, string> = {
  AVAILABLE: 'bg-green-100 text-green-800',
  RESERVED: 'bg-blue-100 text-blue-800',
  OCCUPIED: 'bg-red-100 text-red-800',
  CLEANING: 'bg-yellow-100 text-yellow-800',
  MAINTENANCE: 'bg-orange-100 text-orange-800',
  BLOCKED: 'bg-slate-300 text-slate-800',
}

const CELL_LABELS: Record<CellState, string> = {
  ...ROOM_STATUS_LABELS,
  RESERVED: 'Reservada',
  OCCUPIED: 'Ocupada',
  AVAILABLE: 'Disponible',
  BLOCKED: 'Bloqueada',
} as Record<CellState, string>

function overlaps(checkIn: string, checkOut: string, day: Date): boolean {
  try {
    const start = parseISO(checkIn)
    // checkout date is exclusive: nights are [checkIn, checkOut)
    const end = parseISO(checkOut)
    return isWithinInterval(day, { start, end: addDays(end, -1) })
  } catch {
    return false
  }
}

export function OccupancyCalendarPage() {
  const [anchor, setAnchor] = useState<Date>(() => startOfWeek(new Date(), { weekStartsOn: 1 }))
  const roomsQ = useRooms({ size: 200 })
  const reservationsQ = useReservations({ size: 500 })
  const blocksQ = useRoomBlocks({ size: 200 })

  const days = useMemo(
    () => eachDayOfInterval({ start: anchor, end: addDays(anchor, 6) }),
    [anchor],
  )

  const rooms = roomsQ.data?.content ?? []
  const reservations = reservationsQ.data?.content ?? []
  const blocks = blocksQ.data?.content ?? []

  const cellState = (room: RoomDto, day: Date): CellState => {
    const dayIso = format(day, 'yyyy-MM-dd')
    // Blocks take precedence (maintenance/operational)
    const blocked = blocks.some(
      (b: RoomBlockDto) =>
        b.roomId === room.id &&
        !b.releasedAt &&
        overlaps(b.startDate, b.endDate, day),
    )
    if (blocked) return 'BLOCKED'

    const roomRes = reservations.find(
      (r: ReservationDto) =>
        (r.status === 'CONFIRMED' || r.status === 'CHECKED_IN' || r.status === 'PENDING') &&
        r.rooms.some((rr) => rr.roomId === room.id && overlaps(rr.checkIn, rr.checkOut, day)),
    )
    if (roomRes) {
      return roomRes.status === 'CHECKED_IN' ? 'OCCUPIED' : 'RESERVED'
    }
    // Use the room's static status as a fallback hint for today only
    if (dayIso === format(new Date(), 'yyyy-MM-dd')) {
      if (room.status === 'CLEANING') return 'CLEANING'
      if (room.status === 'MAINTENANCE') return 'MAINTENANCE'
    }
    return 'AVAILABLE'
  }

  if (roomsQ.isLoading || reservationsQ.isLoading) return <LoadingState label="Cargando calendario…" />
  if (roomsQ.isError) return <ErrorState error={roomsQ.error} onRetry={() => roomsQ.refetch()} />
  if (reservationsQ.isError)
    return <ErrorState error={reservationsQ.error} onRetry={() => reservationsQ.refetch()} />

  const weekLabel = `${format(anchor, 'dd MMM', { locale: es })} – ${format(addDays(anchor, 6), 'dd MMM yyyy', { locale: es })}`

  return (
    <div>
      <PageHeader
        title="Calendario de ocupación"
        subtitle="Ocupación por habitación y fecha (vista semanal)"
        actions={
          <div className="flex items-center gap-2">
            <button
              type="button"
              className={BTN_SECONDARY}
              aria-label="Semana anterior"
              onClick={() => setAnchor((d) => addWeeks(d, -1))}
            >
              ← Anterior
            </button>
            <button
              type="button"
              className={BTN_SECONDARY}
              aria-label="Semana actual"
              onClick={() => setAnchor(startOfWeek(new Date(), { weekStartsOn: 1 }))}
            >
              Hoy
            </button>
            <button
              type="button"
              className={BTN_SECONDARY}
              aria-label="Semana siguiente"
              onClick={() => setAnchor((d) => addWeeks(d, 1))}
            >
              Siguiente →
            </button>
          </div>
        }
      />

      <p className="mb-3 text-sm text-slate-600" aria-live="polite">
        Semana: <strong>{weekLabel}</strong>
      </p>

      {rooms.length === 0 ? (
        <EmptyState title="Sin habitaciones" message="No hay habitaciones para mostrar." />
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white shadow-sm">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                <th
                  scope="col"
                  className="sticky left-0 z-10 border-b border-slate-200 bg-slate-50 px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-slate-500"
                >
                  Habitación
                </th>
                {days.map((day) => (
                  <th
                    key={day.toISOString()}
                    scope="col"
                    className="border-b border-slate-200 bg-slate-50 px-2 py-2.5 text-center text-xs font-semibold uppercase tracking-wide text-slate-500 whitespace-nowrap"
                  >
                    {format(day, 'EE', { locale: es })}<br />
                    {format(day, 'dd/MM')}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rooms.map((room) => (
                <tr key={room.id} className="hover:bg-slate-50">
                  <th
                    scope="row"
                    className="sticky left-0 z-10 border-b border-slate-200 bg-white px-3 py-2 text-left text-sm font-medium text-slate-900 whitespace-nowrap"
                  >
                    Hab. {room.number}
                    <span className="ml-1 text-xs font-normal text-slate-400">{room.roomTypeName}</span>
                  </th>
                  {days.map((day) => {
                    const state = cellState(room, day)
                    return (
                      <td key={day.toISOString()} className="border-b border-slate-200 p-1 text-center">
                        <div
                          className={`mx-auto h-9 min-w-[2.5rem] rounded ${CELL_COLORS[state]} flex items-center justify-center text-[10px] font-medium`}
                          title={`${room.number} · ${format(day, 'dd/MM/yyyy')} · ${CELL_LABELS[state]}`}
                          aria-label={`${room.number} ${format(day, 'dd/MM')} ${CELL_LABELS[state]}`}
                        >
                          {CELL_LABELS[state].slice(0, 4)}
                        </div>
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <section className={`mt-5 ${CARD} ${CARD_BODY}`}>
        <h2 className="mb-2 text-base font-semibold text-slate-900">Leyenda</h2>
        <div className="flex flex-wrap gap-3">
          {(Object.keys(CELL_COLORS) as CellState[]).map((s) => (
            <span
              key={s}
              className={`inline-flex items-center rounded px-2.5 py-1 text-xs font-medium ${CELL_COLORS[s]}`}
            >
              {CELL_LABELS[s]}
            </span>
          ))}
        </div>
        <p className="mt-2 text-xs text-slate-500">
          Los estados de limpieza/mantenimiento solo se muestran para el día actual a partir del
          estado de la habitación; el resto se deriva de reservas y bloqueos.
        </p>
      </section>
    </div>
  )
}
