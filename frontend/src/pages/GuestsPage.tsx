import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { Input } from '../components/FormField'
import { useToast } from '../components/Toast'
import { useDebounce } from '../hooks/useDebounce'
import {
  useCreateGuest,
  useGuests,
  useUpdateGuest,
} from '../hooks/useGuests'
import { type GuestWriteDto } from '../api/guests.api'
import type { GuestDto, NormalizedError } from '../api/types'
import {
  PAGE_SIZE_DEFAULT,
  ROUTES,
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
} from '../utils/constants'
import { formatDate, fullName } from '../utils/format'
import { getFieldErrors } from '../utils/error'

const emptyForm: GuestWriteDto = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  documentNumber: '',
  nationality: '',
}

export function GuestsPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search)
  const [page, setPage] = useState(0)
  const size = PAGE_SIZE_DEFAULT

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<GuestDto | null>(null)
  const [form, setForm] = useState<GuestWriteDto>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const params = { page, size, search: debouncedSearch || undefined }
  const { data, isLoading, isError, error, refetch, isFetching } = useGuests(params)
  const createMut = useCreateGuest()
  const updateMut = useUpdateGuest()

  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm)
    setFieldErrors({})
    setSubmitError(null)
    setModalOpen(true)
  }

  const openEdit = async (guest: GuestDto) => {
    setEditing(guest)
    setForm({
      firstName: guest.firstName,
      lastName: guest.lastName,
      email: guest.email ?? '',
      phone: guest.phone ?? '',
      documentNumber: guest.documentNumber,
      nationality: guest.nationality,
    })
    setFieldErrors({})
    setSubmitError(null)
    setModalOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    const payload: GuestWriteDto = {
      ...form,
      email: form.email?.trim() || null,
      phone: form.phone?.trim() || null,
    }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Huésped actualizado.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Huésped creado.')
      }
      setModalOpen(false)
    } catch (err) {
      const normalized = err as NormalizedError
      setFieldErrors(getFieldErrors(err))
      setSubmitError(normalized.message)
    }
  }

  const columns: Column<GuestDto>[] = [
    { key: 'name', header: 'Nombre', render: (g) => fullName(g.firstName, g.lastName) },
    { key: 'email', header: 'Correo', render: (g) => g.email ?? '—' },
    { key: 'documentNumber', header: 'Documento', render: (g) => g.documentNumber },
    { key: 'nationality', header: 'Nacionalidad', render: (g) => g.nationality },
    { key: 'phone', header: 'Teléfono', render: (g) => g.phone ?? '—' },
    { key: 'createdAt', header: 'Alta', render: (g) => formatDate(g.createdAt) },
    {
      key: 'actions',
      header: 'Acciones',
      render: (g) => (
        <div className="flex flex-wrap gap-2">
          <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(g)}>
            Editar
          </button>
          <button
            type="button"
            className={`${BTN_SECONDARY} ${BTN_SM}`}
            onClick={() => navigate(ROUTES.guestReservations(g.id))}
          >
            Reservas
          </button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Huéspedes"
        subtitle="Búsqueda y registro de huéspedes"
        actions={
          <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
            Nuevo huésped
          </button>
        }
      />

      <div className="mb-4 flex flex-wrap items-end gap-2.5">
        <Input
          name="search"
          label="Buscar por nombre, correo o documento"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(0)
          }}
          placeholder="Escriba para buscar…"
          wrapperClassName="min-w-[280px]"
        />
      </div>

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState
            title="Sin huéspedes"
            message="No se encontraron huéspedes con los criterios indicados."
            action={
              <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
                Nuevo huésped
              </button>
            }
          />
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
            empty={
              isFetching ? 'Actualizando…' : undefined
            }
          />
        )
      )}

      <Modal
        open={modalOpen}
        title={editing ? 'Editar huésped' : 'Nuevo huésped'}
        onClose={() => setModalOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <div className={FORM_ROW}>
            <Input
              label="Nombre"
              name="firstName"
              value={form.firstName}
              onChange={(e) => setForm({ ...form, firstName: e.target.value })}
              error={fieldErrors.firstName}
              required
            />
            <Input
              label="Apellidos"
              name="lastName"
              value={form.lastName}
              onChange={(e) => setForm({ ...form, lastName: e.target.value })}
              error={fieldErrors.lastName}
              required
            />
          </div>
          <div className={FORM_ROW}>
            <Input
              label="Correo electrónico"
              name="email"
              type="email"
              value={form.email ?? ''}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              error={fieldErrors.email}
            />
            <Input
              label="Teléfono"
              name="phone"
              value={form.phone ?? ''}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              error={fieldErrors.phone}
            />
          </div>
          <div className={FORM_ROW}>
            <Input
              label="Documento de identidad"
              name="documentNumber"
              value={form.documentNumber}
              onChange={(e) => setForm({ ...form, documentNumber: e.target.value })}
              error={fieldErrors.documentNumber}
              required
            />
            <Input
              label="Nacionalidad"
              name="nationality"
              value={form.nationality}
              onChange={(e) => setForm({ ...form, nationality: e.target.value })}
              error={fieldErrors.nationality}
              required
            />
          </div>
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setModalOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton
              loading={createMut.isPending || updateMut.isPending}
              label={editing ? 'Guardar cambios' : 'Crear huésped'}
            />
          </div>
        </form>
      </Modal>
    </div>
  )
}
