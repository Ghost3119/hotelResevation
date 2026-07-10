import { describe, it, expect } from 'vitest'
import { screen, fireEvent, within } from '@testing-library/react'
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
  it('rendera el formulario de búsqueda sin crashear (BUG-1) y usa clases Tailwind', async () => {
    render(<App />, { route: '/availability', authenticated: true, role: 'ADMIN' })
    // La página carga la cabecera y el formulario (antes crasheaba por .filter
    // sobre un objeto PageDto en lugar de un array).
    expect(await screen.findByRole('heading', { name: 'Disponibilidad' })).toBeInTheDocument()
    expect(screen.getByLabelText('Entrada')).toBeInTheDocument()
    expect(screen.getByLabelText('Salida')).toBeInTheDocument()
    expect(screen.getByLabelText('Huéspedes')).toBeInTheDocument()
    // El botón primario usa la clase Tailwind de botón primario.
    const submit = screen.getByRole('button', { name: 'Buscar' })
    expect(submit).toHaveClass('bg-blue-600')
  })

  it('puebla el dropdown de tipos de habitación desde la respuesta paginada (BUG-1)', async () => {
    render(<App />, { route: '/availability', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Disponibilidad' })).toBeInTheDocument()

    // Espera a que el dropdown se llene desde el PageDto de /room-types.
    const select = await screen.findByLabelText('Tipo de habitación')
    await within(select).findByRole('option', { name: 'Doble' })
    const options = within(select).getAllByRole('option')
    const labels = options.map((o) => o.textContent ?? '')
    expect(labels).toContain('Todos')
    expect(labels).toContain('Doble')
    expect(labels).toContain('Suite')
  })

  it('devuelve habitaciones disponibles al buscar', async () => {
    const { user } = render(<App />, { route: '/availability', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Disponibilidad' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: futureISO(5) } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: futureISO(7) } })
    fireEvent.change(screen.getByLabelText('Huéspedes'), { target: { value: '2' } })

    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Hab. 101', { exact: false })).toBeInTheDocument()
  })

  it('muestra los precios en formato MXN ($X.XX) y no en euros', async () => {
    const { user } = render(<App />, { route: '/availability', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Disponibilidad' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Entrada'), { target: { value: futureISO(5) } })
    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: futureISO(7) } })
    fireEvent.change(screen.getByLabelText('Huéspedes'), { target: { value: '2' } })
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    // La habitación Doble (basePrice 120) muestra "$120.00" por noche.
    expect(await screen.findByText(/\$120\.00/)).toBeInTheDocument()
    // La Suite (basePrice 250) muestra "$250.00".
    expect(screen.getByText(/\$250\.00/)).toBeInTheDocument()
    // Ningún precio debe mostrarse en euros.
    expect(screen.queryByText(/€/)).not.toBeInTheDocument()
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
