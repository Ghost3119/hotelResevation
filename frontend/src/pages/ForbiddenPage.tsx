import { Link } from 'react-router-dom'
import { ROUTES } from '../utils/constants'

export function ForbiddenPage() {
  return (
    <div className="login-shell">
      <div className="login-card" style={{ textAlign: 'center' }}>
        <h1>403</h1>
        <p className="login-subtitle">No tienes permisos para acceder a esta sección.</p>
        <Link to={ROUTES.DASHBOARD} className="btn btn-primary btn-block">
          Volver al panel
        </Link>
      </div>
    </div>
  )
}
