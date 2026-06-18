import { Link } from 'react-router-dom'
import { ROUTES, BTN_PRIMARY, BTN_BLOCK } from '../utils/constants'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-800 to-slate-900 p-5">
      <div className="w-full max-w-sm rounded-lg bg-white p-7 text-center shadow-xl">
        <h1 className="mb-1 text-xl font-semibold text-slate-900">404</h1>
        <p className="mb-5 text-sm text-slate-500">La página no existe.</p>
        <Link to={ROUTES.DASHBOARD} className={`${BTN_PRIMARY} ${BTN_BLOCK}`}>
          Volver al panel
        </Link>
      </div>
    </div>
  )
}
