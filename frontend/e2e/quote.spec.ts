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

test.describe('Cotizador (quote)', () => {
  test('el desglose por noche muestra importes en MXN', async ({ page }) => {
    await page.goto('/quote')
    await expect(page.getByRole('heading', { name: 'Cotizador' })).toBeVisible()

    await page.getByLabel('Entrada').fill(futureISO(5))
    await page.getByLabel('Salida').fill(futureISO(7))
    await page.getByLabel('Adultos').fill('2')

    const tipoSelect = page.getByLabel('Tipo de habitación')
    await expect(tipoSelect.locator('option')).not.toHaveCount(0)
    await tipoSelect.selectOption({ index: 1 })

    await page.getByRole('button', { name: 'Cotizar', exact: true }).click()

    // El desglose por noche aparece (sección con encabezado y tabla).
    await expect(page.getByRole('heading', { name: 'Desglose por noche' })).toBeVisible({ timeout: 10000 })
    // Los importes se muestran en MXN ($X,XXX.XX) y nunca en euros.
    await expect(page.getByText(/\$\d[\d,]*\.\d{2}/).first()).toBeVisible()
    await expect(page.getByText(/€/)).toHaveCount(0)
    // El total general tambien en MXN.
    await expect(page.getByTestId('quote-grand-total')).toBeVisible()
  })
})
