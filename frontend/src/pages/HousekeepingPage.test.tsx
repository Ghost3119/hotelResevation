import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { render } from '../test/render'
import { server } from '../test/server'
import { API } from '../test/handlers'
import App from '../App'

const NOW = '2026-06-17T10:00:00'

interface SeededTask {
  id: number
  roomId: number
  roomNumber: string
  status: 'DIRTY' | 'CLEANING' | 'INSPECTED' | 'READY'
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
  assignedTo: number | null
  notes: string | null
  createdAt: string
  updatedAt: string | null
  completedAt: string | null
  createdBy: number | null
}

let tasks: SeededTask[]

describe('HousekeepingPage', () => {
  beforeEach(() => {
    tasks = [
      {
        id: 1,
        roomId: 1,
        roomNumber: '101',
        status: 'DIRTY',
        priority: 'NORMAL',
        assignedTo: null,
        notes: 'Limpieza pendiente',
        createdAt: NOW,
        updatedAt: NOW,
        completedAt: null,
        createdBy: 1,
      },
      {
        id: 2,
        roomId: 2,
        roomNumber: '102',
        status: 'CLEANING',
        priority: 'HIGH',
        assignedTo: 5,
        notes: null,
        createdAt: NOW,
        updatedAt: NOW,
        completedAt: null,
        createdBy: 1,
      },
    ]
    server.use(
      http.get(`${API}/housekeeping-tasks`, () => HttpResponse.json(tasks)),
      http.patch(`${API}/housekeeping-tasks/:id/status`, async ({ params, request }) => {
        const id = Number(params.id)
        const body = (await request.json()) as { status: SeededTask['status']; notes?: string }
        const t = tasks.find((x) => x.id === id)
        if (t) {
          t.status = body.status
          if (body.notes != null) t.notes = body.notes
          t.updatedAt = NOW
          if (body.status === 'READY') t.completedAt = NOW
        }
        return HttpResponse.json(t ?? { id, roomId: 1, roomNumber: '101', status: body.status, priority: 'NORMAL', assignedTo: null, notes: body.notes ?? null, createdAt: NOW, updatedAt: NOW, completedAt: body.status === 'READY' ? NOW : null, createdBy: 1 })
      }),
    )
  })

  it('rendera las tareas de limpieza con insignias de estado', async () => {
    render(<App />, { route: '/housekeeping', authenticated: true, role: 'MANAGER' })
    expect(await screen.findByRole('heading', { name: 'Limpieza' })).toBeInTheDocument()
    expect(await screen.findByText('101')).toBeInTheDocument()
    expect(screen.getByText('102')).toBeInTheDocument()
    // Insignias de estado color-coded (scope dentro de la fila para evitar el dropdown de filtro)
    const row101 = screen.getByText('101').closest('tr')!
    expect(within(row101).getByText('Sucio')).toHaveClass('bg-red-100')
    const row102 = screen.getByText('102').closest('tr')!
    expect(within(row102).getByText('Limpiando')).toHaveClass('bg-yellow-100')
    // Prioridad alta (insignia en la fila 102)
    expect(within(row102).getByText('Alta')).toHaveClass('bg-orange-100')
  })

  it('actualiza el estado de una tarea (DIRTY → INSPECTED)', async () => {
    const { user } = render(<App />, { route: '/housekeeping', authenticated: true, role: 'MANAGER' })
    expect(await screen.findByText('101')).toBeInTheDocument()

    const row101 = screen.getByText('101').closest('tr')!
    await user.click(within(row101).getByRole('button', { name: 'Cambiar estado' }))

    const dialog = await screen.findByRole('dialog')
    await user.selectOptions(within(dialog).getByLabelText('Nuevo estado'), 'INSPECTED')
    await user.click(within(dialog).getByRole('button', { name: 'Aplicar' }))

    expect(await screen.findByText('Estado de la tarea actualizado.')).toBeInTheDocument()
    // La tarea 101 ahora muestra "Inspeccionado" (insignia azul) — requery tras refetch
    await waitFor(() => {
      const row = screen.getByText('101').closest('tr')!
      expect(within(row).getByText('Inspeccionado')).toHaveClass('bg-blue-100')
    })
  })

  it('solo ADMIN/MANAGER/HOUSEKEEPING pueden acceder (RECEP → 403)', async () => {
    render(<App />, { route: '/housekeeping', authenticated: true, role: 'RECEPCIONISTA' })
    expect(await screen.findByRole('heading', { name: '403' })).toBeInTheDocument()
  })
})
