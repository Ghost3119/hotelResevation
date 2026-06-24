import { describe, it, expect } from 'vitest'
import { screen, fireEvent, within } from '@testing-library/react'
import { render } from '../test/render'
import App from '../App'

describe('RatePlansPage', () => {
  it('ADMIN puede crear un plan tarifario', async () => {
    const { user } = render(<App />, { route: '/rate-plans', authenticated: true, role: 'ADMIN' })
    expect(await screen.findByRole('heading', { name: 'Planes tarifarios' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Nuevo plan' }))
    const dialog = await screen.findByRole('dialog')

    fireEvent.change(within(dialog).getByLabelText('Código'), { target: { value: 'STD' } })
    fireEvent.change(within(dialog).getByLabelText('Nombre'), { target: { value: 'Standard' } })
    await user.selectOptions(within(dialog).getByLabelText('Tipo de habitación'), '1')

    await user.click(within(dialog).getByRole('button', { name: 'Crear plan' }))

    expect(await screen.findByText('Plan tarifario creado.')).toBeInTheDocument()
  })

  it('RECEPCIONISTA no puede acceder a planes tarifarios (403)', async () => {
    render(<App />, { route: '/rate-plans', authenticated: true, role: 'RECEPCIONISTA' })
    expect(await screen.findByRole('heading', { name: '403' })).toBeInTheDocument()
  })

  it('MANAGER puede acceder y ver el botón de creación', async () => {
    render(<App />, { route: '/rate-plans', authenticated: true, role: 'MANAGER' })
    expect(await screen.findByRole('heading', { name: 'Planes tarifarios' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nuevo plan' })).toBeInTheDocument()
  })
})
