import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'
import type { UserRole } from '../api/types'

interface RoleGateProps {
  roles: UserRole[]
  children: ReactNode
}

export function RoleGate({ roles, children }: RoleGateProps) {
  const { user } = useAuth()
  if (!user || !roles.includes(user.role)) return null
  return <>{children}</>
}
