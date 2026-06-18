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
    <div className="pagination">
      <span className="pagination-info">
        {totalElements === 0
          ? '0 resultados'
          : `${from}–${to} de ${totalElements}`}
      </span>
      <div className="pagination-controls">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
        >
          Anterior
        </button>
        {pages.map((p, idx) =>
          p === '…' ? (
            <span key={`gap-${idx}`} className="pagination-gap">…</span>
          ) : (
            <button
              key={p}
              type="button"
              className={p === page ? 'btn btn-page active' : 'btn btn-page'}
              onClick={() => onPageChange(p)}
            >
              {p + 1}
            </button>
          )
        )}
        <button
          type="button"
          className="btn btn-secondary btn-sm"
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
