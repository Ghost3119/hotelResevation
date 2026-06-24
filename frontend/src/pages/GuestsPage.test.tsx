import { describe, it, expect } from 'vitest'
import { screen, fireEvent } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

describe('GuestsPage', () => {
  it('lista huéspedes existentes', async () => {
    render(<App />, { route: '/guests', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('John Doe')).toBeInTheDocument()
    // ER-24: document numbers are masked by default (first & last char kept).
    expect(screen.getByText('D••••6')).toBeInTheDocument()
  })

  it('filtra huéspedes por búsqueda', async () => {
    render(<App />, { route: '/guests', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('John Doe')).toBeInTheDocument()

    const search = screen.getByLabelText('Buscar por nombre, correo o documento')
    fireEvent.change(search, { target: { value: 'Jane' } })

    expect(await screen.findByText('Jane Smith', { exact: false })).toBeInTheDocument()
  })

  it('crea un huésped nuevo', async () => {
    const { user } = render(<App />, { route: '/guests', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByText('John Doe')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Nuevo huésped' }))
    expect(await screen.findByRole('heading', { name: 'Nuevo huésped' })).toBeInTheDocument()

    await user.type(screen.getByLabelText('Nombre'), 'Carlos')
    await user.type(screen.getByLabelText('Apellidos'), 'Ruiz')
    await user.type(screen.getByLabelText('Documento de identidad'), 'CAR999')
    await user.type(screen.getByLabelText('Nacionalidad'), 'Colombia')

    await user.click(screen.getByRole('button', { name: 'Crear huésped' }))

    expect(await screen.findByText('Huésped creado.')).toBeInTheDocument()
  })
})
