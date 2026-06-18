import clsx from 'clsx'
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
  wrapperClassName?: string
}

export function Input({
  label,
  error,
  hint,
  wrapperClassName,
  className,
  id,
  ...rest
}: InputProps) {
  const inputId = id || rest.name
  return (
    <div className={clsx('form-field', wrapperClassName)}>
      {label && (
        <label htmlFor={inputId} className="form-label">
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={clsx('input', error && 'input-error', className)}
        {...rest}
      />
      {hint && !error && <span className="form-hint">{hint}</span>}
      {error && <span className="form-error">{error}</span>}
    </div>
  )
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  hint?: string
  children: ReactNode
  wrapperClassName?: string
}

export function Select({
  label,
  error,
  hint,
  children,
  wrapperClassName,
  className,
  id,
  ...rest
}: SelectProps) {
  const selectId = id || rest.name
  return (
    <div className={clsx('form-field', wrapperClassName)}>
      {label && (
        <label htmlFor={selectId} className="form-label">
          {label}
        </label>
      )}
      <select id={selectId} className={clsx('input', error && 'input-error', className)} {...rest}>
        {children}
      </select>
      {hint && !error && <span className="form-hint">{hint}</span>}
      {error && <span className="form-error">{error}</span>}
    </div>
  )
}

interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  hint?: string
  wrapperClassName?: string
}

export function TextArea({
  label,
  error,
  hint,
  wrapperClassName,
  className,
  id,
  ...rest
}: TextAreaProps) {
  const areaId = id || rest.name
  return (
    <div className={clsx('form-field', wrapperClassName)}>
      {label && (
        <label htmlFor={areaId} className="form-label">
          {label}
        </label>
      )}
      <textarea id={areaId} className={clsx('input', error && 'input-error', className)} {...rest} />
      {hint && !error && <span className="form-hint">{hint}</span>}
      {error && <span className="form-error">{error}</span>}
    </div>
  )
}
