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
  useCreateSeasonalRate,
  useDeleteSeasonalRate,
  useSeasonalRates,
  useUpdateSeasonalRate,
} from '../hooks/useSeasonalRates'
import { useRatePlans } from '../hooks/useRatePlans'
import type { NormalizedError } from '../api/types'
import type { SeasonalRateDto } from '../api/generated/schema'
import { getFieldErrors, normalizeError } from '../utils/error'
import { formatDate, todayISO } from '../utils/format'
import {
  BTN_DANGER,
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
  SEASON_TYPES,
  SEASON_TYPE_LABELS,
  PRICE_MODES,
  PRICE_MODE_LABELS,
  WEEKDAY_LABELS,
} from '../utils/constants'

interface SeasonalFormState {
  ratePlanId: number
  name: string
  startDate: string
  endDate: string
  seasonType: 'ALTA' | 'MEDIA' | 'BAJA'
  priceMode: 'MULTIPLIER' | 'ABSOLUTE'
  weekdays: number[]
}

const emptyForm: SeasonalFormState = {
  ratePlanId: 0,
  name: '',
  startDate: todayISO(),
  endDate: todayISO(),
  seasonType: 'MEDIA',
  priceMode: 'MULTIPLIER',
  weekdays: [1, 1, 1, 1, 1, 1.2, 1.2],
}

export function SeasonalRatesPage() {
  const { user } = useAuth()
  const toast = useToast()
  const canEdit = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  const [ratePlanFilter, setRatePlanFilter] = useState<number | ''>('')
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<SeasonalRateDto | null>(null)
  const [form, setForm] = useState<SeasonalFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<SeasonalRateDto | null>(null)

  const params = { page, size: 20, ratePlanId: ratePlanFilter || undefined }
  const { data, isLoading, isError, error, refetch, isFetching } = useSeasonalRates(params)
  const ratePlansQ = useRatePlans({ size: 100 })

  const createMut = useCreateSeasonalRate()
  const updateMut = useUpdateSeasonalRate()
  const deleteMut = useDeleteSeasonalRate()

  const ratePlans = ratePlansQ.data?.content ?? []
  const ratePlanName = (id: number) =>
    ratePlans.find((rp) => rp.id === id)?.name ?? `#${id}`

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm, ratePlanId: ratePlans[0]?.id ?? 0 })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const openEdit = (s: SeasonalRateDto) => {
    setEditing(s)
    setForm({
      ratePlanId: s.ratePlanId,
      name: s.name,
      startDate: s.startDate,
      endDate: s.endDate,
      seasonType: s.seasonType,
      priceMode: s.priceMode,
      weekdays: [...s.weekdays],
    })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    const payload = { ...form, weekdays: form.weekdays }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Temporada actualizada.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Temporada creada.')
      }
      setFormOpen(false)
    } catch (err) {
      const normalized = normalizeError(err)
      setFieldErrors(getFieldErrors(err))
      // ER-2.2-AC: overlap → 409 SEASON_OVERLAP
      if (normalized.code === 'SEASON_OVERLAP') {
        setSubmitError('Las fechas se solapan con otra temporada del mismo plan.')
      } else {
        setSubmitError(normalized.message)
      }
    }
  }

  const applyDelete = async () => {
    if (!deleteTarget) return
    try {
      await deleteMut.mutateAsync(deleteTarget.id)
      toast.success('Temporada eliminada.')
      setDeleteTarget(null)
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const columns: Column<SeasonalRateDto>[] = [
    { key: 'name', header: 'Nombre', render: (s) => s.name },
    { key: 'plan', header: 'Plan', render: (s) => ratePlanName(s.ratePlanId) },
    { key: 'season', header: 'Temporada', render: (s) => SEASON_TYPE_LABELS[s.seasonType] },
    { key: 'mode', header: 'Modo', render: (s) => PRICE_MODE_LABELS[s.priceMode] },
    {
      key: 'range',
      header: 'Rango',
      render: (s) => `${formatDate(s.startDate)} → ${formatDate(s.endDate)}`,
    },
    {
      key: 'actions',
      header: 'Acciones',
      render: (s) => (
        <div className="flex flex-wrap gap-2">
          {canEdit && (
            <>
              <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(s)}>
                Editar
              </button>
              <button type="button" className={`${BTN_DANGER} ${BTN_SM}`} onClick={() => setDeleteTarget(s)}>
                Eliminar
              </button>
            </>
          )}
        </div>
      ),
    },
  ]

  const inputLabel = form.priceMode === 'MULTIPLIER' ? 'Multiplicador' : 'Precio (MXN)'
  const inputStep = form.priceMode === 'MULTIPLIER' ? '0.001' : '0.01'

  return (
    <div>
      <PageHeader
        title="Tarifas de temporada"
        subtitle="Temporadas y multiplicadores/precios por plan tarifario"
        actions={
          <RoleGate roles={['ADMIN', 'MANAGER']}>
            <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
              Nueva temporada
            </button>
          </RoleGate>
        }
      />

      <div className="mb-4 flex flex-wrap items-end gap-2.5">
        <Select
          label="Plan tarifario"
          name="ratePlanFilter"
          value={ratePlanFilter}
          onChange={(e) => {
            setRatePlanFilter(e.target.value ? Number(e.target.value) : '')
            setPage(0)
          }}
        >
          <option value="">Todos</option>
          {ratePlans.map((rp) => (
            <option key={rp.id} value={rp.id}>{rp.name}</option>
          ))}
        </Select>
      </div>

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin temporadas" message="No hay temporadas configuradas." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(s) => s.id}
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
        title={editing ? 'Editar temporada' : 'Nueva temporada'}
        onClose={() => setFormOpen(false)}
        size="lg"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <div className={FORM_ROW}>
            <Select
              label="Plan tarifario"
              name="ratePlanId"
              value={form.ratePlanId}
              onChange={(e) => setForm({ ...form, ratePlanId: Number(e.target.value) })}
              error={fieldErrors.ratePlanId}
              required
            >
              <option value="">Seleccione…</option>
              {ratePlans.map((rp) => (
                <option key={rp.id} value={rp.id}>{rp.name}</option>
              ))}
            </Select>
            <Input
              label="Nombre"
              name="name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              error={fieldErrors.name}
              required
            />
            <Select
              label="Temporada"
              name="seasonType"
              value={form.seasonType}
              onChange={(e) => setForm({ ...form, seasonType: e.target.value as SeasonalFormState['seasonType'] })}
            >
              {SEASON_TYPES.map((t) => (
                <option key={t} value={t}>{SEASON_TYPE_LABELS[t]}</option>
              ))}
            </Select>
            <Select
              label="Modo de precio"
              name="priceMode"
              value={form.priceMode}
              onChange={(e) => setForm({ ...form, priceMode: e.target.value as SeasonalFormState['priceMode'] })}
            >
              {PRICE_MODES.map((m) => (
                <option key={m} value={m}>{PRICE_MODE_LABELS[m]}</option>
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

          <fieldset className="mb-3.5">
            <legend className="mb-1 text-sm font-medium text-slate-600">
              Valores por día de la semana ({inputLabel})
            </legend>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
              {WEEKDAY_LABELS.map((day, i) => (
                <Input
                  key={day}
                  label={day}
                  name={`weekdays-${i}`}
                  type="number"
                  step={inputStep}
                  min="0"
                  value={form.weekdays[i]}
                  onChange={(e) => {
                    const next = [...form.weekdays]
                    next[i] = Number(e.target.value)
                    setForm({ ...form, weekdays: next })
                  }}
                />
              ))}
            </div>
          </fieldset>

          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending || updateMut.isPending} />
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Eliminar temporada"
        message={`¿Eliminar la temporada "${deleteTarget?.name ?? ''}"? Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar"
        destructive
        loading={deleteMut.isPending}
        onConfirm={applyDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
