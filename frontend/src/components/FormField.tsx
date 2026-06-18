import clsx from 'clsx'
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import {
  FORM_ERROR,
  FORM_FIELD,
  FORM_HINT,
  FORM_LABEL,
  INPUT,
  INPUT_ERROR,
} from '../utils/constants'

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
    <div className={clsx(FORM_FIELD, wrapperClassName)}>
      {label && (
        <label htmlFor={inputId} className={FORM_LABEL}>
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={clsx(INPUT, error && INPUT_ERROR, className)}
        {...rest}
      />
      {hint && !error && <span className={FORM_HINT}>{hint}</span>}
      {error && <span className={FORM_ERROR}>{error}</span>}
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
    <div className={clsx(FORM_FIELD, wrapperClassName)}>
      {label && (
        <label htmlFor={selectId} className={FORM_LABEL}>
          {label}
        </label>
      )}
      <select id={selectId} className={clsx(INPUT, error && INPUT_ERROR, className)} {...rest}>
        {children}
      </select>
      {hint && !error && <span className={FORM_HINT}>{hint}</span>}
      {error && <span className={FORM_ERROR}>{error}</span>}
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
    <div className={clsx(FORM_FIELD, wrapperClassName)}>
      {label && (
        <label htmlFor={areaId} className={FORM_LABEL}>
          {label}
        </label>
      )}
      <textarea id={areaId} className={clsx(INPUT, error && INPUT_ERROR, className)} {...rest} />
      {hint && !error && <span className={FORM_HINT}>{hint}</span>}
      {error && <span className={FORM_ERROR}>{error}</span>}
    </div>
  )
}
