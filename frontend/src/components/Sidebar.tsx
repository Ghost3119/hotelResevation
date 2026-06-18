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
      {open && <div className="sidebar-backdrop" onClick={onClose} aria-hidden="true" />}
      <aside className={clsx('sidebar', open && 'sidebar-open')}>
        <div className="sidebar-brand">
          <span className="sidebar-logo">HM</span>
          <span className="sidebar-title">Hotel Manager</span>
        </div>
        <nav className="sidebar-nav">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) => clsx('sidebar-link', isActive && 'active')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  )
}
