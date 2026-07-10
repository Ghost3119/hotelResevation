import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { render } from '../test/render'
import App from '../App'
import { server } from '../test/server'
import { API } from '../test/handlers'
import { getAccessToken } from './tokenStore'

const NOW = '2026-06-17T10:00:00'

const unauthorizedBody = {
  timestamp: '',
  status: 401,
  error: '',
  code: 'UNAUTHORIZED',
  message: 'Token expirado.',
  path: '',
  fieldErrors: [],
}

describe('Refresh token flow', () => {
  it('refresca el access token tras un 401 y reintenta la petición', async () => {
    let guestsCalls = 0
    server.use(
      http.get(`${API}/guests`, () => {
        guestsCalls++
        if (guestsCalls === 1) {
          return HttpResponse.json(unauthorizedBody, { status: 401 })
        }
        return HttpResponse.json({
          content: [
            {
              id: 1,
              firstName: 'John',
              lastName: 'Doe',
              email: 'john@test.com',
              phone: '555-0100',
              documentNumber: 'PASS123',
              nationality: 'USA',
              createdAt: NOW,
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
        })
      }),
    )

    render(<App />, { route: '/guests', authenticated: true, role: 'ADMIN' })

    // La lista de huéspedes aparece tras el refresh + reintento
    expect(await screen.findByText('John Doe')).toBeInTheDocument()
    // El access token se actualiza solo en memoria.
    expect(getAccessToken()).toBe('new-test-token')
    // Se hicieron dos llamadas a /guests (la original 401 + el reintento)
    expect(guestsCalls).toBe(2)
  })

  it('limpia el token y redirige a /login al fallar el refresh (401 en refresh)', async () => {
    // El interceptor hace `window.location.href = '/login'`; jsdom no implementa
    // navegación y emite un error por consola. Lo silenciamos solo para esta
    // prueba para mantener la salida limpia (es informativo, no un fallo).
    const originalError = console.error
    console.error = (...args: unknown[]) => {
      if (args.some((a) => String(a).includes('Not implemented: navigation'))) return
      originalError(...(args as never[]))
    }
    try {
      server.use(
        http.get(`${API}/guests`, () =>
          HttpResponse.json(unauthorizedBody, { status: 401 }),
        ),
        http.post(`${API}/auth/refresh`, () =>
          HttpResponse.json(
            { ...unauthorizedBody, message: 'Refresh inválido.' },
            { status: 401 },
          ),
        ),
      )

      render(<App />, { route: '/guests', authenticated: true, role: 'ADMIN' })

      // La sesión se descarta y la aplicación vuelve al login.
      expect(await screen.findByRole('heading', { name: 'Hotel Manager' })).toBeInTheDocument()
      // El access token se elimina de memoria.
      expect(getAccessToken()).toBeNull()
    } finally {
      console.error = originalError
    }
  })
})
