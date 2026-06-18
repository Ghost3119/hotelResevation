import { Spinner } from './Spinner'

export function LoadingState({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-4 py-12 text-center text-slate-600" role="status" aria-live="polite">
      <Spinner />
      <span>{label}</span>
    </div>
  )
}
