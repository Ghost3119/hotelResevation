import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { Input, Select, TextArea } from '../components/FormField'
import { PrivacyRequestStatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/Toast'
import {
  useAnonymizePrivacyRequest,
  useCreatePrivacyRequest,
  useExportPrivacyRequest,
  usePersonalDataAccessLogs,
  usePrivacyRequests,
  useUpdatePrivacyRequest,
} from '../hooks/usePrivacy'
import type { NormalizedError } from '../api/types'
import type {
  PersonalDataAccessLogDto,
  PrivacyRequestDto,
  PrivacyRequestStatus,
  PrivacyRequestType,
} from '../api/generated/schema'
import { getFieldErrors } from '../utils/error'
import { formatDateTime } from '../utils/format'
import {
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  DATA_TABLE,
  FORM_ALERT_ERROR,
  FORM_ACTIONS,
  PAGE_SIZE_DEFAULT,
  PRIVACY_REQUEST_TYPES,
  PRIVACY_REQUEST_TYPE_LABELS,
  TABLE_TD,
  TABLE_TH,
  TABLE_EMPTY_TD,
} from '../utils/constants'

interface RequestFormState {
  guestId: number
  type: PrivacyRequestType
  notes: string
}

const emptyForm: RequestFormState = { guestId: 0, type: 'EXPORT', notes: '' }

const BTN_DANGER_SM = `${BTN_SECONDARY} ${BTN_SM} border-red-300 text-red-700 hover:bg-red-50`

export function PrivacyRequestsPage() {
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<RequestFormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const { data, isLoading, isError, error, refetch, isFetching } = usePrivacyRequests({
    page,
    size: PAGE_SIZE_DEFAULT,
  })
  const logsQ = usePersonalDataAccessLogs({ size: 20 })

  const createMut = useCreatePrivacyRequest()
  const updateMut = useUpdatePrivacyRequest()
  const exportMut = useExportPrivacyRequest()
  const anonymizeMut = useAnonymizePrivacyRequest()

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
        guestId: form.guestId,
        type: form.type,
        notes: form.notes.trim() || null,
      })
      toast.success('Solicitud creada.')
      setFormOpen(false)
    } catch (err) {
      setFieldErrors(getFieldErrors(err))
      setSubmitError((err as NormalizedError).message)
    }
  }

  const handleExport = async (req: PrivacyRequestDto) => {
    try {
      const exportData = await exportMut.mutateAsync(req.id)
      const blob = new Blob([JSON.stringify(exportData, null, 2)], {
        type: 'application/json',
      })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `guest-${req.guestId}-export.json`
      a.click()
      URL.revokeObjectURL(url)
      toast.success('Datos exportados.')
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const handleAnonymize = async (req: PrivacyRequestDto) => {
    try {
      await anonymizeMut.mutateAsync(req.id)
      toast.success('Huésped anonimizado.')
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const handleStatus = async (req: PrivacyRequestDto, status: PrivacyRequestStatus) => {
    try {
      await updateMut.mutateAsync({ id: req.id, data: { status } })
      toast.success('Estado actualizado.')
    } catch (err) {
      toast.error((err as NormalizedError).message)
    }
  }

  const columns: Column<PrivacyRequestDto>[] = [
    { key: 'id', header: 'ID', render: (r) => `#${r.id}` },
    { key: 'guest', header: 'Huésped', render: (r) => `#${r.guestId}` },
    { key: 'type', header: 'Tipo', render: (r) => PRIVACY_REQUEST_TYPE_LABELS[r.type] },
    { key: 'status', header: 'Estado', render: (r) => <PrivacyRequestStatusBadge status={r.status} /> },
    { key: 'requestedAt', header: 'Solicitada', render: (r) => formatDateTime(r.requestedAt) },
    { key: 'completedAt', header: 'Completada', render: (r) => formatDateTime(r.completedAt) },
    { key: 'notes', header: 'Notas', render: (r) => r.notes ?? '—' },
    {
      key: 'actions',
      header: 'Acciones',
      render: (r) => (
        <div className="flex flex-wrap gap-2">
          {r.type === 'EXPORT' && (
            <button
              type="button"
              className={`${BTN_SECONDARY} ${BTN_SM}`}
              onClick={() => handleExport(r)}
              disabled={exportMut.isPending}
            >
              Exportar
            </button>
          )}
          {r.type === 'DELETE' && r.status !== 'COMPLETED' && (
            <button
              type="button"
              className={`${BTN_DANGER_SM}`}
              onClick={() => handleAnonymize(r)}
              disabled={anonymizeMut.isPending}
            >
              Anonimizar
            </button>
          )}
          {r.status === 'PENDING' && (
            <button
              type="button"
              className={`${BTN_SECONDARY} ${BTN_SM}`}
              onClick={() => handleStatus(r, 'IN_PROGRESS')}
            >
              Iniciar
            </button>
          )}
          {r.status === 'IN_PROGRESS' && r.type !== 'DELETE' && (
            <button
              type="button"
              className={`${BTN_SECONDARY} ${BTN_SM}`}
              onClick={() => handleStatus(r, 'COMPLETED')}
            >
              Completar
            </button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Solicitudes de privacidad"
        subtitle="Gestión de EXPORT, RECTIFY y DELETE con auditoría de acceso a datos"
        actions={
          <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
            Nueva solicitud
          </button>
        }
      />

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin solicitudes" message="No hay solicitudes de privacidad registradas." />
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

      <h2 className="mb-2.5 mt-6 text-base font-semibold text-slate-900">Auditoría de acceso a datos</h2>
      {logsQ.isLoading && <LoadingState />}
      {logsQ.isError && <ErrorState error={logsQ.error} onRetry={() => logsQ.refetch()} />}
      {logsQ.data && (
        <div className={DATA_TABLE}>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr>
                  <th scope="col" className={TABLE_TH}>ID</th>
                  <th scope="col" className={TABLE_TH}>Usuario</th>
                  <th scope="col" className={TABLE_TH}>Huésped</th>
                  <th scope="col" className={TABLE_TH}>Acción</th>
                  <th scope="col" className={TABLE_TH}>Justificación</th>
                  <th scope="col" className={TABLE_TH}>Fecha</th>
                </tr>
              </thead>
              <tbody>
                {logsQ.data.content.length === 0 ? (
                  <tr>
                    <td className={TABLE_EMPTY_TD} colSpan={6}>Sin registros de auditoría.</td>
                  </tr>
                ) : (
                  logsQ.data.content.map((log: PersonalDataAccessLogDto) => (
                    <tr key={log.id}>
                      <td className={TABLE_TD}>#{log.id}</td>
                      <td className={TABLE_TD}>#{log.userId}</td>
                      <td className={TABLE_TD}>#{log.guestId}</td>
                      <td className={TABLE_TD}>{log.action}</td>
                      <td className={TABLE_TD}>{log.justification ?? '—'}</td>
                      <td className={TABLE_TD}>{formatDateTime(log.createdAt)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal
        open={formOpen}
        title="Nueva solicitud de privacidad"
        onClose={() => setFormOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <Input
            label="ID del huésped"
            name="guestId"
            type="number"
            min={1}
            value={form.guestId || ''}
            onChange={(e) => setForm({ ...form, guestId: Number(e.target.value) })}
            error={fieldErrors.guestId}
            required
          />
          <Select
            label="Tipo de solicitud"
            name="type"
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value as PrivacyRequestType })}
          >
            {PRIVACY_REQUEST_TYPES.map((t) => (
              <option key={t} value={t}>{PRIVACY_REQUEST_TYPE_LABELS[t]}</option>
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
            <ModalSubmitButton loading={createMut.isPending} label="Crear solicitud" />
          </div>
        </form>
      </Modal>
    </div>
  )
}
