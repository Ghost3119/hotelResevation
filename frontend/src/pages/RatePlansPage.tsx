import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Input, Select } from '../components/FormField'
import { useToast } from '../components/Toast'
import { RoleGate } from '../auth/RoleGate'
import { useAuth } from '../auth/AuthContext'
import {
  useCreateRatePlan,
  useRatePlans,
  useSetRatePlanActive,
  useUpdateRatePlan,
} from '../hooks/useRatePlans'
import { useRoomTypes } from '../hooks/useRoomTypes'
import { useCancellationPolicies } from '../hooks/useCancellationPolicies'
import type { NormalizedError } from '../api/types'
import type { RatePlanDto } from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatCurrency, formatDate, todayISO } from '../utils/format'
import {
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
  WEEKDAY_LABELS,
} from '../utils/constants'

interface RatePlanFormState {
  code: string
  name: string
  roomTypeId: number
  weekdayRates: number[]
  adultExtraRate: number
  childExtraRate: number
  cancellationPolicyId: number | ''
  minNights: number
  maxNights: number | ''
  isDefault: boolean
  active: boolean
  validFrom: string
  validTo: string
}

const emptyForm: RatePlanFormState = {
  code: '',
  name: '',
  roomTypeId: 0,
  weekdayRates: [0, 0, 0, 0, 0, 0, 0],
  adultExtraRate: 0,
  childExtraRate: 0,
  cancellationPolicyId: '',
  minNights: 1,
  maxNights: '',
  isDefault: false,
  active: true,
  validFrom: todayISO(),
  validTo: '',
}

export function RatePlansPage() {
  const { user } = useAuth()
  const toast = useToast()
  const canEdit = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<RatePlanDto | null>(null)
  const [form, setForm] = useState<RatePlanFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [toggleTarget, setToggleTarget] = useState<RatePlanDto | null>(null)

  const { data, isLoading, isError, error, refetch, isFetching } = useRatePlans({ page, size: 20 })
  const roomTypesQ = useRoomTypes({})
  const policiesQ = useCancellationPolicies()

  const createMut = useCreateRatePlan()
  const updateMut = useUpdateRatePlan()
  const setActiveMut = useSetRatePlanActive()

  const roomTypes = roomTypesQ.data ?? []
  const roomTypeName = (id: number) =>
    roomTypes.find((rt) => rt.id === id)?.name ?? `#${id}`

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm, roomTypeId: roomTypes[0]?.id ?? 0 })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const openEdit = (rp: RatePlanDto) => {
    setEditing(rp)
    setForm({
      code: rp.code,
      name: rp.name,
      roomTypeId: rp.roomTypeId,
      weekdayRates: [...rp.weekdayRates],
      adultExtraRate: rp.adultExtraRate,
      childExtraRate: rp.childExtraRate,
      cancellationPolicyId: rp.cancellationPolicyId ?? '',
      minNights: rp.minNights,
      maxNights: rp.maxNights ?? '',
      isDefault: rp.isDefault,
      active: rp.active,
      validFrom: rp.validFrom,
      validTo: rp.validTo ?? '',
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
      code: form.code,
      name: form.name,
      roomTypeId: form.roomTypeId,
      weekdayRates: form.weekdayRates,
      adultExtraRate: form.adultExtraRate,
      childExtraRate: form.childExtraRate,
      cancellationPolicyId: form.cancellationPolicyId ? Number(form.cancellationPolicyId) : null,
      minNights: form.minNights,
      maxNights: form.maxNights ? Number(form.maxNights) : null,
      isDefault: form.isDefault,
      active: form.active,
      validFrom: form.validFrom,
      validTo: form.validTo || null,
    }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Plan tarifario actualizado.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Plan tarifario creado.')
      }
      setFormOpen(false)
    } catch (err) {
      setFieldErrors(getFieldErrors(err))
      setSubmitError((err as NormalizedError).message)
    }
  }

  const confirmToggle = (rp: RatePlanDto) => setToggleTarget(rp)

  const applyToggle = async () => {
    if (!toggleTarget) return
    try {
      await setActiveMut.mutateAsync({ id: toggleTarget.id, data: { active: !toggleTarget.active } })
      toast.success('Estado del plan actualizado.')
      setToggleTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const columns: Column<RatePlanDto>[] = [
    { key: 'code', header: 'Código', render: (rp) => rp.code },
    { key: 'name', header: 'Nombre', render: (rp) => rp.name },
    { key: 'roomType', header: 'Tipo', render: (rp) => roomTypeName(rp.roomTypeId) },
    {
      key: 'default',
      header: 'Por defecto',
      render: (rp) => (rp.isDefault ? 'Sí' : 'No'),
    },
    {
      key: 'rates',
      header: 'Tarifa mín.',
      render: (rp) => formatCurrency(Math.min(...rp.weekdayRates)),
    },
    {
      key: 'validity',
      header: 'Vigencia',
      render: (rp) => `${formatDate(rp.validFrom)} → ${formatDate(rp.validTo)}`,
    },
    {
      key: 'active',
      header: 'Estado',
      render: (rp) =>
        rp.active ? (
          <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
            Activo
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full bg-gray-200 px-2.5 py-0.5 text-xs font-medium text-gray-800">
            Inactivo
          </span>
        ),
    },
    {
      key: 'actions',
      header: 'Acciones',
      render: (rp) => (
        <div className="flex flex-wrap gap-2">
          {canEdit && (
            <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(rp)}>
              Editar
            </button>
          )}
          <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => confirmToggle(rp)}>
            {rp.active ? 'Desactivar' : 'Activar'}
          </button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Planes tarifarios"
        subtitle="Planes de precios por noche y tipo de habitación"
        actions={
          <RoleGate roles={['ADMIN', 'MANAGER']}>
            <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
              Nuevo plan
            </button>
          </RoleGate>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin planes tarifarios" message="No hay planes configurados todavía." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(rp) => rp.id}
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
        title={editing ? 'Editar plan tarifario' : 'Nuevo plan tarifario'}
        onClose={() => setFormOpen(false)}
        size="lg"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <div className={FORM_ROW}>
            <Input
              label="Código"
              name="code"
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              error={fieldErrors.code}
              required
            />
            <Input
              label="Nombre"
              name="name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              error={fieldErrors.name}
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
                <option key={rt.id} value={rt.id}>{rt.name}</option>
              ))}
            </Select>
            <Select
              label="Política de cancelación"
              name="cancellationPolicyId"
              value={form.cancellationPolicyId}
              onChange={(e) => setForm({ ...form, cancellationPolicyId: e.target.value ? Number(e.target.value) : '' })}
              error={fieldErrors.cancellationPolicyId}
            >
              <option value="">Sin política</option>
              {(policiesQ.data ?? []).map((p) => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </Select>
          </div>

          <fieldset className="mb-3.5">
            <legend className="mb-1 text-sm font-medium text-slate-600">Tarifas por día de la semana (MXN)</legend>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
              {WEEKDAY_LABELS.map((day, i) => (
                <Input
                  key={day}
                  label={day}
                  name={`weekdayRates-${i}`}
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.weekdayRates[i]}
                  onChange={(e) => {
                    const next = [...form.weekdayRates]
                    next[i] = Number(e.target.value)
                    setForm({ ...form, weekdayRates: next })
                  }}
                />
              ))}
            </div>
          </fieldset>

          <div className={FORM_ROW}>
            <Input
              label="Cargo extra adulto (MXN)"
              name="adultExtraRate"
              type="number"
              step="0.01"
              min="0"
              value={form.adultExtraRate}
              onChange={(e) => setForm({ ...form, adultExtraRate: Number(e.target.value) })}
              error={fieldErrors.adultExtraRate}
            />
            <Input
              label="Cargo extra niño (MXN)"
              name="childExtraRate"
              type="number"
              step="0.01"
              min="0"
              value={form.childExtraRate}
              onChange={(e) => setForm({ ...form, childExtraRate: Number(e.target.value) })}
              error={fieldErrors.childExtraRate}
            />
            <Input
              label="Noches mínimas"
              name="minNights"
              type="number"
              min="1"
              value={form.minNights}
              onChange={(e) => setForm({ ...form, minNights: Number(e.target.value) })}
              error={fieldErrors.minNights}
              required
            />
            <Input
              label="Noches máximas"
              name="maxNights"
              type="number"
              min="1"
              value={form.maxNights}
              onChange={(e) => setForm({ ...form, maxNights: e.target.value ? Number(e.target.value) : '' })}
              hint="Vacío = sin límite"
            />
            <Input
              label="Vigente desde"
              name="validFrom"
              type="date"
              value={form.validFrom}
              onChange={(e) => setForm({ ...form, validFrom: e.target.value })}
              error={fieldErrors.validFrom}
              required
            />
            <Input
              label="Vigente hasta"
              name="validTo"
              type="date"
              value={form.validTo}
              onChange={(e) => setForm({ ...form, validTo: e.target.value })}
              hint="Vacío = sin fin"
            />
          </div>

          <div className="mb-3.5 flex flex-wrap gap-5">
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input
                type="checkbox"
                name="isDefault"
                checked={form.isDefault}
                onChange={(e) => setForm({ ...form, isDefault: e.target.checked })}
              />
              Plan por defecto
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input
                type="checkbox"
                name="active"
                checked={form.active}
                onChange={(e) => setForm({ ...form, active: e.target.checked })}
              />
              Activo
            </label>
          </div>

          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton
              loading={createMut.isPending || updateMut.isPending}
              label={editing ? 'Guardar cambios' : 'Crear plan'}
            />
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!toggleTarget}
        title={toggleTarget?.active ? 'Desactivar plan' : 'Activar plan'}
        message={`¿Confirmar el cambio de estado del plan "${toggleTarget?.name ?? ''}"?`}
        confirmLabel={toggleTarget?.active ? 'Desactivar' : 'Activar'}
        loading={setActiveMut.isPending}
        onConfirm={applyToggle}
        onCancel={() => setToggleTarget(null)}
      />
    </div>
  )
}
