import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Input, Select } from '../components/FormField'
import { Amount } from '../components/Amount'
import { ReservationStatusBadge } from '../components/StatusBadge'
import { useReservations } from '../hooks/useReservations'
import type { ReservationDto, ReservationStatus } from '../api/types'
import { formatDate } from '../utils/format'
import { PAGE_SIZE_DEFAULT, RESERVATION_STATUSES, RESERVATION_STATUS_LABELS, ROUTES } from '../utils/constants'

export function ReservationsPage() {
  const navigate = useNavigate()
  const [status, setStatus] = useState<'' | ReservationStatus>('')
  const [checkIn, setCheckIn] = useState('')
  const [page, setPage] = useState(0)
  const size = PAGE_SIZE_DEFAULT

  const params = {
    page,
    size,
    status: status || undefined,
    checkIn: checkIn || undefined,
  }
  const { data, isLoading, isError, error, refetch, isFetching } = useReservations(params)

  const columns: Column<ReservationDto>[] = [
    { key: 'id', header: 'ID', render: (r) => <Link to={ROUTES.reservationDetail(r.id)}>#{r.id}</Link> },
    { key: 'guestName', header: 'Huésped', render: (r) => r.guestName },
    { key: 'checkIn', header: 'Entrada', render: (r) => formatDate(r.checkIn) },
    { key: 'checkOut', header: 'Salida', render: (r) => formatDate(r.checkOut) },
    { key: 'nights', header: 'Noches', render: (r) => r.nights },
    { key: 'roomTypeName', header: 'Tipo', render: (r) => r.roomTypeName },
    { key: 'status', header: 'Estado', render: (r) => <ReservationStatusBadge status={r.status} /> },
    { key: 'totalAmount', header: 'Total', className: 'text-right', render: (r) => <Amount value={r.totalAmount} /> },
    { key: 'balance', header: 'Saldo', className: 'text-right', render: (r) => <Amount value={r.balance} /> },
  ]

  return (
    <div>
      <PageHeader
        title="Reservas"
        subtitle="Listado y gestión de reservas"
        actions={
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => navigate(ROUTES.RESERVATION_NEW)}
          >
            Nueva reserva
          </button>
        }
      />

      <div className="toolbar">
        <Select
          label="Estado"
          name="status"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as ReservationStatus | '')
            setPage(0)
          }}
        >
          <option value="">Todos</option>
          {RESERVATION_STATUSES.map((s) => (
            <option key={s} value={s}>
              {RESERVATION_STATUS_LABELS[s]}
            </option>
          ))}
        </Select>
        <Input
          label="Fecha entrada"
          name="checkIn"
          type="date"
          value={checkIn}
          onChange={(e) => {
            setCheckIn(e.target.value)
            setPage(0)
          }}
        />
        {(status || checkIn) && (
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              setStatus('')
              setCheckIn('')
              setPage(0)
            }}
          >
            Limpiar
          </button>
        )}
      </div>

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin reservas" message="No hay reservas con los filtros indicados." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(r) => r.id}
            page={data.page}
            size={data.size}
            totalElements={data.totalElements}
            totalPages={data.totalPages}
            onPageChange={setPage}
            empty={isFetching ? 'Actualizando…' : undefined}
          />
        )
      )}
    </div>
  )
}
