import { render as rtlRender } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '../auth/AuthContext'
import { ToastProvider } from '../components/Toast'
import { adminUser, recepUser, setMeUser } from './handlers'
import { STORAGE_TOKEN_KEY } from '../utils/constants'
import type { ReactElement, ReactNode } from 'react'
import type { UserDto, UserRole } from '../api/types'

interface RenderOptions {
  route?: string
  role?: UserRole
  authenticated?: boolean
}

const ROLE_USERS: Record<UserRole, UserDto> = {
  ADMIN: adminUser,
  RECEPCIONISTA: recepUser,
  MANAGER: { id: 3, email: 'manager@hotel.test', fullName: 'Manager User', role: 'MANAGER', active: true },
  HOUSEKEEPING: { id: 4, email: 'housekeeping@hotel.test', fullName: 'Housekeeping User', role: 'HOUSEKEEPING', active: true },
  PRIVACY_OFFICER: { id: 5, email: 'privacy@hotel.test', fullName: 'Privacy Officer', role: 'PRIVACY_OFFICER', active: true },
}

export function render(ui: ReactElement, options: RenderOptions = {}) {
  const { route = '/', role = 'ADMIN', authenticated = false } = options

  if (authenticated) {
    window.localStorage.setItem(STORAGE_TOKEN_KEY, 'test-token')
    setMeUser(ROLE_USERS[role] ?? adminUser)
  } else {
    window.localStorage.removeItem(STORAGE_TOKEN_KEY)
    setMeUser(adminUser)
  }

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0, staleTime: 0, refetchOnWindowFocus: false },
    },
  })

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>
          <ToastProvider>
            <AuthProvider>{children}</AuthProvider>
          </ToastProvider>
        </MemoryRouter>
      </QueryClientProvider>
    )
  }

  return {
    user: userEvent.setup(),
    ...rtlRender(ui, { wrapper: Wrapper }),
  }
}
