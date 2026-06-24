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

describe('QuotePage', () => {
  it('envía el formulario y muestra el desglose por noche en MXN', async () => {
    const { user } = render(<App />, { route: '/quote', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Cotizador' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: futureISO(5) } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: futureISO(7) } })
    await user.selectOptions(screen.getByLabelText('Tipo de habitación'), '1')
    fireEvent.change(screen.getByLabelText('Adultos'), { target: { value: '2' } })

    await user.click(screen.getByRole('button', { name: 'Cotizar' }))

    // Desglose por noche (cabecera de la sección de resultados)
    expect(await screen.findByText('Desglose por noche')).toBeInTheDocument()
    // Tarifa base $120.00 (Doble) aparece en cada fila de noche
    expect((await screen.findAllByText(/\$120\.00/)).length).toBeGreaterThan(0)
    // Cargo extra adulto $20.00 (una fila por noche)
    expect((await screen.findAllByText(/\$20\.00/)).length).toBeGreaterThan(0)
    // Total general = 2 noches × (120 + 20 + 19.20) = $318.40
    expect(screen.getByTestId('quote-grand-total')).toHaveTextContent(/\$318\.40/)
    // Nunca en euros
    expect(screen.queryByText(/€/)).not.toBeInTheDocument()
  })

  it('rechaza fechas inválidas (salida <= entrada)', async () => {
    const { user } = render(<App />, { route: '/quote', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Cotizador' })).toBeInTheDocument()

    const same = futureISO(5)
    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: same } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: same } })
    await user.click(screen.getByRole('button', { name: 'Cotizar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('posterior a la de entrada')
  })
})
