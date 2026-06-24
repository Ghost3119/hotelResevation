import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { Input, Select, TextArea } from '../components/FormField'
import {
  HousekeepingPriorityBadge,
  HousekeepingStatusBadge,
} from '../components/StatusBadge'
import { useToast } from '../components/Toast'
import { RoleGate } from '../auth/RoleGate'
import {
  useCreateHousekeepingTask,
  useHousekeepingTasks,
  useUpdateHousekeepingStatus,
} from '../hooks/useHousekeeping'
import type { NormalizedError } from '../api/types'
import type { HousekeepingTaskDto, HousekeepingStatus } from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatDateTime } from '../utils/format'
import {
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  CARD,
  CARD_BODY,
  DATA_TABLE,
  FORM_ALERT_ERROR,
  FORM_ACTIONS,
  HOUSEKEEPING_PRIORITIES,
  HOUSEKEEPING_PRIORITY_LABELS,
  HOUSEKEEPING_STATUSES,
  HOUSEKEEPING_STATUS_LABELS,
  TABLE_TD,
  TABLE_TH,
} from '../utils/constants'

interface TaskFormState {
  roomId: number
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
  notes: string
}

const emptyForm: TaskFormState = { roomId: 0, priority: 'NORMAL', notes: '' }

export function HousekeepingPage() {
  const toast = useToast()

  const [statusFilter, setStatusFilter] = useState<'' | HousekeepingStatus>('')
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<TaskFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [statusTarget, setStatusTarget] = useState<HousekeepingTaskDto | null>(null)
  const [newStatus, setNewStatus] = useState<HousekeepingStatus>('DIRTY')
  const [statusNotes, setStatusNotes] = useState('')

  const { data, isLoading, isError, error, refetch, isFetching } = useHousekeepingTasks({
    status: statusFilter || undefined,
  })
  const createMut = useCreateHousekeepingTask()
  const updateStatusMut = useUpdateHousekeepingStatus()

  const openCreate = () => {
    setForm({ ...emptyForm })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    try {
      await createMut.mutateAsync({
        roomId: form.roomId,
        priority: form.priority,
        notes: form.notes.trim() || null,
      })
      toast.success('Tarea de limpieza creada.')
      setFormOpen(false)
    } catch (err) {
      setFieldErrors(getFieldErrors(err))
      setSubmitError((err as NormalizedError).message)
    }
  }

  const openStatus = (task: HousekeepingTaskDto) => {
    setStatusTarget(task)
    setNewStatus(task.status)
    setStatusNotes('')
  }

  const applyStatus = async () => {
    if (!statusTarget) return
    try {
      await updateStatusMut.mutateAsync({
        id: statusTarget.id,
        data: { status: newStatus, notes: statusNotes.trim() || null },
      })
      toast.success('Estado de la tarea actualizado.')
      setStatusTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const tasks = data ?? []

  return (
    <div>
      <PageHeader
        title="Limpieza"
        subtitle="Tareas de housekeeping y flujo de estados de habitaciones"
        actions={
          <RoleGate roles={['ADMIN', 'MANAGER']}>
            <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
              Nueva tarea
            </button>
          </RoleGate>
        }
      />

      <div className="mb-4 flex flex-wrap items-end gap-2.5">
        <Select
          label="Estado"
          name="statusFilter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as HousekeepingStatus | '')}
        >
          <option value="">Todos</option>
          {HOUSEKEEPING_STATUSES.map((s) => (
            <option key={s} value={s}>{HOUSEKEEPING_STATUS_LABELS[s]}</option>
          ))}
        </Select>
        {statusFilter && (
          <button
            type="button"
            className={BTN_SECONDARY}
            onClick={() => setStatusFilter('')}
          >
            Limpiar
          </button>
        )}
      </div>

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && (
        tasks.length === 0 ? (
          <EmptyState
            title="Sin tareas"
            message={isFetching ? 'Actualizando…' : 'No hay tareas de limpieza con el filtro indicado.'}
          />
        ) : (
          <div className={DATA_TABLE}>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse">
                <thead>
                  <tr>
                    <th scope="col" className={TABLE_TH}>Habitación</th>
                    <th scope="col" className={TABLE_TH}>Estado</th>
                    <th scope="col" className={TABLE_TH}>Prioridad</th>
                    <th scope="col" className={TABLE_TH}>Notas</th>
                    <th scope="col" className={TABLE_TH}>Actualizada</th>
                    <th scope="col" className={TABLE_TH}>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.map((t) => (
                    <tr key={t.id}>
                      <td className={TABLE_TD}>{t.roomNumber ?? `#${t.roomId}`}</td>
                      <td className={TABLE_TD}><HousekeepingStatusBadge status={t.status} /></td>
                      <td className={TABLE_TD}><HousekeepingPriorityBadge priority={t.priority} /></td>
                      <td className={TABLE_TD}>{t.notes ?? '—'}</td>
                      <td className={TABLE_TD}>{formatDateTime(t.updatedAt ?? t.createdAt)}</td>
                      <td className={TABLE_TD}>
                        <button
                          type="button"
                          className={`${BTN_SECONDARY} ${BTN_SM}`}
                          onClick={() => openStatus(t)}
                        >
                          Cambiar estado
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )
      )}

      <Modal
        open={formOpen}
        title="Nueva tarea de limpieza"
        onClose={() => setFormOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <Input
            label="ID de habitación"
            name="roomId"
            type="number"
            min={1}
            value={form.roomId || ''}
            onChange={(e) => setForm({ ...form, roomId: Number(e.target.value) })}
            error={fieldErrors.roomId}
            required
          />
          <Select
            label="Prioridad"
            name="priority"
            value={form.priority}
            onChange={(e) => setForm({ ...form, priority: e.target.value as TaskFormState['priority'] })}
          >
            {HOUSEKEEPING_PRIORITIES.map((p) => (
              <option key={p} value={p}>{HOUSEKEEPING_PRIORITY_LABELS[p]}</option>
            ))}
          </Select>
          <TextArea
            label="Notas"
            name="notes"
            rows={3}
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
            error={fieldErrors.notes}
          />
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending} label="Crear tarea" />
          </div>
        </form>
      </Modal>

      <Modal
        open={!!statusTarget}
        title={`Actualizar estado — Hab. ${statusTarget?.roomNumber ?? ''}`}
        onClose={() => setStatusTarget(null)}
        size="sm"
      >
        <Select
          label="Nuevo estado"
          name="newStatus"
          value={newStatus}
          onChange={(e) => setNewStatus(e.target.value as HousekeepingStatus)}
        >
          {HOUSEKEEPING_STATUSES.map((s) => (
            <option key={s} value={s}>{HOUSEKEEPING_STATUS_LABELS[s]}</option>
          ))}
        </Select>
        <TextArea
          label="Notas (opcional)"
          name="statusNotes"
          rows={2}
          value={statusNotes}
          onChange={(e) => setStatusNotes(e.target.value)}
        />
        <div className={FORM_ACTIONS}>
          <button type="button" className={BTN_SECONDARY} onClick={() => setStatusTarget(null)}>
            Cancelar
          </button>
          <button
            type="button"
            className={BTN_PRIMARY}
            onClick={applyStatus}
            disabled={updateStatusMut.isPending}
          >
            {updateStatusMut.isPending ? 'Guardando…' : 'Aplicar'}
          </button>
        </div>
      </Modal>

      <section className={`mt-5 ${CARD} ${CARD_BODY}`}>
        <h2 className="mb-1 text-base font-semibold text-slate-900">Flujo de estados</h2>
        <p className="text-sm text-slate-500">
          DIRTY → CLEANING → INSPECTED → READY. Cuando una tarea pasa a READY, la habitación
          vuelve a estar disponible.
        </p>
      </section>
    </div>
  )
}
