import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { render } from '../test/render'
import App from '../App'
import { server } from '../test/server'
import { API } from '../test/handlers'

describe('LoginPage', () => {
  it('inicia sesión y navega al dashboard', async () => {
    const { user } = render(<App />, { route: '/login' })

    const password = screen.getByLabelText('Contraseña')
    await user.type(password, 'admin123')
    await user.click(screen.getByRole('button', { name: /Entrar/ }))

    expect(await screen.findByRole('heading', { name: 'Panel' })).toBeInTheDocument()
    expect(window.localStorage.getItem('hotel.token')).not.toBeNull()
  })

  it('muestra error con credenciales no válidas', async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(
          { timestamp: '', status: 401, error: '', code: 'BAD_CREDENTIALS', message: 'Credenciales no válidas.', path: '', fieldErrors: [] },
          { status: 401 },
        ),
      ),
    )

    const { user } = render(<App />, { route: '/login' })
    await user.type(screen.getByLabelText('Contraseña'), 'wrong')
    await user.click(screen.getByRole('button', { name: /Entrar/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales no válidas.')
  })

  it('requiere autenticación para el dashboard', async () => {
    render(<App />, { route: '/dashboard', authenticated: false })
    expect(await screen.findByRole('heading', { name: 'Hotel Manager' })).toBeInTheDocument()
  })
})
