import { test, expect } from '@playwright/test'
import { backendAvailable, futureISO, loginAsAdmin, skipIfNoBackend } from './helpers'

let backendOk = false

test.beforeAll(async () => {
  backendOk = await backendAvailable()
})

test.beforeEach(async ({ page }) => {
  skipIfNoBackend(backendOk)
  await loginAsAdmin(page)
})

test.describe('Disponibilidad', () => {
  test('la búsqueda devuelve habitaciones disponibles con precios en MXN', async ({ page }) => {
    await page.goto('/availability')
    await expect(page.getByRole('heading', { name: 'Disponibilidad' })).toBeVisible()

    // El dropdown de tipos se llena desde la respuesta paginada (BUG-1).
    const tipoSelect = page.getByLabel('Tipo de habitación')
    await expect(tipoSelect).toBeVisible()
    // Al menos debe existir la opción "Todos".
    await expect(tipoSelect.locator('option')).toContainText(['Todos'])

    await page.getByLabel('Entrada').fill(futureISO(5))
    await page.getByLabel('Salida').fill(futureISO(7))
    await page.getByLabel('Huéspedes').fill('2')
    await page.getByRole('button', { name: 'Buscar' }).click()

    // Aparecen tarjetas de habitaciones disponibles (formato "Hab. NNN").
    await expect(page.getByText(/^Hab\. \d+/).first()).toBeVisible({ timeout: 10000 })

    // Los precios se muestran en MXN ($X,XXX.XX) y no en euros.
    await expect(page.getByText(/\$\d[\d,]*\.\d{2}/).first()).toBeVisible()
    await expect(page.getByText(/€/)).toHaveCount(0)
  })
})
