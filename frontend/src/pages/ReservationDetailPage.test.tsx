import { describe, it, expect } from 'vitest'
import { screen, fireEvent, within } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

describe('ReservationDetailPage', () => {
  it('cancela una reserva confirmada futura', async () => {
    const { user } = render(<App />, { route: '/reservations/1', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('Reserva #1')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Cancelar reserva' }))
    expect(await screen.findByRole('heading', { name: 'Cancelar reserva' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Sí, cancelar' }))

    expect(await screen.findByText('Reserva cancelada.')).toBeInTheDocument()
    expect(screen.getByText('Cancelada')).toBeInTheDocument()
  })

  it('realiza check-in de una reserva confirmada', async () => {
    const { user } = render(<App />, { route: '/reservations/1', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('Reserva #1')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Check-in' }))

    expect(await screen.findByText('Check-in realizado.')).toBeInTheDocument()
    expect(screen.getByText('Check-in realizado')).toBeInTheDocument()
  })

  it('registra un pago y actualiza el saldo', async () => {
    const { user } = render(<App />, { route: '/reservations/1', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('Reserva #1')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Registrar pago' }))
    expect(await screen.findByRole('heading', { name: 'Registrar pago' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Importe (EUR)'), { target: { value: '100' } })
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Registrar pago' }))

    expect(await screen.findByText('Pago registrado.')).toBeInTheDocument()
    expect((await screen.findAllByText(/140,00/)).length).toBeGreaterThan(0)
  })

  it('realiza check-out de una reserva checked-in', async () => {
    const { user } = render(<App />, { route: '/reservations/2', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('Reserva #2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Check-out' }))
    expect(await screen.findByRole('heading', { name: 'Check-out' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Confirmar check-out' }))

    expect(await screen.findByText('Check-out realizado.')).toBeInTheDocument()
    expect(screen.getByText('Check-out realizado')).toBeInTheDocument()
  })
})
