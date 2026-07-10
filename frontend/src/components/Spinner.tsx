export function Spinner({ size = 20 }: { size?: number }) {
  const sizeClass = size === 14 ? 'h-3.5 w-3.5' : size === 28 ? 'h-7 w-7' : 'h-5 w-5'
  return (
    <span
      className={`${sizeClass} inline-block animate-spin rounded-full border-2 border-slate-200 border-t-blue-600`}
      role="status"
      aria-label="Cargando"
    />
  )
}
