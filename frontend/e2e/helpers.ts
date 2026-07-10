/* global process: readonly */
import { test, expect, type Page } from '@playwright/test'

export const BACKEND_BASE = 'http://localhost:5173/api'

function requiredEnv(name: string): string {
  const value = process.env[name]
  if (!value) throw new Error(`${name} is required for E2E tests`)
  return value
}

export const ADMIN_EMAIL = requiredEnv('ADMIN_EMAIL')
export const ADMIN_PASSWORD = requiredEnv('ADMIN_PASSWORD')

/** Check the backend through the same-origin frontend proxy. */
export async function backendAvailable(): Promise<boolean> {
  try {
    const res = await fetch(`${BACKEND_BASE}/auth/refresh`, {
      method: 'POST',
      signal: AbortSignal.timeout(4000),
    })
    return res.status < 500
  } catch {
    return false
  }
}

export function skipIfNoBackend(backendOk: boolean): void {
  test.skip(!backendOk, 'Backend unavailable through /api; skipping E2E tests')
}

export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login')
  await page.getByLabel(/Correo electr/i).fill(ADMIN_EMAIL)
  await page.getByLabel(/Contrase/i).fill(ADMIN_PASSWORD)
  await page.getByRole('button', { name: /Entrar/ }).click()
  await page.waitForURL('**/dashboard')
}

export function futureISO(daysAhead: number): string {
  const d = new Date()
  d.setDate(d.getDate() + daysAhead)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export { test, expect }
