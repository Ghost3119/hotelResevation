import type { NormalizedError } from '../api/types'
import { BTN_SECONDARY } from '../utils/constants'

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
    <div className="flex flex-col items-center justify-center gap-2 px-4 py-12 text-center text-red-600" role="alert">
      <strong>Error</strong>
      <span>{text}</span>
      {onRetry && (
        <button type="button" className={BTN_SECONDARY} onClick={onRetry}>
          Reintentar
        </button>
      )}
    </div>
  )
}
