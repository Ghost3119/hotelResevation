import { describe, it, expect } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

describe('RoomsPage', () => {
  it('rendera la lista de habitaciones sin crashear (BUG-1)', async () => {
    render(<App />, { route: '/rooms', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Habitaciones' })).toBeInTheDocument()
    // La tabla carga habitaciones (antes crasheaba por .filter/.map sobre PageDto).
    expect(await screen.findByText('101')).toBeInTheDocument()
    expect(screen.getByText('102')).toBeInTheDocument()
    expect(screen.getByText('201')).toBeInTheDocument()
  })

  it('puebla el dropdown de tipos desde la respuesta paginada (BUG-1)', async () => {
    render(<App />, { route: '/rooms', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Habitaciones' })).toBeInTheDocument()

    const select = await screen.findByLabelText('Tipo')
    await waitFor(() => {
      const labels = within(select).getAllByRole('option').map((o) => o.textContent ?? '')
      expect(labels).toContain('Todos')
      expect(labels).toContain('Doble')
      expect(labels).toContain('Suite')
    })
  })

  it('muestra los datos correctos en la tabla de habitaciones', async () => {
    render(<App />, { route: '/rooms', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Habitaciones' })).toBeInTheDocument()

    // Fila de la habitación 101 → Doble, Disponible.
    const cell101 = await screen.findByText('101')
    const row101 = cell101.closest('tr')!
    expect(within(row101).getByText('Doble')).toBeInTheDocument()
    expect(within(row101).getByText('Disponible')).toBeInTheDocument()

    // Fila de la habitación 102 → Doble, Ocupada.
    const cell102 = screen.getByText('102')
    const row102 = cell102.closest('tr')!
    expect(within(row102).getByText('Ocupada')).toBeInTheDocument()

    // Fila de la habitación 201 → Suite.
    const cell201 = screen.getByText('201')
    const row201 = cell201.closest('tr')!
    expect(within(row201).getByText('Suite')).toBeInTheDocument()
  })

  it('rendera las insignias de estado con clases Tailwind correctas', async () => {
    render(<App />, { route: '/rooms', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('101')).toBeInTheDocument()

    // AVAILABLE → bg-green-100 text-green-800 (insignia dentro de la fila 101)
    const row101 = screen.getByText('101').closest('tr')!
    const availableBadge = within(row101).getByText('Disponible')
    expect(availableBadge).toHaveClass('bg-green-100')
    expect(availableBadge).toHaveClass('text-green-800')

    // OCCUPIED → bg-red-100 text-red-800 (insignia dentro de la fila 102)
    const row102 = screen.getByText('102').closest('tr')!
    const occupiedBadge = within(row102).getByText('Ocupada')
    expect(occupiedBadge).toHaveClass('bg-red-100')
    expect(occupiedBadge).toHaveClass('text-red-800')
  })

  it('filtra habitaciones por tipo al seleccionar el dropdown', async () => {
    const { user } = render(<App />, { route: '/rooms', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('201')).toBeInTheDocument()

    const select = screen.getByLabelText('Tipo')
    await user.selectOptions(select, '1') // Doble

    // La habitación 201 (Suite) desaparece al filtrar por tipo Doble.
    await waitFor(() => {
      expect(screen.queryByText('201')).not.toBeInTheDocument()
    })
    expect(screen.getByText('101')).toBeInTheDocument()
    expect(screen.getByText('102')).toBeInTheDocument()
  })

  it('muestra el botón "Nueva habitación" a ADMIN', async () => {
    render(<App />, { route: '/rooms', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('button', { name: 'Nueva habitación' })).toBeInTheDocument()
  })

  it('oculta el botón "Nueva habitación" a RECEPCIONISTA', async () => {
    render(<App />, { route: '/rooms', authenticated: true, role: 'RECEPCIONISTA' })
    // Espera a que la página cargue antes de comprobar la ausencia del botón.
    expect(await screen.findByRole('heading', { name: 'Habitaciones' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nueva habitación' })).not.toBeInTheDocument()
  })
})
