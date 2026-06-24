import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { Input, Select } from '../components/FormField'
import { useToast } from '../components/Toast'
import { RoleGate } from '../auth/RoleGate'
import { useAuth } from '../auth/AuthContext'
import {
  useCancellationPolicies,
  useCreateCancellationPolicy,
  useUpdateCancellationPolicy,
} from '../hooks/useCancellationPolicies'
import type { NormalizedError } from '../api/types'
import type { CancellationPolicyDto } from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatCurrency } from '../utils/format'
import {
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  CARD,
  CARD_BODY,
  DATA_TABLE,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
  PENALTY_TYPES,
  PENALTY_TYPE_LABELS,
  TABLE_TD,
  TABLE_TH,
} from '../utils/constants'

interface PolicyFormState {
  name: string
  deadlineHours: number
  penaltyType: 'NONE' | 'PERCENTAGE' | 'FIXED' | 'FIRST_NIGHT'
  penaltyValue: number
  noShowPenaltyType: 'NONE' | 'PERCENTAGE' | 'FIXED' | 'FIRST_NIGHT'
  noShowPenaltyValue: number
  active: boolean
}

const emptyForm: PolicyFormState = {
  name: '',
  deadlineHours: 48,
  penaltyType: 'NONE',
  penaltyValue: 0,
  noShowPenaltyType: 'NONE',
  noShowPenaltyValue: 0,
  active: true,
}

export function CancellationPoliciesPage() {
  const { user } = useAuth()
  const toast = useToast()
  const canEdit = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  const { data, isLoading, isError, error, refetch } = useCancellationPolicies()
  const createMut = useCreateCancellationPolicy()
  const updateMut = useUpdateCancellationPolicy()

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<CancellationPolicyDto | null>(null)
  const [form, setForm] = useState<PolicyFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const openEdit = (p: CancellationPolicyDto) => {
    setEditing(p)
    setForm({
      name: p.name,
      deadlineHours: p.deadlineHours,
      penaltyType: p.penaltyType,
      penaltyValue: p.penaltyValue,
      noShowPenaltyType: p.noShowPenaltyType,
      noShowPenaltyValue: p.noShowPenaltyValue,
      active: p.active,
    })
    setFieldErrors({})
    setSubmitError(null)
    setFormOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    const payload = { ...form }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Política actualizada.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Política creada.')
      }
      setFormOpen(false)
    } catch (err) {
      setFieldErrors(getFieldErrors(err))
      setSubmitError((err as NormalizedError).message)
    }
  }

  return (
    <div>
      <PageHeader
        title="Políticas de cancelación"
        subtitle="Plazos y penalizaciones de cancelación y no-show"
        actions={
          <RoleGate roles={['ADMIN', 'MANAGER']}>
            <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
              Nueva política
            </button>
          </RoleGate>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.length === 0 ? (
          <EmptyState title="Sin políticas" message="No hay políticas de cancelación configuradas." />
        ) : (
          <div className={DATA_TABLE}>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse">
                <thead>
                  <tr>
                    <th scope="col" className={TABLE_TH}>Nombre</th>
                    <th scope="col" className={TABLE_TH}>Plazo (h)</th>
                    <th scope="col" className={TABLE_TH}>Penalización</th>
                    <th scope="col" className={TABLE_TH}>No-show</th>
                    <th scope="col" className={TABLE_TH}>Estado</th>
                    <th scope="col" className={TABLE_TH}>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((p) => (
                    <tr key={p.id}>
                      <td className={TABLE_TD}>{p.name}</td>
                      <td className={TABLE_TD}>{p.deadlineHours}</td>
                      <td className={TABLE_TD}>
                        {PENALTY_TYPE_LABELS[p.penaltyType]}
                        {p.penaltyType === 'PERCENTAGE' && ` (${p.penaltyValue}%)`}
                        {p.penaltyType === 'FIXED' && ` (${formatCurrency(p.penaltyValue)})`}
                      </td>
                      <td className={TABLE_TD}>
                        {PENALTY_TYPE_LABELS[p.noShowPenaltyType]}
                        {p.noShowPenaltyType === 'PERCENTAGE' && ` (${p.noShowPenaltyValue}%)`}
                        {p.noShowPenaltyType === 'FIXED' && ` (${formatCurrency(p.noShowPenaltyValue)})`}
                      </td>
                      <td className={TABLE_TD}>
                        {p.active ? (
                          <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
                            Activa
                          </span>
                        ) : (
                          <span className="inline-flex items-center rounded-full bg-gray-200 px-2.5 py-0.5 text-xs font-medium text-gray-800">
                            Inactiva
                          </span>
                        )}
                      </td>
                      <td className={TABLE_TD}>
                        {canEdit ? (
                          <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(p)}>
                            Editar
                          </button>
                        ) : (
                          '—'
                        )}
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
        title={editing ? 'Editar política' : 'Nueva política de cancelación'}
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
            <Input
              label="Plazo (horas antes del check-in)"
              name="deadlineHours"
              type="number"
              min="0"
              value={form.deadlineHours}
              onChange={(e) => setForm({ ...form, deadlineHours: Number(e.target.value) })}
              error={fieldErrors.deadlineHours}
              required
            />
            <Select
              label="Penalización (cancelación)"
              name="penaltyType"
              value={form.penaltyType}
              onChange={(e) => setForm({ ...form, penaltyType: e.target.value as PolicyFormState['penaltyType'] })}
            >
              {PENALTY_TYPES.map((p) => (
                <option key={p} value={p}>{PENALTY_TYPE_LABELS[p]}</option>
              ))}
            </Select>
            <Input
              label="Valor penalización"
              name="penaltyValue"
              type="number"
              step="0.01"
              min="0"
              value={form.penaltyValue}
              onChange={(e) => setForm({ ...form, penaltyValue: Number(e.target.value) })}
              error={fieldErrors.penaltyValue}
              hint="% o MXN según el tipo"
            />
            <Select
              label="Penalización no-show"
              name="noShowPenaltyType"
              value={form.noShowPenaltyType}
              onChange={(e) => setForm({ ...form, noShowPenaltyType: e.target.value as PolicyFormState['noShowPenaltyType'] })}
            >
              {PENALTY_TYPES.map((p) => (
                <option key={p} value={p}>{PENALTY_TYPE_LABELS[p]}</option>
              ))}
            </Select>
            <Input
              label="Valor no-show"
              name="noShowPenaltyValue"
              type="number"
              step="0.01"
              min="0"
              value={form.noShowPenaltyValue}
              onChange={(e) => setForm({ ...form, noShowPenaltyValue: Number(e.target.value) })}
              error={fieldErrors.noShowPenaltyValue}
              hint="% o MXN según el tipo"
            />
          </div>
          <label className="mb-3.5 flex items-center gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              name="active"
              checked={form.active}
              onChange={(e) => setForm({ ...form, active: e.target.checked })}
            />
            Activa
          </label>
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setFormOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending || updateMut.isPending} />
          </div>
        </form>
      </Modal>

      <section className={`mt-5 ${CARD} ${CARD_BODY}`}>
        <h2 className="mb-1 text-base font-semibold text-slate-900">Resumen de penalizaciones</h2>
        <p className="text-sm text-slate-500">
          NONE: sin cargo · PERCENTAGE: % del total · FIXED: importe fijo (MXN) · FIRST_NIGHT: tarifa
          de la primera noche del snapshot.
        </p>
      </section>
    </div>
  )
}
