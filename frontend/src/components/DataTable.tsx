import type { ReactNode } from 'react'
import clsx from 'clsx'
import { Pagination } from './Pagination'
import {
  DATA_TABLE,
  ROW_CLICKABLE,
  TABLE_EMPTY_TD,
  TABLE_TD,
  TABLE_TH,
} from '../utils/constants'

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
    <div className={DATA_TABLE}>
      <table className="w-full border-collapse">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} className={clsx(TABLE_TH, col.className)}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td className={TABLE_EMPTY_TD} colSpan={columns.length}>{empty ?? 'Sin resultados.'}</td>
            </tr>
          ) : (
            data.map((row) => (
              <tr
                key={rowKey(row)}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={onRowClick ? ROW_CLICKABLE : undefined}
              >
                {columns.map((col) => (
                  <td key={col.key} className={clsx(TABLE_TD, col.className)}>
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
