import { test, expect } from '@playwright/test'
import { backendAvailable, loginAsAdmin, skipIfNoBackend } from './helpers'

let backendOk = false

test.beforeAll(async () => {
  backendOk = await backendAvailable()
})

test.beforeEach(async () => {
  skipIfNoBackend(backendOk)
})

test.describe('Autenticación', () => {
  test('inicio de sesión redirige al dashboard', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Correo electrónico').fill('admin@hotel.test')
    await page.getByLabel('Contraseña').fill('admin123')
    await page.getByRole('button', { name: /Entrar/ }).click()

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible()
  })

  test('el dashboard muestra los KPI principales', async ({ page }) => {
    await loginAsAdmin(page)
    await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible()

    await expect(page.getByText('Llegadas hoy')).toBeVisible()
    await expect(page.getByText('Salidas hoy')).toBeVisible()
    await expect(page.getByText('Habitaciones ocupadas')).toBeVisible()
    await expect(page.getByText('Habitaciones disponibles')).toBeVisible()
    await expect(page.getByText('Ocupación')).toBeVisible()
    await expect(page.getByText('Ingresos del periodo')).toBeVisible()
  })

  test('cerrar sesión redirige al login', async ({ page }) => {
    await loginAsAdmin(page)
    await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible()

    await page.getByRole('button', { name: 'Cerrar sesión' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Hotel Manager' })).toBeVisible()
  })
})
