import AxeBuilder from '@axe-core/playwright'
import type { Page } from '@playwright/test'
import { expect, loginAsAdmin, backendAvailable, skipIfNoBackend, test } from './helpers'

let backendOk = false

test.beforeAll(async () => {
  backendOk = await backendAvailable()
})

test.beforeEach(async () => {
  skipIfNoBackend(backendOk)
})

async function expectNoAxeViolations(page: Page) {
  const results = await new AxeBuilder({ page })
    .exclude('[data-testid="toast-region"]')
    .analyze()

  expect(results.violations).toEqual([])
}

test('login page has no detectable axe violations', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: 'Hotel Manager' })).toBeVisible()
  await expectNoAxeViolations(page)
})

test('authenticated operational screens have no detectable axe violations', async ({ page }) => {
  await loginAsAdmin(page)

  const routes = [
    ['/dashboard', 'Panel'],
    ['/availability', 'Disponibilidad'],
    ['/rooms', 'Habitaciones'],
    ['/quote', 'Cotizador'],
    ['/rate-plans', /Planes tarifarios/i],
    ['/housekeeping', 'Limpieza'],
  ] as const

  for (const [route, heading] of routes) {
    await page.goto(route)
    await expect(page.getByRole('heading', { name: heading })).toBeVisible({ timeout: 10000 })
    await expectNoAxeViolations(page)
  }
})
