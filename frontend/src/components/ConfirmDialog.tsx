import clsx from 'clsx'
import { Spinner } from './Spinner'
import { BTN_DANGER, BTN_PRIMARY, BTN_SECONDARY } from '../utils/constants'

interface ConfirmDialogProps {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  destructive?: boolean
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  destructive = false,
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  if (!open) return null
  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/50 px-4 py-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
    >
      <div className="flex w-full max-w-md flex-col rounded-lg bg-white shadow-xl">
        <div className="flex items-center border-b border-slate-200 px-4 py-3.5">
          <h2 id="confirm-title" className="text-lg font-semibold text-slate-900">
            {title}
          </h2>
        </div>
        <div className="p-4">
          <p>{message}</p>
        </div>
        <div className="flex justify-end gap-2 border-t border-slate-200 px-4 py-3">
          <button type="button" className={BTN_SECONDARY} onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={clsx(destructive ? BTN_DANGER : BTN_PRIMARY)}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading && <Spinner size={14} />} {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
