import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { Input, Select, TextArea } from '../components/FormField'
import { RoomStatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/Toast'
import { useAuth } from '../auth/AuthContext'
import { RoleGate } from '../auth/RoleGate'
import { useDebounce } from '../hooks/useDebounce'
import {
  useCreateRoom,
  useRooms,
  useSetRoomObservations,
  useSetRoomStatus,
  useUpdateRoom,
} from '../hooks/useRooms'
import { useRoomTypes } from '../hooks/useRoomTypes'
import type { NormalizedError, RoomDto, RoomStatus } from '../api/types'
import type { RoomWriteDto } from '../api/rooms.api'
import { getFieldErrors } from '../utils/error'
import {
  PAGE_SIZE_DEFAULT,
  ROOM_STATUSES,
  ROOM_STATUS_LABELS,
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
} from '../utils/constants'

type RoomFormState = RoomWriteDto

const emptyRoom: RoomFormState = {
  number: '',
  floor: 1,
  roomTypeId: 0,
  status: 'AVAILABLE',
  observations: '',
}

export function RoomsPage() {
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const [floor, setFloor] = useState('')
  const [roomTypeId, setRoomTypeId] = useState<number | ''>('')
  const [statusFilter, setStatusFilter] = useState<'' | RoomStatus>('')
  const [page, setPage] = useState(0)
  const size = PAGE_SIZE_DEFAULT

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<RoomDto | null>(null)
  const [form, setForm] = useState<RoomFormState>(emptyRoom)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const [statusTarget, setStatusTarget] = useState<RoomDto | null>(null)
  const [newStatus, setNewStatus] = useState<RoomStatus>('AVAILABLE')

  const [obsTarget, setObsTarget] = useState<RoomDto | null>(null)
  const [obsValue, setObsValue] = useState('')

  const debouncedFloor = useDebounce(floor, 200)
  const roomTypesQ = useRoomTypes({})
  const params = {
    page,
    size,
    floor: debouncedFloor ? Number(debouncedFloor) : undefined,
    roomTypeId: roomTypeId || undefined,
    status: statusFilter || undefined,
  }
  const { data, isLoading, isError, error, refetch, isFetching } = useRooms(params)

  const createMut = useCreateRoom()
  const updateMut = useUpdateRoom()
  const setStatusMut = useSetRoomStatus()
  const setObsMut = useSetRoomObservations()

  const roomTypes = roomTypesQ.data ?? []

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyRoom, roomTypeId: roomTypes[0]?.id ?? 0 })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const openEdit = (room: RoomDto) => {
    setEditing(room)
    setForm({
      number: room.number,
      floor: room.floor,
      roomTypeId: room.roomTypeId,
      status: room.status,
      observations: room.observations ?? '',
    })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: form })
        toast.success('Habitación actualizada.')
      } else {
        await createMut.mutateAsync(form)
        toast.success('Habitación creada.')
      }
      setFormOpen(false)
    } catch (err) {
      const normalized = err as NormalizedError
      setFieldErrors(getFieldErrors(err))
      setSubmitError(normalized.message)
    }
  }

  const confirmStatus = (room: RoomDto) => {
    setStatusTarget(room)
    setNewStatus(room.status)
  }

  const applyStatus = async () => {
    if (!statusTarget) return
    try {
      await setStatusMut.mutateAsync({ id: statusTarget.id, data: { status: newStatus } })
      toast.success('Estado de habitación actualizado.')
      setStatusTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const openObs = (room: RoomDto) => {
    setObsTarget(room)
    setObsValue(room.observations ?? '')
  }

  const applyObs = async () => {
    if (!obsTarget) return
    try {
      await setObsMut.mutateAsync({ id: obsTarget.id, data: { observations: obsValue.trim() || null } })
      toast.success('Observaciones guardadas.')
      setObsTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const columns: Column<RoomDto>[] = [
    { key: 'number', header: 'Habitación', render: (r) => r.number },
    { key: 'floor', header: 'Piso', render: (r) => r.floor },
    { key: 'roomTypeName', header: 'Tipo', render: (r) => r.roomTypeName },
    { key: 'status', header: 'Estado', render: (r) => <RoomStatusBadge status={r.status} /> },
    { key: 'observations', header: 'Observaciones', render: (r) => r.observations ?? '—' },
    {
      key: 'actions',
      header: 'Acciones',
      render: (r) => (
        <div className="flex flex-wrap gap-2">
          {isAdmin && (
            <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(r)}>
              Editar
            </button>
          )}
          <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => confirmStatus(r)}>
            Cambiar estado
          </button>
          <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openObs(r)}>
            Observaciones
          </button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Habitaciones"
        subtitle="Gestión y estados de habitaciones"
        actions={
          <RoleGate roles={['ADMIN']}>
            <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
              Nueva habitación
            </button>
          </RoleGate>
        }
      />

      <div className="mb-4 flex flex-wrap items-end gap-2.5">
        <Input
          label="Piso"
          name="floor"
          type="number"
          value={floor}
          onChange={(e) => {
            setFloor(e.target.value)
            setPage(0)
          }}
          placeholder="Todos"
        />
        <Select
          label="Tipo"
          name="roomTypeId"
          value={roomTypeId}
          onChange={(e) => {
            setRoomTypeId(e.target.value ? Number(e.target.value) : '')
            setPage(0)
          }}
        >
          <option value="">Todos</option>
          {roomTypes.map((rt) => (
            <option key={rt.id} value={rt.id}>
              {rt.name}
            </option>
          ))}
        </Select>
        <Select
          label="Estado"
          name="status"
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as RoomStatus | '')
            setPage(0)
          }}
        >
          <option value="">Todos</option>
          {ROOM_STATUSES.map((s) => (
            <option key={s} value={s}>
              {ROOM_STATUS_LABELS[s]}
            </option>
          ))}
        </Select>
        {(floor || roomTypeId || statusFilter) && (
          <button
            type="button"
            className={BTN_SECONDARY}
            onClick={() => {
              setFloor('')
              setRoomTypeId('')
              setStatusFilter('')
              setPage(0)
            }}
          >
            Limpiar
          </button>
        )}
      </div>

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin habitaciones" message="No hay habitaciones con los filtros indicados." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(r) => r.id}
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
        title={editing ? 'Editar habitación' : 'Nueva habitación'}
        onClose={() => setFormOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <div className={FORM_ROW}>
            <Input
              label="Número"
              name="number"
              value={form.number}
              onChange={(e) => setForm({ ...form, number: e.target.value })}
              error={fieldErrors.number}
              required
            />
            <Input
              label="Piso"
              name="floor"
              type="number"
              min={0}
              value={form.floor}
              onChange={(e) => setForm({ ...form, floor: Number(e.target.value) })}
              error={fieldErrors.floor}
              required
            />
            <Select
              label="Tipo de habitación"
              name="roomTypeId"
              value={form.roomTypeId}
              onChange={(e) => setForm({ ...form, roomTypeId: Number(e.target.value) })}
              error={fieldErrors.roomTypeId}
              required
            >
              <option value="">Seleccione…</option>
              {roomTypes.map((rt) => (
                <option key={rt.id} value={rt.id}>
                  {rt.name}
                </option>
              ))}
            </Select>
            <Select
              label="Estado"
              name="status"
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value as RoomStatus })}
              error={fieldErrors.status}
              disabled={!isAdmin}
            >
              {ROOM_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {ROOM_STATUS_LABELS[s]}
                </option>
              ))}
            </Select>
          </div>
          <TextArea
            label="Observaciones"
            name="observations"
            rows={2}
            value={form.observations ?? ''}
            onChange={(e) => setForm({ ...form, observations: e.target.value })}
            error={fieldErrors.observations}
          />
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending || updateMut.isPending} />
          </div>
        </form>
      </Modal>

      <Modal
        open={!!statusTarget}
        title="Cambiar estado de habitación"
        onClose={() => setStatusTarget(null)}
        size="sm"
      >
        <p className="text-sm text-slate-500">
          {statusTarget
            ? `Habitación ${statusTarget.number} — estado actual: ${ROOM_STATUS_LABELS[statusTarget.status]}.`
            : ''}
        </p>
        <Select
          label="Nuevo estado"
          name="newStatus"
          value={newStatus}
          onChange={(e) => setNewStatus(e.target.value as RoomStatus)}
        >
          {ROOM_STATUSES.map((s) => (
            <option key={s} value={s}>
              {ROOM_STATUS_LABELS[s]}
            </option>
          ))}
        </Select>
        <div className={FORM_ACTIONS}>
          <button type="button" className={BTN_SECONDARY} onClick={() => setStatusTarget(null)}>
            Cancelar
          </button>
          <button type="button" className={BTN_PRIMARY} onClick={applyStatus} disabled={setStatusMut.isPending}>
            Aplicar
          </button>
        </div>
      </Modal>

      <Modal
        open={!!obsTarget}
        title={`Observaciones — Hab. ${obsTarget?.number ?? ''}`}
        onClose={() => setObsTarget(null)}
        size="md"
      >
        <TextArea
          label="Observaciones"
          name="observations"
          rows={4}
          value={obsValue}
          onChange={(e) => setObsValue(e.target.value)}
        />
        <div className={FORM_ACTIONS}>
          <button type="button" className={BTN_SECONDARY} onClick={() => setObsTarget(null)}>
            Cancelar
          </button>
          <button type="button" className={BTN_PRIMARY} onClick={applyObs} disabled={setObsMut.isPending}>
            Guardar
          </button>
        </div>
      </Modal>
    </div>
  )
}
