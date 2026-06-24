import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { Input, Select } from '../components/FormField'
import { useToast } from '../components/Toast'
import { RoleGate } from '../auth/RoleGate'
import { useAuth } from '../auth/AuthContext'
import { useCreateTax, useTaxes, useUpdateTax } from '../hooks/useTaxes'
import type { NormalizedError } from '../api/types'
import type { TaxOrFeeDto } from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatDate, todayISO } from '../utils/format'
import {
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
  TAX_TYPES,
  TAX_TYPE_LABELS,
  TAX_APPLIES_TO,
  TAX_APPLIES_TO_LABELS,
} from '../utils/constants'

interface TaxFormState {
  name: string
  type: 'TAX_PERCENT' | 'FEE_FIXED'
  value: number
  appliesTo: 'ROOM_RATE' | 'TOTAL' | 'PER_NIGHT'
  validFrom: string
  validTo: string
  active: boolean
}

const emptyForm: TaxFormState = {
  name: '',
  type: 'TAX_PERCENT',
  value: 0,
  appliesTo: 'ROOM_RATE',
  validFrom: todayISO(),
  validTo: '',
  active: true,
}

export function TaxesAndFeesPage() {
  const { user } = useAuth()
  const toast = useToast()
  const canEdit = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<TaxOrFeeDto | null>(null)
  const [form, setForm] = useState<TaxFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const { data, isLoading, isError, error, refetch, isFetching } = useTaxes({ page, size: 20 })
  const createMut = useCreateTax()
  const updateMut = useUpdateTax()

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const openEdit = (t: TaxOrFeeDto) => {
    setEditing(t)
    setForm({
      name: t.name,
      type: t.type,
      value: t.value,
      appliesTo: t.appliesTo,
      validFrom: t.validFrom,
      validTo: t.validTo ?? '',
      active: t.active,
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
      name: form.name,
      type: form.type,
      value: form.value,
      appliesTo: form.appliesTo,
      validFrom: form.validFrom,
      validTo: form.validTo || null,
      active: form.active,
    }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Impuesto/cargo actualizado.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Impuesto/cargo creado.')
      }
      setFormOpen(false)
    } catch (err) {
      setFieldErrors(getFieldErrors(err))
      setSubmitError((err as NormalizedError).message)
    }
  }

  const valueLabel = form.type === 'TAX_PERCENT' ? 'Porcentaje (%)' : 'Importe (MXN)'

  const columns: Column<TaxOrFeeDto>[] = [
    { key: 'name', header: 'Nombre', render: (t) => t.name },
    { key: 'type', header: 'Tipo', render: (t) => TAX_TYPE_LABELS[t.type] },
    {
      key: 'value',
      header: 'Valor',
      render: (t) => (t.type === 'TAX_PERCENT' ? `${t.value}%` : formatValue(t.value)),
    },
    { key: 'appliesTo', header: 'Aplica a', render: (t) => TAX_APPLIES_TO_LABELS[t.appliesTo] },
    {
      key: 'validity',
      header: 'Vigencia',
      render: (t) => `${formatDate(t.validFrom)} → ${formatDate(t.validTo)}`,
    },
    {
      key: 'active',
      header: 'Estado',
      render: (t) =>
        t.active ? (
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
      render: (t) =>
        canEdit ? (
          <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(t)}>
            Editar
          </button>
        ) : (
          '—'
        ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Impuestos y cargos"
        subtitle="Impuestos y cargos configurables con vigencia temporal"
        actions={
          <RoleGate roles={['ADMIN', 'MANAGER']}>
            <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
              Nuevo impuesto
            </button>
          </RoleGate>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin impuestos" message="No hay impuestos o cargos configurados." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(t) => t.id}
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
        title={editing ? 'Editar impuesto/cargo' : 'Nuevo impuesto/cargo'}
        onClose={() => setFormOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <Input
            label="Nombre"
            name="name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            error={fieldErrors.name}
            required
          />
          <div className={FORM_ROW}>
            <Select
              label="Tipo"
              name="type"
              value={form.type}
              onChange={(e) => setForm({ ...form, type: e.target.value as TaxFormState['type'] })}
            >
              {TAX_TYPES.map((t) => (
                <option key={t} value={t}>{TAX_TYPE_LABELS[t]}</option>
              ))}
            </Select>
            <Input
              label={valueLabel}
              name="value"
              type="number"
              step="0.01"
              min="0"
              value={form.value}
              onChange={(e) => setForm({ ...form, value: Number(e.target.value) })}
              error={fieldErrors.value}
              required
            />
            <Select
              label="Aplica a"
              name="appliesTo"
              value={form.appliesTo}
              onChange={(e) => setForm({ ...form, appliesTo: e.target.value as TaxFormState['appliesTo'] })}
            >
              {TAX_APPLIES_TO.map((a) => (
                <option key={a} value={a}>{TAX_APPLIES_TO_LABELS[a]}</option>
              ))}
            </Select>
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
          <label className="mb-3.5 flex items-center gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              name="active"
              checked={form.active}
              onChange={(e) => setForm({ ...form, active: e.target.checked })}
            />
            Activo
          </label>
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending || updateMut.isPending} />
          </div>
        </form>
      </Modal>
    </div>
  )
}

function formatValue(value: number): string {
  return new Intl.NumberFormat('es-MX', {
    style: 'currency',
    currency: 'MXN',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}
