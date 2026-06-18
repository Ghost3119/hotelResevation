import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

describe('DashboardPage', () => {
  it('muestra los KPI e ingresos del periodo', async () => {
    render(<App />, { route: '/dashboard', authenticated: true, role: 'ADMIN' })

    expect(await screen.findByRole('heading', { name: 'Panel' })).toBeInTheDocument()
    expect(await screen.findByText('Llegadas hoy')).toBeInTheDocument()
    expect(screen.getByText('Salidas hoy')).toBeInTheDocument()
    expect(screen.getByText('Habitaciones ocupadas')).toBeInTheDocument()
    expect(screen.getByText('Habitaciones disponibles')).toBeInTheDocument()
    expect(screen.getByText('En limpieza')).toBeInTheDocument()
    expect(screen.getByText('Ocupación')).toBeInTheDocument()
    expect(screen.getByText('Ingresos del periodo')).toBeInTheDocument()
    expect(screen.getByText('Reservas recientes')).toBeInTheDocument()
  })
})
