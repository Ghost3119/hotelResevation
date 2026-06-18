import { describe, it, expect } from 'vitest'
import { screen, fireEvent } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

function futureISO(daysAhead: number): string {
  const d = new Date()
  d.setDate(d.getDate() + daysAhead)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

describe('AvailabilityPage', () => {
  it('devuelve habitaciones disponibles al buscar', async () => {
    const { user } = render(<App />, { route: '/availability', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Disponibilidad' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: futureISO(5) } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: futureISO(7) } })
    fireEvent.change(screen.getByLabelText('Huéspedes'), { target: { value: '2' } })

    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Hab. 101', { exact: false })).toBeInTheDocument()
  })

  it('rechaza fechas inválidas (salida <= entrada)', async () => {
    const { user } = render(<App />, { route: '/availability', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Disponibilidad' })).toBeInTheDocument()

    const sameDate = futureISO(5)
    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: sameDate } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: sameDate } })
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('posterior a la de entrada')
  })
})
