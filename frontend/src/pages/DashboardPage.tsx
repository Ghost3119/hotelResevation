import { useState } from 'react'
import { Link } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { Amount } from '../components/Amount'
import { ReservationStatusBadge } from '../components/StatusBadge'
import { useDashboard } from '../hooks/useDashboard'
import { useAuth } from '../auth/AuthContext'
import { formatDate, formatPercent, todayISO } from '../utils/format'
import {
  ROUTES,
  BTN_PRIMARY,
  CARD,
  CARD_HEADER,
  FORM_LABEL,
  INPUT,
  TABLE_TH,
  TABLE_TD,
  TABLE_EMPTY_TD,
} from '../utils/constants'
import type { DashboardParams } from '../api/dashboard.api'

const KPI = 'rounded-lg border border-slate-200 bg-white p-4 shadow-sm'
const KPI_LABEL = 'text-xs uppercase tracking-wide text-slate-500'
const KPI_VALUE = 'mt-1 text-2xl font-bold text-slate-900'

export function DashboardPage() {
  const { user } = useAuth()
  const today = todayISO()
  const [from, setFrom] = useState(today)
  const [to, setTo] = useState(today)
  const [params, setParams] = useState<DashboardParams>({ from: today, to: today })

  const { data, isLoading, isError, error, refetch } = useDashboard(params)

  const applyRange = () => {
    const next: DashboardParams = { from, to }
    setParams(next)
  }

  return (
    <div>
      <PageHeader
        title="Panel"
        subtitle={`Hola, ${user?.fullName ?? ''}`}
        actions={
          <div className="flex flex-wrap items-end gap-2">
            <label className="flex flex-col gap-1">
              <span className={FORM_LABEL}>Desde</span>
              <input
                className={INPUT}
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className={FORM_LABEL}>Hasta</span>
              <input
                className={INPUT}
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            </label>
            <button type="button" className={BTN_PRIMARY} onClick={applyRange}>
              Aplicar
            </button>
          </div>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {data && (
        <div className="flex flex-col gap-3">
          <div className="grid gap-4 [grid-template-columns:repeat(auto-fit,minmax(180px,1fr))]">
            <div className={KPI}>
              <div className={KPI_LABEL}>Llegadas hoy</div>
              <div className={KPI_VALUE}>{data.arrivalsToday}</div>
            </div>
            <div className={KPI}>
              <div className={KPI_LABEL}>Salidas hoy</div>
              <div className={KPI_VALUE}>{data.departuresToday}</div>
            </div>
            <div className={KPI}>
              <div className={KPI_LABEL}>Habitaciones ocupadas</div>
              <div className={KPI_VALUE}>{data.occupiedRooms}</div>
            </div>
            <div className={KPI}>
              <div className={KPI_LABEL}>Habitaciones disponibles</div>
              <div className={KPI_VALUE}>{data.availableRooms}</div>
            </div>
            <div className={KPI}>
              <div className={KPI_LABEL}>En limpieza</div>
              <div className={KPI_VALUE}>{data.cleaningRooms}</div>
            </div>
            <div className={KPI}>
              <div className={KPI_LABEL}>Ocupación</div>
              <div className={KPI_VALUE}>{formatPercent(data.occupancyRate)}</div>
            </div>
            <div className={KPI}>
              <div className={KPI_LABEL}>Ingresos del periodo</div>
              <div className={KPI_VALUE}>
                <Amount value={data.incomePeriod} />
              </div>
            </div>
          </div>

          <div className={CARD}>
            <div className={CARD_HEADER}>
              <h3 className="text-base font-semibold text-slate-900">Reservas recientes</h3>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse">
                <thead>
                  <tr>
                    <th className={TABLE_TH}>ID</th>
                    <th className={TABLE_TH}>Huésped</th>
                    <th className={TABLE_TH}>Entrada</th>
                    <th className={TABLE_TH}>Salida</th>
                    <th className={TABLE_TH}>Estado</th>
                    <th className={`${TABLE_TH} text-right`}>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentReservations.length === 0 ? (
                    <tr>
                      <td className={TABLE_EMPTY_TD} colSpan={6}>Sin reservas recientes.</td>
                    </tr>
                  ) : (
                    data.recentReservations.map((r) => (
                      <tr key={r.id}>
                        <td className={TABLE_TD}>
                          <Link to={ROUTES.reservationDetail(r.id)} className="text-blue-600 hover:underline">#{r.id}</Link>
                        </td>
                        <td className={TABLE_TD}>{r.guestName}</td>
                        <td className={TABLE_TD}>{formatDate(r.checkIn)}</td>
                        <td className={TABLE_TD}>{formatDate(r.checkOut)}</td>
                        <td className={TABLE_TD}>
                          <ReservationStatusBadge status={r.status} />
                        </td>
                        <td className={`${TABLE_TD} text-right`}>
                          <Amount value={r.totalAmount} />
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
