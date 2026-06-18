import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Input } from '../components/FormField'
import { Spinner } from '../components/Spinner'
import { ROUTES } from '../utils/constants'
import type { NormalizedError } from '../api/types'

export function LoginPage() {
  const { login, user, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('admin@hotel.test')
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
      <div className="login-shell">
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
    <div className="login-shell">
      <form className="login-card" onSubmit={onSubmit} noValidate>
        <h1>Hotel Manager</h1>
        <p className="login-subtitle">Acceso al panel de recepción</p>
        {error && <div className="form-alert form-alert-error" role="alert">{error}</div>}
        <Input
          label="Correo electrónico"
          name="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="username"
          required
        />
        <Input
          label="Contraseña"
          name="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting && <Spinner size={14} />} Entrar
        </button>
      </form>
    </div>
  )
}
