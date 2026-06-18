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
    <div className="flex flex-col items-center justify-center gap-2 px-4 py-12 text-center text-slate-500">
      <strong>{title}</strong>
      <span>{message}</span>
      {action}
    </div>
  )
}
