import type { ReactNode } from 'react'
import { Spinner } from './Spinner'

interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  size?: 'sm' | 'md' | 'lg'
}

export function Modal({ open, title, onClose, children, footer, size = 'md' }: ModalProps) {
  if (!open) return null
  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div className={`modal modal-${size}`}>
        <div className="modal-header">
          <h2 id="modal-title">{title}</h2>
          <button
            type="button"
            className="modal-close"
            aria-label="Cerrar"
            onClick={onClose}
          >
            ×
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
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
    <button type="submit" className="btn btn-primary" disabled={loading}>
      {loading && <Spinner size={14} />} {label}
    </button>
  )
}
