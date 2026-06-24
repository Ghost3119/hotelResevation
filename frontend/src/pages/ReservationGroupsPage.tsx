import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Input, Select, TextArea } from '../components/FormField'
import { useToast } from '../components/Toast'
import { RoleGate } from '../auth/RoleGate'
import {
  useCancelReservationGroup,
  useCreateReservationGroup,
  useReservationGroups,
} from '../hooks/useReservationGroups'
import { useGuests } from '../hooks/useGuests'
import type { NormalizedError } from '../api/types'
import type { ReservationGroupDto } from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatDateTime } from '../utils/format'
import {
  BTN_DANGER,
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
  PAGE_SIZE_DEFAULT,
} from '../utils/constants'

interface GroupFormState {
  name: string
  contactGuestId: number | ''
  notes: string
}

const emptyForm: GroupFormState = { name: '', contactGuestId: '', notes: '' }

export function ReservationGroupsPage() {
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<GroupFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [cancelTarget, setCancelTarget] = useState<ReservationGroupDto | null>(null)

  const { data, isLoading, isError, error, refetch, isFetching } = useReservationGroups({
    page,
    size: PAGE_SIZE_DEFAULT,
  })
  const guestsQ = useGuests({ size: 100 })
  const createMut = useCreateReservationGroup()
  const cancelMut = useCancelReservationGroup()

  const guests = guestsQ.data?.content ?? []
  const guestName = (id: number | null) => {
    if (id == null) return '—'
    const g = guests.find((x) => x.id === id)
    return g ? `${g.firstName} ${g.lastName}` : `#${id}`
  }

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
        name: form.name,
        contactGuestId: form.contactGuestId ? Number(form.contactGuestId) : null,
        notes: form.notes.trim() || null,
      })
      toast.success('Grupo creado.')
      setFormOpen(false)
    } catch (err) {
      setFieldErrors(getFieldErrors(err))
      setSubmitError((err as NormalizedError).message)
    }
  }

  const applyCancel = async () => {
    if (!cancelTarget) return
    try {
      const res = await cancelMut.mutateAsync(cancelTarget.id)
      toast.success(`Grupo cancelado: ${res.cancelledCount} reserva(s).`)
      setCancelTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const columns: Column<ReservationGroupDto>[] = [
    { key: 'id', header: 'ID', render: (g) => `#${g.id}` },
    { key: 'name', header: 'Nombre', render: (g) => g.name },
    { key: 'contact', header: 'Contacto', render: (g) => guestName(g.contactGuestId) },
    { key: 'notes', header: 'Notas', render: (g) => g.notes ?? '—' },
    { key: 'createdAt', header: 'Creado', render: (g) => formatDateTime(g.createdAt) },
    {
      key: 'actions',
      header: 'Acciones',
      render: (g) => (
        <div className="flex flex-wrap gap-2">
          <button type="button" className={`${BTN_DANGER} ${BTN_SM}`} onClick={() => setCancelTarget(g)}>
            Cancelar grupo
          </button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Reservas grupales"
        subtitle="Grupos de reservas con cancelación grupal"
        actions={
          <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
            Nuevo grupo
          </button>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin grupos" message="No hay reservas grupales registradas." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(g) => g.id}
            page={data.page}
            size={data.size}
            totalElements={data.totalElements}
            totalPages={data.totalPages}
            onPageChange={setPage}
            empty={isFetching ? 'Actualizando…' : undefined}
          />
        )
      )}

      <Modal
        open={formOpen}
        title="Nuevo grupo de reservas"
        onClose={() => setFormOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <Input
            label="Nombre del grupo"
            name="name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            error={fieldErrors.name}
            required
          />
          <div className={FORM_ROW}>
            <Select
              label="Huésped de contacto"
              name="contactGuestId"
              value={form.contactGuestId}
              onChange={(e) => setForm({ ...form, contactGuestId: e.target.value ? Number(e.target.value) : '' })}
              error={fieldErrors.contactGuestId}
            >
              <option value="">Sin contacto</option>
              {guests.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.firstName} {g.lastName}
                </option>
              ))}
            </Select>
          </div>
          <TextArea
            label="Notas"
            name="notes"
            rows={3}
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending} label="Crear grupo" />
          </div>
        </form>
      </Modal>

      <RoleGate roles={['ADMIN', 'MANAGER']}>
        <ConfirmDialog
          open={!!cancelTarget}
          title="Cancelar grupo"
          message={`¿Cancelar todas las reservas activas del grupo "${cancelTarget?.name ?? ''}"?`}
          confirmLabel="Sí, cancelar grupo"
          destructive
          loading={cancelMut.isPending}
          onConfirm={applyCancel}
          onCancel={() => setCancelTarget(null)}
        />
      </RoleGate>
    </div>
  )
}
