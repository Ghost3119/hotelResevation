import type { ReactNode } from 'react'

interface EmptyStateProps {
  title?: string
  message?: string
  action?: ReactNode
}

export function EmptyState({
  title = 'Sin resultados',
  message = 'No hay elementos para mostrar.',
  action,
}: EmptyStateProps) {
  return (
    <div className="state state-empty">
      <strong>{title}</strong>
      <span>{message}</span>
      {action}
    </div>
  )
}
