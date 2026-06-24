import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { RequireAuth } from './auth/RequireAuth'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { GuestsPage } from './pages/GuestsPage'
import { GuestReservationsPage } from './pages/GuestReservationsPage'
import { ReservationsPage } from './pages/ReservationsPage'
import { ReservationDetailPage } from './pages/ReservationDetailPage'
import { ReservationFormPage } from './pages/ReservationFormPage'
import { AvailabilityPage } from './pages/AvailabilityPage'
import { RoomsPage } from './pages/RoomsPage'
import { RoomTypesPage } from './pages/RoomTypesPage'
import { UsersPage } from './pages/UsersPage'
import { PaymentsPage } from './pages/PaymentsPage'
import { QuotePage } from './pages/QuotePage'
import { RatePlansPage } from './pages/RatePlansPage'
import { SeasonalRatesPage } from './pages/SeasonalRatesPage'
import { TaxesAndFeesPage } from './pages/TaxesAndFeesPage'
import { CancellationPoliciesPage } from './pages/CancellationPoliciesPage'
import { ReservationGroupsPage } from './pages/ReservationGroupsPage'
import { RoomBlocksPage } from './pages/RoomBlocksPage'
import { HousekeepingPage } from './pages/HousekeepingPage'
import { OccupancyCalendarPage } from './pages/OccupancyCalendarPage'
import { PrivacyRequestsPage } from './pages/PrivacyRequestsPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ForbiddenPage } from './pages/ForbiddenPage'

const STAFF = ['ADMIN', 'MANAGER', 'RECEPCIONISTA'] as const
const CONFIG = ['ADMIN', 'MANAGER'] as const
const HOUSEKEEPING_ROLES = ['ADMIN', 'MANAGER', 'HOUSEKEEPING'] as const

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/403" element={<ForbiddenPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* Personal de recepción / gerencia / administración */}
          <Route element={<RequireAuth roles={[...STAFF]} />}>
            <Route path="/guests" element={<GuestsPage />} />
            <Route path="/guests/:id/reservations" element={<GuestReservationsPage />} />
            <Route path="/reservations" element={<ReservationsPage />} />
            <Route path="/reservations/new" element={<ReservationFormPage />} />
            <Route path="/reservations/:id" element={<ReservationDetailPage />} />
            <Route path="/availability" element={<AvailabilityPage />} />
            <Route path="/rooms" element={<RoomsPage />} />
            <Route path="/room-types" element={<RoomTypesPage />} />
            <Route path="/payments" element={<PaymentsPage />} />
            <Route path="/quote" element={<QuotePage />} />
            <Route path="/reservation-groups" element={<ReservationGroupsPage />} />
            <Route path="/occupancy-calendar" element={<OccupancyCalendarPage />} />
          </Route>

          {/* Configuración tarifaria / operativa */}
          <Route element={<RequireAuth roles={[...CONFIG]} />}>
            <Route path="/rate-plans" element={<RatePlansPage />} />
            <Route path="/seasonal-rates" element={<SeasonalRatesPage />} />
            <Route path="/taxes-and-fees" element={<TaxesAndFeesPage />} />
            <Route path="/cancellation-policies" element={<CancellationPoliciesPage />} />
            <Route path="/room-blocks" element={<RoomBlocksPage />} />
            <Route path="/users" element={<UsersPage />} />
          </Route>

          {/* Operación: housekeeping */}
          <Route element={<RequireAuth roles={[...HOUSEKEEPING_ROLES]} />}>
            <Route path="/housekeeping" element={<HousekeepingPage />} />
          </Route>

          {/* Privacidad */}
          <Route element={<RequireAuth roles={['PRIVACY_OFFICER']} />}>
            <Route path="/privacy-requests" element={<PrivacyRequestsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
