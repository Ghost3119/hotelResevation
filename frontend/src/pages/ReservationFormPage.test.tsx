import { describe, it, expect } from 'vitest'
import { screen, fireEvent } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { render } from '../test/render'
import App from '../App'
import { server } from '../test/server'
import { API } from '../test/handlers'

function futureISO(daysAhead: number): string {
  const d = new Date()
  d.setDate(d.getDate() + daysAhead)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function selectGuest(user: { click: (el: HTMLElement) => Promise<void> }) {
  const guestInput = screen.getByLabelText('Huésped')
  fireEvent.change(guestInput, { target: { value: 'John' } })
  const guestBtn = await screen.findByText('John Doe', { exact: false })
  await user.click(guestBtn)
}

describe('ReservationFormPage', () => {
  it('rechaza fechas inválidas en el cliente', async () => {
    const { user } = render(<App />, { route: '/reservations/new', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('Nueva reserva')).toBeInTheDocument()

    const sameDate = futureISO(5)
    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: sameDate } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: sameDate } })

    await user.click(screen.getByRole('button', { name: 'Crear reserva' }))

    expect(await screen.findByText('La salida debe ser posterior a la entrada.')).toBeInTheDocument()
  })

  it('crea una reserva válida y navega al detalle', async () => {
    const { user } = render(<App />, { route: '/reservations/new', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('Nueva reserva')).toBeInTheDocument()

    await selectGuest(user)

    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: futureISO(10) } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: futureISO(12) } })

    const roomTypeSelect = screen.getByLabelText('Tipo de habitación')
    await user.selectOptions(roomTypeSelect, '1')

    await user.click(screen.getByRole('button', { name: 'Crear reserva' }))

    expect(await screen.findByText('Reserva creada.')).toBeInTheDocument()
  })

  it('muestra error de solapamiento desde el servidor', async () => {
    server.use(
      http.post(`${API}/reservations`, () =>
        HttpResponse.json(
          { timestamp: '', status: 409, error: '', code: 'RESERVATION_OVERLAP', message: 'La habitación no está disponible para esas fechas.', path: '', fieldErrors: [] },
          { status: 409 },
        ),
      ),
    )

    const { user } = render(<App />, { route: '/reservations/new', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('Nueva reserva')).toBeInTheDocument()

    await selectGuest(user)

    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: futureISO(20) } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: futureISO(22) } })
    await user.selectOptions(screen.getByLabelText('Tipo de habitación'), '1')

    await user.click(screen.getByRole('button', { name: 'Crear reserva' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('La habitación no está disponible para esas fechas.')
  })
})
