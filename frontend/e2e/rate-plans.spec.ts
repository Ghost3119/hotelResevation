import { test, expect } from '@playwright/test'
import { backendAvailable, loginAsAdmin, skipIfNoBackend } from './helpers'

let backendOk = false

test.beforeAll(async () => {
  backendOk = await backendAvailable()
})

test.beforeEach(async ({ page }) => {
  skipIfNoBackend(backendOk)
  await loginAsAdmin(page)
})

test.describe('Planes tarifarios (rate plans)', () => {
  test('crear un nuevo plan tarifario y verificar que aparece en el listado', async ({ page }) => {
    await page.goto('/rate-plans')
    await expect(page.getByRole('heading', { name: /Planes tarifarios/i })).toBeVisible()

    await page.getByRole('button', { name: 'Nuevo plan' }).click()
    await expect(page.getByRole('heading', { name: /Nuevo plan/i })).toBeVisible()

    const code = `E2E-${Date.now()}`
    await page.getByLabel('Código').fill(code)
    await page.getByLabel('Nombre').fill('Plan E2E')
    await page.getByLabel('Tipo de habitación').selectOption({ index: 1 })
    // Siete tarifas por dia de la semana (lun-dom).
    for (const day of ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']) {
      await page.getByLabel(day, { exact: true }).fill('1500')
    }
    await page.getByRole('button', { name: 'Crear plan' }).click()

    await expect(page.getByText(/Plan tarifario creado./i)).toBeVisible()
    await expect(page.getByText(code)).toBeVisible()
  })
})
