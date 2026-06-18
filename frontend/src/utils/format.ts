import { format, parseISO } from 'date-fns'

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  try {
    return format(parseISO(iso), 'yyyy-MM-dd')
  } catch {
    return String(iso)
  }
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  try {
    return format(parseISO(iso), 'yyyy-MM-dd HH:mm')
  } catch {
    return String(iso)
  }
}

export function formatCurrency(amount: number | null | undefined): string {
  if (amount == null) return '—'
  return new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatPercent(value: number | null | undefined): string {
  if (value == null) return '—'
  return `${value.toFixed(1)}%`
}

export function todayISO(): string {
  return format(new Date(), 'yyyy-MM-dd')
}

export function fullName(first: string, last: string): string {
  return `${first} ${last}`.trim()
}
