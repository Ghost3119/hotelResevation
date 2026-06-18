/* global process: readonly */
import { defineConfig, devices } from '@playwright/test'

/**
 * Configuración de Playwright para pruebas E2E del Hotel Manager.
 *
 * REQUISITOS PREVIOS:
 * - El backend debe estar corriendo en http://localhost:8080 (levántelo con
 *   `docker compose up -d postgres backend` desde la raíz del proyecto, o
 *   manualmente con Maven + Postgres). Las pruebas E2E hacen llamadas reales
 *   al API (sin MSW) y necesitan backend + Postgres.
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
