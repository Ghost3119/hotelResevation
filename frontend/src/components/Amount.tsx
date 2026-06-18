import { formatCurrency } from '../utils/format'

export function Amount({
  value,
  className,
}: {
  value: number | null | undefined
  className?: string
}) {
  return <span className={className}>{formatCurrency(value)}</span>
}
