import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

describe('Protección de rutas por rol', () => {
  it('redirige a login sin autenticación', async () => {
    render(<App />, { route: '/dashboard', authenticated: false })
    expect(await screen.findByRole('heading', { name: 'Hotel Manager' })).toBeInTheDocument()
  })

  it('permite a ADMIN ver usuarios', async () => {
    render(<App />, { route: '/users', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Usuarios' })).toBeInTheDocument()
  })

  it('bloquea a RECEPCIONISTA el acceso a usuarios (403)', async () => {
    render(<App />, { route: '/users', authenticated: true, role: 'RECEPCIONISTA' })
    expect(await screen.findByRole('heading', { name: '403' })).toBeInTheDocument()
    expect(screen.getByText(/No tienes permisos/)).toBeInTheDocument()
  })
})
