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

test.describe('Habitaciones', () => {
  test('la tabla de habitaciones rendera y muestra insignias de estado', async ({ page }) => {
    await page.goto('/rooms')
    await expect(page.getByRole('heading', { name: 'Habitaciones' })).toBeVisible()

    // La tabla carga habitaciones (BUG-1: antes crasheaba por PageDto).
    await expect(page.getByRole('cell', { name: '101' })).toBeVisible({ timeout: 10000 })

    // Insignia de estado "Disponible" visible (span con clase bg-green-100, no
    // la opción oculta del dropdown de filtro).
    const disponibleBadge = page.locator('span.bg-green-100').first()
    await expect(disponibleBadge).toBeVisible()
    await expect(disponibleBadge).toHaveText('Disponible')
  })

  test('el filtro por tipo de habitación filtra la tabla', async ({ page }) => {
    await page.goto('/rooms')
    await expect(page.getByRole('cell', { name: '101' })).toBeVisible({ timeout: 10000 })

    // El dropdown de tipos se llena desde la respuesta paginada (BUG-1).
    const tipoSelect = page.getByLabel('Tipo')
    await expect(tipoSelect.locator('option')).toContainText(['Doble'])

    // Filtra por tipo Doble: la habitación 101 (Sencilla) deja de aparecer.
    await tipoSelect.selectOption('Doble')
    await expect(page.getByRole('cell', { name: '101' })).toHaveCount(0)
    // Pero una habitación Doble (102) sigue visible.
    await expect(page.getByRole('cell', { name: '102' })).toBeVisible()
  })

  test('el botón "Nueva habitación" está disponible para ADMIN', async ({ page }) => {
    await page.goto('/rooms')
    await expect(page.getByRole('heading', { name: 'Habitaciones' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Nueva habitación' })).toBeVisible()
  })
})
