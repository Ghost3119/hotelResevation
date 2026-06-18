import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Input, TextArea } from '../components/FormField'
import { Amount } from '../components/Amount'
import { useToast } from '../components/Toast'
import { useAuth } from '../auth/AuthContext'
import {
  useCreateRoomType,
  useRoomTypes,
  useSetRoomTypeStatus,
  useUpdateRoomType,
} from '../hooks/useRoomTypes'
import type { NormalizedError, RoomTypeDto } from '../api/types'
import type { RoomTypeWriteDto } from '../api/roomTypes.api'
import { getFieldErrors } from '../utils/error'

const emptyForm: RoomTypeWriteDto = {
  name: '',
  description: '',
  maxCapacity: 2,
  basePrice: 0,
  amenities: [],
  active: true,
}

export function RoomTypesPage() {
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const { data, isLoading, isError, error, refetch } = useRoomTypes({})
  const createMut = useCreateRoomType()
  const updateMut = useUpdateRoomType()
  const setStatusMut = useSetRoomTypeStatus()

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<RoomTypeDto | null>(null)
  const [form, setForm] = useState<RoomTypeWriteDto>(emptyForm)
  const [amenitiesText, setAmenitiesText] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const [toggleTarget, setToggleTarget] = useState<RoomTypeDto | null>(null)

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm })
    setAmenitiesText('')
    setFieldErrors({})
    setSubmitError(null)
    setModalOpen(true)
  }

  const openEdit = (rt: RoomTypeDto) => {
    setEditing(rt)
    setForm({
      name: rt.name,
      description: rt.description ?? '',
      maxCapacity: rt.maxCapacity,
      basePrice: rt.basePrice,
      amenities: rt.amenities,
      active: rt.active,
    })
    setAmenitiesText(rt.amenities.join(', '))
    setFieldErrors({})
    setSubmitError(null)
    setModalOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    const payload: RoomTypeWriteDto = {
      ...form,
      description: form.description?.trim() || null,
      amenities: amenitiesText
        .split(',')
        .map((a) => a.trim())
        .filter(Boolean),
    }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Tipo actualizado.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Tipo creado.')
      }
      setModalOpen(false)
    } catch (err) {
      const normalized = err as NormalizedError
      setFieldErrors(getFieldErrors(err))
      setSubmitError(normalized.message)
    }
  }

  const applyToggle = async () => {
    if (!toggleTarget) return
    try {
      await setStatusMut.mutateAsync({ id: toggleTarget.id, data: { active: !toggleTarget.active } })
      toast.success('Estado actualizado.')
      setToggleTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  return (
    <div>
      <PageHeader
        title="Tipos de habitación"
        subtitle={isAdmin ? 'Catálogo de tipos, capacidades y precios' : 'Catálogo de tipos (solo lectura)'}
        actions={
          isAdmin && (
            <button type="button" className="btn btn-primary" onClick={openCreate}>
              Nuevo tipo
            </button>
          )
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && data.length === 0 && (
        <EmptyState title="Sin tipos" message="No hay tipos de habitación registrados." />
      )}
      {!isLoading && !isError && data && data.length > 0 && (
        <div className="data-table">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Descripción</th>
                <th>Capacidad</th>
                <th className="text-right">Precio base</th>
                <th>Comodidades</th>
                <th>Estado</th>
                {isAdmin && <th>Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {data.map((rt) => (
                <tr key={rt.id}>
                  <td><strong>{rt.name}</strong></td>
                  <td>{rt.description ?? '—'}</td>
                  <td>{rt.maxCapacity}</td>
                  <td className="text-right"><Amount value={rt.basePrice} /></td>
                  <td>{rt.amenities.length ? rt.amenities.join(', ') : '—'}</td>
                  <td>
                    <span className={rt.active ? 'badge badge-green' : 'badge badge-gray'}>
                      {rt.active ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  {isAdmin && (
                    <td>
                      <div className="flex-gap">
                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEdit(rt)}>
                          Editar
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => setToggleTarget(rt)}
                        >
                          {rt.active ? 'Desactivar' : 'Activar'}
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isAdmin && (
        <Modal
          open={modalOpen}
          title={editing ? 'Editar tipo' : 'Nuevo tipo'}
          onClose={() => setModalOpen(false)}
          size="md"
        >
          <form onSubmit={onSubmit} noValidate>
            {submitError && (
              <div className="form-alert form-alert-error" role="alert">{submitError}</div>
            )}
            <Input
              label="Nombre"
              name="name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              error={fieldErrors.name}
              required
            />
            <TextArea
              label="Descripción"
              name="description"
              rows={2}
              value={form.description ?? ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              error={fieldErrors.description}
            />
            <div className="form-row">
              <Input
                label="Capacidad máxima"
                name="maxCapacity"
                type="number"
                min={1}
                value={form.maxCapacity}
                onChange={(e) => setForm({ ...form, maxCapacity: Number(e.target.value) })}
                error={fieldErrors.maxCapacity}
                required
              />
              <Input
                label="Precio base (EUR)"
                name="basePrice"
                type="number"
                step="0.01"
                min={0}
                value={form.basePrice}
                onChange={(e) => setForm({ ...form, basePrice: Number(e.target.value) })}
                error={fieldErrors.basePrice}
                required
              />
            </div>
            <Input
              label="Comodidades (separadas por comas)"
              name="amenities"
              value={amenitiesText}
              onChange={(e) => setAmenitiesText(e.target.value)}
              hint="Ej.: WiFi, TV, Aire acondicionado"
            />
            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalOpen(false)}>
                Cancelar
              </button>
              <ModalSubmitButton loading={createMut.isPending || updateMut.isPending} />
            </div>
          </form>
        </Modal>
      )}

      <ConfirmDialog
        open={!!toggleTarget}
        title={toggleTarget?.active ? 'Desactivar tipo' : 'Activar tipo'}
        message={
          toggleTarget
            ? `¿Confirmar el cambio de estado del tipo "${toggleTarget.name}"?`
            : ''
        }
        confirmLabel="Confirmar"
        loading={setStatusMut.isPending}
        onConfirm={applyToggle}
        onCancel={() => setToggleTarget(null)}
      />
    </div>
  )
}
