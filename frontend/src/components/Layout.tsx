import { useState, type ReactNode } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Sidebar } from './Sidebar'
import { ROLE_LABELS, ROUTES, BTN_SECONDARY, BTN_SM } from '../utils/constants'

export function Layout(): ReactNode {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate(ROUTES.LOGIN, { replace: true })
  }

  return (
    <div className="flex min-h-screen">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b border-slate-200 bg-white px-4">
          <button
            type="button"
            className="px-1.5 py-1 text-xl text-slate-600 hover:bg-slate-100 lg:hidden"
            aria-label="Abrir menú"
            onClick={() => setSidebarOpen((v) => !v)}
          >
            ☰
          </button>
          <div className="flex items-center gap-3">
            <div className="flex flex-col items-end leading-tight">
              <span className="font-semibold text-slate-900">{user?.fullName ?? '—'}</span>
              <span className="text-xs text-slate-500">
                {user ? ROLE_LABELS[user.role] : ''}
              </span>
            </div>
            <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={handleLogout}>
              Cerrar sesión
            </button>
          </div>
        </header>
        <main className="flex-1 px-5 py-5 max-lg:px-4">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
