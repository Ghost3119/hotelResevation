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

interface NavSection {
  title?: string
  items: NavItem[]
  // Roles allowed to see the section header + items. If omitted, any authed user.
  roles?: UserRole[]
}

const ALL: UserRole[] = ['ADMIN', 'MANAGER', 'RECEPCIONISTA', 'HOUSEKEEPING', 'PRIVACY_OFFICER']

const SECTIONS: NavSection[] = [
  {
    items: [
      { to: ROUTES.DASHBOARD, label: 'Panel' },
      { to: ROUTES.QUOTE, label: 'Cotizador', roles: ['ADMIN', 'MANAGER', 'RECEPCIONISTA'] },
      { to: ROUTES.AVAILABILITY, label: 'Disponibilidad', roles: ['ADMIN', 'MANAGER', 'RECEPCIONISTA'] },
      { to: ROUTES.OCCUPANCY_CALENDAR, label: 'Calendario de ocupación', roles: ['ADMIN', 'MANAGER', 'RECEPCIONISTA'] },
    ],
  },
  {
    title: 'Reservas',
    roles: ['ADMIN', 'MANAGER', 'RECEPCIONISTA'],
    items: [
      { to: ROUTES.RESERVATIONS, label: 'Reservas' },
      { to: ROUTES.RESERVATION_GROUPS, label: 'Reservas grupales' },
      { to: ROUTES.GUESTS, label: 'Huéspedes' },
    ],
  },
  {
    title: 'Habitaciones',
    roles: ['ADMIN', 'MANAGER', 'RECEPCIONISTA'],
    items: [
      { to: ROUTES.ROOMS, label: 'Habitaciones' },
      { to: ROUTES.ROOM_TYPES, label: 'Tipos de habitación' },
      { to: ROUTES.PAYMENTS, label: 'Pagos' },
    ],
  },
  {
    title: 'Configuración',
    roles: ['ADMIN', 'MANAGER'],
    items: [
      { to: ROUTES.RATE_PLANS, label: 'Planes tarifarios' },
      { to: ROUTES.SEASONAL_RATES, label: 'Tarifas de temporada' },
      { to: ROUTES.TAXES_AND_FEES, label: 'Impuestos y cargos' },
      { to: ROUTES.CANCELLATION_POLICIES, label: 'Cancelación' },
      { to: ROUTES.ROOM_BLOCKS, label: 'Bloqueos' },
      { to: ROUTES.USERS, label: 'Usuarios', roles: ['ADMIN'] },
    ],
  },
  {
    title: 'Operación',
    roles: ['ADMIN', 'MANAGER', 'HOUSEKEEPING'],
    items: [{ to: ROUTES.HOUSEKEEPING, label: 'Limpieza' }],
  },
  {
    title: 'Privacidad',
    roles: ['PRIVACY_OFFICER'],
    items: [{ to: ROUTES.PRIVACY_REQUESTS, label: 'Solicitudes de privacidad' }],
  },
]

export function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user } = useAuth()
  const role = user?.role

  const visibleSections = SECTIONS.filter((section) => {
    const allowed = section.roles ?? ALL
    return role ? allowed.includes(role) : false
  }).map((section) => ({
    ...section,
    items: section.items.filter((item) => !item.roles || (role && item.roles.includes(role))),
  }))

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
        <nav className="flex flex-col gap-3 overflow-y-auto px-2.5 py-3" aria-label="Navegación principal">
          {visibleSections.map((section, idx) => (
            <div key={section.title ?? `section-${idx}`} className="flex flex-col gap-0.5">
              {section.title && (
                <span className="px-3 pb-1 pt-1 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                  {section.title}
                </span>
              )}
              {section.items.map((item) => (
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
            </div>
          ))}
        </nav>
      </aside>
    </>
  )
}
