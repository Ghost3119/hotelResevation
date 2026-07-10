import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api/auth.api'
import type { LoginRequest, UserDto } from '../api/types'
import { getAccessToken, setAccessToken, subscribeAccessToken } from './tokenStore'

interface AuthContextValue {
  user: UserDto | null
  token: string | null
  loading: boolean
  login: (data: LoginRequest) => Promise<UserDto>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

let restoreSessionPromise: Promise<{ token: string; user: UserDto }> | null = null

function restoreSession() {
  if (!restoreSessionPromise) {
    restoreSessionPromise = authApi.refresh()
      .then((refresh) => {
        setAccessToken(refresh.token)
        return authApi.me().then((user) => ({ token: refresh.token, user }))
      })
      .finally(() => {
        restoreSessionPromise = null
      })
  }
  return restoreSessionPromise
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<UserDto | null>(null)
  const [token, setToken] = useState<string | null>(() => getAccessToken())
  const [loading, setLoading] = useState(true)

  useEffect(() => subscribeAccessToken((nextToken) => {
    setToken(nextToken)
    if (!nextToken) {
      setUser(null)
      queryClient.clear()
    }
  }), [queryClient])

  useEffect(() => {
    let active = true
    restoreSession()
      .then(({ token: restoredToken, user: restoredUser }) => {
        if (active) {
          setAccessToken(restoredToken)
          setUser(restoredUser)
        }
      })
      .catch(() => {
        if (active) {
          setAccessToken(null)
          setUser(null)
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
    queryClient.clear()
    setAccessToken(res.token)
    setUser(res.user)
    return res.user
  }, [queryClient])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // Clear local sensitive state even when the server cannot be reached.
    } finally {
      setAccessToken(null)
      setUser(null)
      queryClient.clear()
    }
  }, [queryClient])

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
