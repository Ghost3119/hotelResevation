export function Spinner({ size = 20 }: { size?: number }) {
  return (
    <span
      className="inline-block animate-spin rounded-full border-2 border-slate-200 border-t-blue-600"
      style={{ width: size, height: size }}
      role="status"
      aria-label="Cargando"
    />
  )
}
