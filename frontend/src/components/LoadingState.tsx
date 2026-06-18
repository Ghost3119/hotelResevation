import { Spinner } from './Spinner'

export function LoadingState({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="state state-loading" role="status" aria-live="polite">
      <Spinner />
      <span>{label}</span>
    </div>
  )
}
