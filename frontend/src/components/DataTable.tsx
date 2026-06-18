import type { ReactNode } from 'react'
import { Pagination } from './Pagination'

export interface Column<T> {
  key: string
  header: string
  render?: (row: T) => ReactNode
  className?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  data: T[]
  rowKey: (row: T) => string | number
  page: number
  size: number
  totalElements: number
  totalPages: number
  onPageChange: (page: number) => void
  onRowClick?: (row: T) => void
  empty?: ReactNode
}

export function DataTable<T>({
  columns,
  data,
  rowKey,
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onRowClick,
  empty,
}: DataTableProps<T>) {
  return (
    <div className="data-table">
      <table>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} className={col.className}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr className="data-table-empty-row">
              <td colSpan={columns.length}>{empty ?? 'Sin resultados.'}</td>
            </tr>
          ) : (
            data.map((row) => (
              <tr
                key={rowKey(row)}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={onRowClick ? 'row-clickable' : undefined}
              >
                {columns.map((col) => (
                  <td key={col.key} className={col.className}>
                    {col.render
                      ? col.render(row)
                      : String((row as Record<string, unknown>)[col.key] ?? '')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
      <Pagination
        page={page}
        size={size}
        totalElements={totalElements}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />
    </div>
  )
}
