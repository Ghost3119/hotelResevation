import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import clsx from 'clsx'

type ToastKind = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  kind: ToastKind
  message: string
}

interface ToastContextValue {
  notify: (kind: ToastKind, message: string) => void
  success: (message: string) => void
  error: (message: string) => void
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined)

const TOAST_KIND_BORDER: Record<ToastKind, string> = {
  success: 'border-l-4 border-l-green-500',
  error: 'border-l-4 border-l-red-500',
  info: 'border-l-4 border-l-cyan-500',
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const remove = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const notify = useCallback(
    (kind: ToastKind, message: string) => {
      const id = Date.now() + Math.random()
      setToasts((prev) => [...prev, { id, kind, message }])
      setTimeout(() => remove(id), 4000)
    },
    [remove]
  )

  const value = useMemo<ToastContextValue>(
    () => ({
      notify,
      success: (m: string) => notify('success', m),
      error: (m: string) => notify('error', m),
    }),
    [notify]
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-5 right-5 z-[80] flex min-w-[240px] max-w-[360px] flex-col gap-2" aria-live="polite">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={clsx(
              'flex items-center gap-2.5 rounded-md border border-slate-200 bg-white px-3.5 py-2.5 shadow-md',
              TOAST_KIND_BORDER[t.kind],
            )}
            role="status"
          >
            <span>{t.message}</span>
            <button
              type="button"
              className="text-lg leading-none text-slate-400 hover:text-slate-700"
              onClick={() => remove(t.id)}
              aria-label="Cerrar"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast debe usarse dentro de ToastProvider')
  return ctx
}
