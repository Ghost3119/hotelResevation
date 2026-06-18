import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { authApi } from '../api/auth.api'
import type { LoginRequest, UserDto } from '../api/types'
import { STORAGE_TOKEN_KEY } from '../utils/constants'

interface AuthContextValue {
  user: UserDto | null
  token: string | null
  loading: boolean
  login: (data: LoginRequest) => Promise<UserDto>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null)
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(STORAGE_TOKEN_KEY)
  )
  const [loading, setLoading] = useState<boolean>(() => !!localStorage.getItem(STORAGE_TOKEN_KEY))

  useEffect(() => {
    let active = true
    const stored = localStorage.getItem(STORAGE_TOKEN_KEY)
    if (!stored) {
      setLoading(false)
      return
    }
    authApi
      .me()
      .then((me) => {
        if (active) {
          setUser(me)
          setToken(stored)
        }
      })
      .catch(() => {
        if (active) {
          localStorage.removeItem(STORAGE_TOKEN_KEY)
          setUser(null)
          setToken(null)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const login = useCallback(async (data: LoginRequest) => {
    const res = await authApi.login(data)
    localStorage.setItem(STORAGE_TOKEN_KEY, res.token)
    setToken(res.token)
    setUser(res.user)
    return res.user
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_TOKEN_KEY)
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, token, loading, login, logout }),
    [user, token, loading, login, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
