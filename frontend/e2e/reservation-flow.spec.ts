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

test.describe('Flujo completo de reserva', () => {
  test('crea reserva, registra pago, check-in y check-out', async ({ page }) => {
    // --- 1. Crea un huésped determinista (documento único) ---------------------
    const stamp = Date.now()
    const doc = `E2E-${stamp}`
    const firstName = `E2e${stamp}`
    const lastName = 'Flujo'

    await page.goto('/guests')
    await expect(page.getByRole('heading', { name: 'Huéspedes' })).toBeVisible()
    await page.getByRole('button', { name: 'Nuevo huésped' }).click()
    await expect(page.getByRole('heading', { name: 'Nuevo huésped' })).toBeVisible()

    await page.getByLabel('Nombre', { exact: true }).fill(firstName)
    await page.getByLabel('Apellidos', { exact: true }).fill(lastName)
    await page.getByLabel('Documento de identidad', { exact: true }).fill(doc)
    await page.getByLabel('Nacionalidad', { exact: true }).fill('Mexico')
    await page.getByRole('button', { name: 'Crear huésped' }).click()
    await expect(page.getByText('Huésped creado.')).toBeVisible()

    // --- 2. Crea una reserva para ese huésped (tipo Suite, entrada = hoy) -----
    // Nota: el backend solo permite check-in dentro del rango de fechas de la
    // reserva, por eso la entrada es hoy (futureISO(0)). Se asigna una habitación
    // al crear para que el estado nazca CONFIRMED (requisito del check-in).
    await page.goto('/reservations/new')
    await expect(page.getByRole('heading', { name: 'Nueva reserva' })).toBeVisible()

    await page.getByLabel('Huésped').fill(firstName)
    await page.getByRole('button').filter({ hasText: `${firstName} ${lastName}` }).first().click()
    await expect(page.getByText(/Seleccionado:/)).toBeVisible()

    await page.getByLabel('Entrada').fill(futureISO(0))
    await page.getByLabel('Salida').fill(futureISO(2))
    await page.getByLabel('Adultos').fill('2')

    const roomTypeSelect = page.getByLabel('Tipo de habitación')
    const suiteValue = await roomTypeSelect.locator('option').filter({ hasText: 'Suite' }).first().getAttribute('value')
    expect(suiteValue).not.toBeNull()
    await roomTypeSelect.selectOption(suiteValue!)

    // Selecciona una habitación disponible para que la reserva nazca CONFIRMED
    // (el backend deja PENDING sin habitación asignada; el check-in exige
    // CONFIRMED). El dropdown de habitación se llena vía /availability.
    const roomSelect = page.getByLabel(/Habitación \(opcional/)
    await expect(roomSelect).toBeVisible()
    await expect(roomSelect.locator('option').filter({ hasText: 'Hab.' })).not.toHaveCount(0)
    await roomSelect.selectOption({ index: 1 })

    await page.getByRole('button', { name: 'Crear reserva' }).click()

    // Navega al detalle de la reserva creada.
    await page.waitForURL(/\/reservations\/\d+$/)
    await expect(page.getByRole('heading', { name: /Reserva #\d+/ })).toBeVisible()
    // Estado inicial: Confirmada (se asignó habitación al crear).
    await expect(page.getByText('Confirmada', { exact: true })).toBeVisible()

    // --- 3. Registra un pago por el saldo pendiente ----------------------------
    await page.getByRole('button', { name: 'Registrar pago' }).first().click()
    const dialog = page.getByRole('dialog')
    await expect(dialog.getByRole('heading', { name: 'Registrar pago' })).toBeVisible()

    // Lee el saldo pendiente mostrado en el modal y paga exactamente eso.
    const dialogText = (await dialog.textContent()) ?? ''
    const balanceMatch = dialogText.match(/Saldo pendiente\s*\$([\d,.]+)/)
    const balance = balanceMatch ? balanceMatch[1].replace(/,/g, '') : '9000'
    await dialog.getByLabel('Importe (MXN)').fill(balance)
    await dialog.getByLabel('Método de pago').selectOption({ label: 'Efectivo' })
    await dialog.getByRole('button', { name: 'Registrar pago' }).click()

    await expect(page.getByText('Pago registrado.')).toBeVisible()
    // El saldo queda en $0.00.
    await expect(page.getByText('$0.00')).toBeVisible()

    // --- 4. Check-in -----------------------------------------------------------
    await page.getByRole('button', { name: 'Check-in' }).click()
    await expect(page.getByText('Check-in realizado.')).toBeVisible()
    await expect(page.getByText('Check-in realizado', { exact: true })).toBeVisible()

    // --- 5. Check-out (con confirmación) --------------------------------------
    await page.getByRole('button', { name: 'Check-out' }).click()
    await expect(page.getByRole('heading', { name: 'Check-out' })).toBeVisible()
    await page.getByRole('button', { name: 'Confirmar check-out' }).click()

    await expect(page.getByText('Check-out realizado.')).toBeVisible()
    await expect(page.getByText('Check-out realizado', { exact: true })).toBeVisible()
  })
})
