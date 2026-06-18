import { Link, useParams } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Amount } from '../components/Amount'
import { ReservationStatusBadge } from '../components/StatusBadge'
import { useGuest, useGuestReservations } from '../hooks/useGuests'
import { formatDate, fullName } from '../utils/format'
import {
  ROUTES,
  BTN_SECONDARY,
  DATA_TABLE,
  TABLE_TH,
  TABLE_TD,
} from '../utils/constants'

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
          <Link to={ROUTES.GUESTS} className={BTN_SECONDARY}>
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
            <div className={DATA_TABLE}>
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr>
                      <th className={TABLE_TH}>ID</th>
                      <th className={TABLE_TH}>Entrada</th>
                      <th className={TABLE_TH}>Salida</th>
                      <th className={TABLE_TH}>Noches</th>
                      <th className={TABLE_TH}>Estado</th>
                      <th className={`${TABLE_TH} text-right`}>Total</th>
                      <th className={`${TABLE_TH} text-right`}>Saldo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reservationsQ.data.map((r) => (
                      <tr key={r.id}>
                        <td className={TABLE_TD}>
                          <Link to={ROUTES.reservationDetail(r.id)} className="text-blue-600 hover:underline">#{r.id}</Link>
                        </td>
                        <td className={TABLE_TD}>{formatDate(r.checkIn)}</td>
                        <td className={TABLE_TD}>{formatDate(r.checkOut)}</td>
                        <td className={TABLE_TD}>{r.nights}</td>
                        <td className={TABLE_TD}>
                          <ReservationStatusBadge status={r.status} />
                        </td>
                        <td className={`${TABLE_TD} text-right`}>
                          <Amount value={r.totalAmount} />
                        </td>
                        <td className={`${TABLE_TD} text-right`}>
                          <Amount value={r.balance} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
