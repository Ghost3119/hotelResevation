import { test, expect } from '@playwright/test'
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  backendAvailable,
  loginAsAdmin,
  skipIfNoBackend,
} from './helpers'

let backendOk = false

test.beforeAll(async () => {
  backendOk = await backendAvailable()
})

test.beforeEach(async () => {
  skipIfNoBackend(backendOk)
})

test.describe('Authentication', () => {
  test('login redirects to the dashboard', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel(/Correo electr/i).fill(ADMIN_EMAIL)
    await page.getByLabel(/Contrase/i).fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: /Entrar/ }).click()

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible()
  })

  test('dashboard shows the main KPIs', async ({ page }) => {
    await loginAsAdmin(page)
    await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible()

    await expect(page.getByText('Llegadas hoy')).toBeVisible()
    await expect(page.getByText('Salidas hoy')).toBeVisible()
    await expect(page.getByText('Habitaciones ocupadas')).toBeVisible()
    await expect(page.getByText('Habitaciones disponibles')).toBeVisible()
    await expect(page.getByText(/Ocupaci/, { exact: true })).toBeVisible()
    await expect(page.getByText('Ingresos del periodo')).toBeVisible()
  })

  test('logout redirects to login', async ({ page }) => {
    await loginAsAdmin(page)
    await expect(page.getByRole('heading', { name: 'Panel' })).toBeVisible()

    await page.getByRole('button', { name: /Cerrar sesi/ }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Hotel Manager' })).toBeVisible()
  })
})
