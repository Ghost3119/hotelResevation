import { Link } from 'react-router-dom'
import { ROUTES } from '../utils/constants'

export function NotFoundPage() {
  return (
    <div className="login-shell">
      <div className="login-card" style={{ textAlign: 'center' }}>
        <h1>404</h1>
        <p className="login-subtitle">La página no existe.</p>
        <Link to={ROUTES.DASHBOARD} className="btn btn-primary btn-block">
          Volver al panel
        </Link>
      </div>
    </div>
  )
}
