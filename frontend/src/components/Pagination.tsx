import clsx from 'clsx'
import { BTN, BTN_SECONDARY, BTN_SM } from '../utils/constants'

interface PaginationProps {
  page: number
  size: number
  totalElements: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
}: PaginationProps) {
  const from = totalElements === 0 ? 0 : page * size + 1
  const to = Math.min((page + 1) * size, totalElements)

  const pages = computePages(page, totalPages)

  return (
    <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 bg-slate-50 px-3 py-2.5">
      <span className="text-xs text-slate-500">
        {totalElements === 0
          ? '0 resultados'
          : `${from}–${to} de ${totalElements}`}
      </span>
      <div className="flex flex-wrap items-center gap-1">
        <button
          type="button"
          className={clsx(BTN_SECONDARY, BTN_SM)}
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
        >
          Anterior
        </button>
        {pages.map((p, idx) =>
          p === '…' ? (
            <span key={`gap-${idx}`} className="px-1 text-slate-500">…</span>
          ) : (
            <button
              key={p}
              type="button"
              className={clsx(
                BTN,
                'min-w-[34px] px-2.5 py-1 text-xs',
                p === page
                  ? 'border-blue-600 bg-blue-600 text-white'
                  : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50',
              )}
              onClick={() => onPageChange(p)}
            >
              {p + 1}
            </button>
          )
        )}
        <button
          type="button"
          className={clsx(BTN_SECONDARY, BTN_SM)}
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
        >
          Siguiente
        </button>
      </div>
    </div>
  )
}

function computePages(page: number, totalPages: number): (number | '…')[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i)
  }
  const result: (number | '…')[] = [0]
  const start = Math.max(1, page - 1)
  const end = Math.min(totalPages - 2, page + 1)
  if (start > 1) result.push('…')
  for (let i = start; i <= end; i++) result.push(i)
  if (end < totalPages - 2) result.push('…')
  result.push(totalPages - 1)
  return result
}
