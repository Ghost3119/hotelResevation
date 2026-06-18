import { Link } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { EmptyState } from '../components/EmptyState'
import { ROUTES, BTN_PRIMARY } from '../utils/constants'

export function PaymentsPage() {
  return (
    <div>
      <PageHeader title="Pagos" subtitle="Gestión de pagos por reserva" />
      <EmptyState
        title="Pagos gestionados por reserva"
        message="Los pagos se registran y consultan desde el detalle de cada reserva."
        action={
          <Link to={ROUTES.RESERVATIONS} className={BTN_PRIMARY}>
            Ir a reservas
          </Link>
        }
      />
    </div>
  )
}
