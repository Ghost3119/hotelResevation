import { Link, useParams } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Amount } from '../components/Amount'
import { ReservationStatusBadge } from '../components/StatusBadge'
import { useGuest, useGuestReservations } from '../hooks/useGuests'
import { formatDate, fullName } from '../utils/format'
import { ROUTES } from '../utils/constants'

export function GuestReservationsPage() {
  const { id } = useParams<{ id: string }>()
  const guestId = Number(id)

  const guestQ = useGuest(guestId)
  const reservationsQ = useGuestReservations(guestId)

  const guest = guestQ.data

  return (
    <div>
      <PageHeader
        title={guest ? `Reservas de ${fullName(guest.firstName, guest.lastName)}` : 'Reservas del huésped'}
        subtitle={guest ? `${guest.documentNumber} · ${guest.nationality}` : undefined}
        actions={
          <Link to={ROUTES.GUESTS} className="btn btn-secondary">
            Volver a huéspedes
          </Link>
        }
      />

      {guestQ.isLoading && <LoadingState />}
      {guestQ.isError && <ErrorState error={guestQ.error} onRetry={() => guestQ.refetch()} />}

      {guest && (
        <>
          {reservationsQ.isLoading && <LoadingState />}
          {reservationsQ.isError && (
            <ErrorState error={reservationsQ.error} onRetry={() => reservationsQ.refetch()} />
          )}
          {reservationsQ.data && reservationsQ.data.length === 0 && (
            <EmptyState title="Sin reservas" message="Este huésped no tiene reservas." />
          )}
          {reservationsQ.data && reservationsQ.data.length > 0 && (
            <div className="data-table">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Entrada</th>
                    <th>Salida</th>
                    <th>Noches</th>
                    <th>Estado</th>
                    <th className="text-right">Total</th>
                    <th className="text-right">Saldo</th>
                  </tr>
                </thead>
                <tbody>
                  {reservationsQ.data.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <Link to={ROUTES.reservationDetail(r.id)}>#{r.id}</Link>
                      </td>
                      <td>{formatDate(r.checkIn)}</td>
                      <td>{formatDate(r.checkOut)}</td>
                      <td>{r.nights}</td>
                      <td>
                        <ReservationStatusBadge status={r.status} />
                      </td>
                      <td className="text-right">
                        <Amount value={r.totalAmount} />
                      </td>
                      <td className="text-right">
                        <Amount value={r.balance} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  )
}
