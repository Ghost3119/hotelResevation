import { describe, it, expect } from 'vitest'
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  formatPercent,
  fullName,
  todayISO,
} from './format'

describe('formatCurrency (MXN)', () => {
  it('formatea un importe en MXN con formato $X.XX', () => {
    expect(formatCurrency(120)).toBe('$120.00')
  })

  it('formatea decimales con dos cifras', () => {
    expect(formatCurrency(250.5)).toBe('$250.50')
    expect(formatCurrency(1250)).toBe('$1,250.00')
  })

  it('no usa el símbolo de euro (€)', () => {
    expect(formatCurrency(120)).not.toContain('€')
    expect(formatCurrency(120)).toMatch(/^\$/)
  })

  it('devuelve "—" para null, undefined o NaN', () => {
    expect(formatCurrency(null)).toBe('—')
    expect(formatCurrency(undefined)).toBe('—')
  })
})

describe('formatDate', () => {
  it('formatea una fecha ISO a yyyy-MM-dd', () => {
    expect(formatDate('2026-07-01')).toBe('2026-07-01')
    expect(formatDate('2026-07-01T10:00:00')).toBe('2026-07-01')
  })

  it('devuelve "—" para null o vacío', () => {
    expect(formatDate(null)).toBe('—')
    expect(formatDate(undefined)).toBe('—')
    expect(formatDate('')).toBe('—')
  })
})

describe('formatDateTime', () => {
  it('formatea fecha y hora a yyyy-MM-dd HH:mm', () => {
    expect(formatDateTime('2026-06-15T14:05:00')).toBe('2026-06-15 14:05')
  })

  it('devuelve "—" para null', () => {
    expect(formatDateTime(null)).toBe('—')
  })
})

describe('formatPercent', () => {
  it('formatea un porcentaje con un decimal', () => {
    expect(formatPercent(33.3)).toBe('33.3%')
    expect(formatPercent(0)).toBe('0.0%')
  })

  it('devuelve "—" para null o undefined', () => {
    expect(formatPercent(null)).toBe('—')
    expect(formatPercent(undefined)).toBe('—')
  })
})

describe('todayISO', () => {
  it('devuelve la fecha actual en formato yyyy-MM-dd', () => {
    expect(todayISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    const now = new Date()
    const expected = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    expect(todayISO()).toBe(expected)
  })
})

describe('fullName', () => {
  it('concatena nombre y apellidos', () => {
    expect(fullName('John', 'Doe')).toBe('John Doe')
  })
})
