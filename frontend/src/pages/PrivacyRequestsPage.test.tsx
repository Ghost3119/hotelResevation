import { describe, it, expect } from 'vitest'
import { screen } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

describe('PrivacyRequestsPage', () => {
  it('PRIVACY_OFFICER puede acceder a las solicitudes de privacidad', async () => {
    render(<App />, { route: '/privacy-requests', authenticated: true, role: 'PRIVACY_OFFICER' })
    expect(await screen.findByRole('heading', { name: 'Solicitudes de privacidad' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nueva solicitud' })).toBeInTheDocument()
  })

  it('ADMIN no puede acceder (solo PRIVACY_OFFICER) → 403', async () => {
    render(<App />, { route: '/privacy-requests', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: '403' })).toBeInTheDocument()
  })

  it('RECEPCIONISTA no puede acceder → 403', async () => {
    render(<App />, { route: '/privacy-requests', authenticated: true, role: 'RECEPCIONISTA' })
    expect(await screen.findByRole('heading', { name: '403' })).toBeInTheDocument()
  })
})
