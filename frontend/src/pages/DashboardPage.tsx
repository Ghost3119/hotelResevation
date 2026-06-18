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
import { ROUTES } from '../utils/constants'
import type { DashboardParams } from '../api/dashboard.api'

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
          <div className="flex-gap">
            <label className="form-field" style={{ marginBottom: 0 }}>
              <span className="form-label">Desde</span>
              <input
                className="input"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            </label>
            <label className="form-field" style={{ marginBottom: 0 }}>
              <span className="form-label">Hasta</span>
              <input
                className="input"
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            </label>
            <button type="button" className="btn btn-primary" onClick={applyRange}>
              Aplicar
            </button>
          </div>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {data && (
        <div className="stack">
          <div className="grid kpi-grid">
            <div className="kpi">
              <div className="kpi-label">Llegadas hoy</div>
              <div className="kpi-value">{data.arrivalsToday}</div>
            </div>
            <div className="kpi">
              <div className="kpi-label">Salidas hoy</div>
              <div className="kpi-value">{data.departuresToday}</div>
            </div>
            <div className="kpi">
              <div className="kpi-label">Habitaciones ocupadas</div>
              <div className="kpi-value">{data.occupiedRooms}</div>
            </div>
            <div className="kpi">
              <div className="kpi-label">Habitaciones disponibles</div>
              <div className="kpi-value">{data.availableRooms}</div>
            </div>
            <div className="kpi">
              <div className="kpi-label">En limpieza</div>
              <div className="kpi-value">{data.cleaningRooms}</div>
            </div>
            <div className="kpi">
              <div className="kpi-label">Ocupación</div>
              <div className="kpi-value">{formatPercent(data.occupancyRate)}</div>
            </div>
            <div className="kpi">
              <div className="kpi-label">Ingresos del periodo</div>
              <div className="kpi-value">
                <Amount value={data.incomePeriod} />
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h3>Reservas recientes</h3>
            </div>
            <div className="data-table" style={{ border: 'none', boxShadow: 'none' }}>
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Huésped</th>
                    <th>Entrada</th>
                    <th>Salida</th>
                    <th>Estado</th>
                    <th className="text-right">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentReservations.length === 0 ? (
                    <tr className="data-table-empty-row">
                      <td colSpan={6}>Sin reservas recientes.</td>
                    </tr>
                  ) : (
                    data.recentReservations.map((r) => (
                      <tr key={r.id}>
                        <td>
                          <Link to={ROUTES.reservationDetail(r.id)}>#{r.id}</Link>
                        </td>
                        <td>{r.guestName}</td>
                        <td>{formatDate(r.checkIn)}</td>
                        <td>{formatDate(r.checkOut)}</td>
                        <td>
                          <ReservationStatusBadge status={r.status} />
                        </td>
                        <td className="text-right">
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
