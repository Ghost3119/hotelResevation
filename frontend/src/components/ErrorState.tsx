import type { NormalizedError } from '../api/types'

interface ErrorStateProps {
  error?: unknown
  message?: string
  onRetry?: () => void
}

function asNormalized(error: unknown): NormalizedError | null {
  if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
    return error as NormalizedError
  }
  return null
}

export function ErrorState({ error, message, onRetry }: ErrorStateProps) {
  const normalized = asNormalized(error)
  const text = message || normalized?.message || 'Se ha producido un error.'
  return (
    <div className="state state-error" role="alert">
      <strong>Error</strong>
      <span>{text}</span>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Reintentar
        </button>
      )}
    </div>
  )
}
