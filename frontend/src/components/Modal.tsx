import type { ReactNode } from 'react'
import clsx from 'clsx'
import { Spinner } from './Spinner'
import { BTN_PRIMARY } from '../utils/constants'

interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  size?: 'sm' | 'md' | 'lg'
}

const SIZE_MAP: Record<NonNullable<ModalProps['size']>, string> = {
  sm: 'max-w-md',
  md: 'max-w-2xl',
  lg: 'max-w-3xl',
}

export function Modal({ open, title, onClose, children, footer, size = 'md' }: ModalProps) {
  if (!open) return null
  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/50 px-4 py-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div className={clsx('flex w-full max-h-[88vh] flex-col rounded-lg bg-white shadow-xl', SIZE_MAP[size])}>
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3.5">
          <h2 id="modal-title" className="text-lg font-semibold text-slate-900">
            {title}
          </h2>
          <button
            type="button"
            className="px-1.5 text-2xl leading-none text-slate-400 hover:text-slate-700"
            aria-label="Cerrar"
            onClick={onClose}
          >
            ×
          </button>
        </div>
        <div className="overflow-y-auto p-4">{children}</div>
        {footer && (
          <div className="flex justify-end gap-2 border-t border-slate-200 px-4 py-3">
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}

interface ModalSubmitButtonProps {
  loading?: boolean
  label?: string
}

export function ModalSubmitButton({ loading = false, label = 'Guardar' }: ModalSubmitButtonProps) {
  return (
    <button type="submit" className={BTN_PRIMARY} disabled={loading}>
      {loading && <Spinner size={14} />} {label}
    </button>
  )
}
