import { NavLink } from 'react-router-dom'
import clsx from 'clsx'
import { useAuth } from '../auth/AuthContext'
import { ROUTES } from '../utils/constants'
import type { UserRole } from '../api/types'

interface NavItem {
  to: string
  label: string
  roles?: UserRole[]
}

const NAV_ITEMS: NavItem[] = [
  { to: ROUTES.DASHBOARD, label: 'Panel' },
  { to: ROUTES.AVAILABILITY, label: 'Disponibilidad' },
  { to: ROUTES.RESERVATIONS, label: 'Reservas' },
  { to: ROUTES.GUESTS, label: 'Huéspedes' },
  { to: ROUTES.ROOMS, label: 'Habitaciones' },
  { to: ROUTES.ROOM_TYPES, label: 'Tipos de habitación' },
  { to: ROUTES.USERS, label: 'Usuarios', roles: ['ADMIN'] },
  { to: ROUTES.PAYMENTS, label: 'Pagos' },
]

export function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user } = useAuth()
  const items = NAV_ITEMS.filter((item) => !item.roles || (user && item.roles.includes(user.role)))

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/50 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}
      <aside
        className={clsx(
          'sticky top-0 flex h-screen w-60 flex-shrink-0 flex-col bg-slate-900 text-slate-200',
          'max-lg:fixed max-lg:left-0 max-lg:z-40 max-lg:transition-transform',
          open ? 'max-lg:translate-x-0' : 'max-lg:-translate-x-full',
        )}
      >
        <div className="flex items-center gap-2.5 border-b border-white/10 px-4 py-4">
          <span className="flex h-7 w-7 items-center justify-center rounded bg-blue-600 text-sm font-bold text-white">
            HM
          </span>
          <span className="font-semibold text-white">Hotel Manager</span>
        </div>
        <nav className="flex flex-col gap-0.5 overflow-y-auto px-2.5 py-3">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                clsx(
                  'rounded px-3 py-2 font-medium text-slate-300 hover:bg-white/10 hover:text-white',
                  isActive && 'bg-blue-600 text-white',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  )
}
