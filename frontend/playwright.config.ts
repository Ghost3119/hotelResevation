/* global process: readonly */
import { defineConfig, devices } from '@playwright/test'

/**
 * Configuración de Playwright para pruebas E2E del Hotel Manager.
 *
 * REQUISITOS PREVIOS:
 * - Levanta el stack con `docker compose up --build` desde la raíz. Las pruebas
 *   llaman al backend por el proxy same-origin `http://localhost:5173/api`; los
 *   puertos internos de backend/Postgres no se publican.
 * - Define ADMIN_EMAIL y ADMIN_PASSWORD con las credenciales bootstrap locales.
 * - El frontend se levanta automáticamente mediante `webServer` (npm run dev)
 *   en http://localhost:5173, salvo que ya esté corriendo (reuseExistingServer
 *   fuera de CI).
 *
 * Si el backend no es alcanzable, las pruebas se omiten (skip) de forma graciosa
 * (ver e2e/helpers.ts) en lugar de fallar.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    actionTimeout: 15000,
    navigationTimeout: 20000,
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 30000,
  },
})
