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

test.describe('Housekeeping', () => {
  test('crea una tarea y avanza su estado (DIRTY -> CLEANING)', async ({ page }) => {
    await page.goto('/housekeeping')
    await expect(page.getByRole('heading', { name: 'Limpieza' })).toBeVisible()

    // Crea una tarea DIRTY determinista sobre una habitacion semilla (id=1, 101).
    await page.getByRole('button', { name: 'Nueva tarea' }).click()
    await expect(page.getByRole('heading', { name: 'Nueva tarea de limpieza' })).toBeVisible()
    await page.getByLabel('ID de habitación').fill('1')
    await page.getByRole('button', { name: 'Crear tarea' }).click()
    await expect(page.getByText('Tarea de limpieza creada.')).toBeVisible()

    // La nueva tarea queda la primera (orden por created_at desc) en estado Sucio.
    await expect(page.getByRole('button', { name: 'Cambiar estado' }).first()).toBeVisible({ timeout: 10000 })

    // Avanza el estado de esa tarea: DIRTY -> CLEANING (Limpiando).
    await page.getByRole('button', { name: 'Cambiar estado' }).first().click()
    await expect(page.getByRole('heading', { name: /Actualizar estado/ })).toBeVisible()
    await page.getByLabel('Nuevo estado').selectOption({ label: 'Limpiando' })
    await page.getByRole('button', { name: 'Aplicar' }).click()

    await expect(page.getByText('Estado de la tarea actualizado.')).toBeVisible()
  })
})
