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
import { useAuth } from '../auth/AuthContext'
import {
  useCreateRoomBlock,
  useReleaseRoomBlock,
  useRoomBlocks,
  useUpdateRoomBlock,
} from '../hooks/useRoomBlocks'
import { useRooms } from '../hooks/useRooms'
import type { NormalizedError } from '../api/types'
import type { RoomBlockDto } from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatDate, formatDateTime, todayISO } from '../utils/format'
import {
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
  BLOCK_TYPES,
  BLOCK_TYPE_LABELS,
} from '../utils/constants'

interface BlockFormState {
  roomId: number
  startDate: string
  endDate: string
  blockType: 'MAINTENANCE' | 'OPERATIONAL'
  reason: string
}

const emptyForm: BlockFormState = {
  roomId: 0,
  startDate: todayISO(),
  endDate: todayISO(),
  blockType: 'MAINTENANCE',
  reason: '',
}

export function RoomBlocksPage() {
  const { user } = useAuth()
  const toast = useToast()
  const canEdit = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<RoomBlockDto | null>(null)
  const [form, setForm] = useState<BlockFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [releaseTarget, setReleaseTarget] = useState<RoomBlockDto | null>(null)

  const { data, isLoading, isError, error, refetch, isFetching } = useRoomBlocks({ page, size: 20 })
  const roomsQ = useRooms({ size: 100 })
  const createMut = useCreateRoomBlock()
  const updateMut = useUpdateRoomBlock()
  const releaseMut = useReleaseRoomBlock()

  const rooms = roomsQ.data?.content ?? []
  const roomNumber = (id: number) =>
    rooms.find((r) => r.id === id)?.number ?? `#${id}`

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm, roomId: rooms[0]?.id ?? 0 })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const openEdit = (b: RoomBlockDto) => {
    setEditing(b)
    setForm({
      roomId: b.roomId,
      startDate: b.startDate,
      endDate: b.endDate,
      blockType: b.blockType,
      reason: b.reason ?? '',
    })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    const payload = {
      roomId: form.roomId,
      startDate: form.startDate,
      endDate: form.endDate,
      blockType: form.blockType,
      reason: form.reason.trim() || null,
    }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Bloqueo actualizado.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Bloqueo creado.')
      }
      setFormOpen(false)
    } catch (err) {
      setFieldErrors(getFieldErrors(err))
      setSubmitError((err as NormalizedError).message)
    }
  }

  const applyRelease = async () => {
    if (!releaseTarget) return
    try {
      await releaseMut.mutateAsync(releaseTarget.id)
      toast.success('Bloqueo liberado.')
      setReleaseTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const columns: Column<RoomBlockDto>[] = [
    { key: 'room', header: 'Habitación', render: (b) => roomNumber(b.roomId) },
    { key: 'type', header: 'Tipo', render: (b) => BLOCK_TYPE_LABELS[b.blockType] },
    {
      key: 'range',
      header: 'Rango',
      render: (b) => `${formatDate(b.startDate)} → ${formatDate(b.endDate)}`,
    },
    { key: 'reason', header: 'Motivo', render: (b) => b.reason ?? '—' },
    {
      key: 'released',
      header: 'Liberado',
      render: (b) =>
        b.releasedAt ? (
          <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
            {formatDateTime(b.releasedAt)}
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full bg-yellow-100 px-2.5 py-0.5 text-xs font-medium text-yellow-800">
            Activo
          </span>
        ),
    },
    {
      key: 'actions',
      header: 'Acciones',
      render: (b) => (
        <div className="flex flex-wrap gap-2">
          {canEdit && (
            <>
              <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(b)}>
                Editar
              </button>
              {!b.releasedAt && (
                <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => setReleaseTarget(b)}>
                  Liberar
                </button>
              )}
            </>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Bloqueos de habitaciones"
        subtitle="Bloqueos operativos y de mantenimiento por rango de fechas"
        actions={
          <RoleGate roles={['ADMIN', 'MANAGER']}>
            <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
              Nuevo bloqueo
            </button>
          </RoleGate>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin bloqueos" message="No hay bloqueos de habitaciones registrados." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(b) => b.id}
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
        title={editing ? 'Editar bloqueo' : 'Nuevo bloqueo de habitación'}
        onClose={() => setFormOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <div className={FORM_ROW}>
            <Select
              label="Habitación"
              name="roomId"
              value={form.roomId}
              onChange={(e) => setForm({ ...form, roomId: Number(e.target.value) })}
              error={fieldErrors.roomId}
              required
            >
              <option value="">Seleccione…</option>
              {rooms.map((r) => (
                <option key={r.id} value={r.id}>Hab. {r.number} · {r.roomTypeName}</option>
              ))}
            </Select>
            <Select
              label="Tipo de bloqueo"
              name="blockType"
              value={form.blockType}
              onChange={(e) => setForm({ ...form, blockType: e.target.value as BlockFormState['blockType'] })}
            >
              {BLOCK_TYPES.map((t) => (
                <option key={t} value={t}>{BLOCK_TYPE_LABELS[t]}</option>
              ))}
            </Select>
            <Input
              label="Fecha inicial"
              name="startDate"
              type="date"
              value={form.startDate}
              onChange={(e) => setForm({ ...form, startDate: e.target.value })}
              error={fieldErrors.startDate}
              required
            />
            <Input
              label="Fecha final"
              name="endDate"
              type="date"
              value={form.endDate}
              min={form.startDate}
              onChange={(e) => setForm({ ...form, endDate: e.target.value })}
              error={fieldErrors.endDate}
              required
            />
          </div>
          <TextArea
            label="Motivo"
            name="reason"
            rows={2}
            value={form.reason}
            onChange={(e) => setForm({ ...form, reason: e.target.value })}
            error={fieldErrors.reason}
          />
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending || updateMut.isPending} />
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!releaseTarget}
        title="Liberar bloqueo"
        message={`¿Liberar el bloqueo de la hab. ${releaseTarget ? roomNumber(releaseTarget.roomId) : ''}? La habitación volverá a estar disponible.`}
        confirmLabel="Liberar"
        loading={releaseMut.isPending}
        onConfirm={applyRelease}
        onCancel={() => setReleaseTarget(null)}
      />
    </div>
  )
}
