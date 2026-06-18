import { test, expect, type Page } from '@playwright/test'

export const BACKEND_BASE = 'http://localhost:8080/api'
export const ADMIN_EMAIL = 'admin@hotel.test'
export const ADMIN_PASSWORD = 'admin123'

/**
 * Comprueba si el backend es alcanzable consultando el endpoint OpenAPI.
 * Se usa para omitir (skip) las pruebas E2E de forma graciosa cuando el
 * backend no está levantado (p. ej., en entornos sin Docker/Postgres).
 */
export async function backendAvailable(): Promise<boolean> {
  try {
    const res = await fetch(`${BACKEND_BASE}/openapi.json`, {
      signal: AbortSignal.timeout(4000),
    })
    return res.ok
  } catch {
    return false
  }
}

/**
 * Omite todas las pruebas del archivo si el backend no es alcanzable.
 * Llamar en un `test.beforeAll` + `test.beforeEach` de cada spec:
 *
 *   let backendOk = false
 *   test.beforeAll(async () => { backendOk = await backendAvailable() })
 *   test.beforeEach(async () => { test.skip(!backendOk, '...') })
 */
export function skipIfNoBackend(backendOk: boolean): void {
  test.skip(!backendOk, 'Backend no disponible en http://localhost:8080 — se omiten las pruebas E2E')
}

/** Inicia sesión como administrador y espera a llegar al dashboard. */
export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill(ADMIN_EMAIL)
  await page.getByLabel('Contraseña').fill(ADMIN_PASSWORD)
  await page.getByRole('button', { name: /Entrar/ }).click()
  await page.waitForURL('**/dashboard')
}

/** Devuelve una fecha futura en formato yyyy-MM-dd. */
export function futureISO(daysAhead: number): string {
  const d = new Date()
  d.setDate(d.getDate() + daysAhead)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export { test, expect }
