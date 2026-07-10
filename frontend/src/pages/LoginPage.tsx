import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Input } from '../components/FormField'
import { Spinner } from '../components/Spinner'
import { ROUTES, BTN_PRIMARY, BTN_BLOCK, FORM_ALERT_ERROR } from '../utils/constants'
import type { NormalizedError } from '../api/types'

export function LoginPage() {
  const { login, user, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname

  useEffect(() => {
    if (user) {
      navigate(from || ROUTES.DASHBOARD, { replace: true })
    }
  }, [user, navigate, from])

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-800 to-slate-900">
        <Spinner size={28} />
      </div>
    )
  }

  if (user) {
    return <Navigate to={ROUTES.DASHBOARD} replace />
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login({ email: email.trim(), password })
      navigate(ROUTES.DASHBOARD, { replace: true })
    } catch (err) {
      const normalized = err as NormalizedError
      setError(normalized.message || 'Credenciales no válidas.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-800 to-slate-900 p-5">
      <form className="w-full max-w-sm rounded-lg bg-white p-7 shadow-xl" onSubmit={onSubmit} noValidate>
        <h1 className="mb-1 text-xl font-semibold text-slate-900">Hotel Manager</h1>
        <p className="mb-5 text-sm text-slate-500">Acceso al panel de recepción</p>
        {error && (
          <div className={FORM_ALERT_ERROR} role="alert">{error}</div>
        )}
        <Input
          label="Correo electrónico"
          name="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          maxLength={254}
          autoComplete="username"
          required
        />
        <Input
          label="Contraseña"
          name="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          maxLength={72}
          autoComplete="current-password"
          required
        />
        <button type="submit" className={`${BTN_PRIMARY} ${BTN_BLOCK}`} disabled={submitting}>
          {submitting && <Spinner size={14} />} Entrar
        </button>
      </form>
    </main>
  )
}
