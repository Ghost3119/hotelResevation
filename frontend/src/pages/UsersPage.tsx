import { useState, type FormEvent } from 'react'
import clsx from 'clsx'
import { PageHeader } from '../components/PageHeader'
import { DataTable, type Column } from '../components/DataTable'
import { LoadingState } from '../components/LoadingState'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { Modal, ModalSubmitButton } from '../components/Modal'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { Input, Select } from '../components/FormField'
import { useToast } from '../components/Toast'
import { useCreateUser, useSetUserStatus, useUpdateUser, useUsers } from '../hooks/useUsers'
import type { NormalizedError, UserDto, UserRole } from '../api/types'
import type { UserWriteDto } from '../api/users.api'
import { getFieldErrors } from '../utils/error'
import { useDebounce } from '../hooks/useDebounce'
import {
  PAGE_SIZE_DEFAULT,
  ROLE_LABELS,
  BADGE_BASE,
  BTN_PRIMARY,
  BTN_SECONDARY,
  BTN_SM,
  FORM_ALERT_ERROR,
  FORM_ROW,
  FORM_ACTIONS,
} from '../utils/constants'

const emptyForm: UserWriteDto = {
  email: '',
  password: '',
  fullName: '',
  role: 'RECEPCIONISTA',
  active: true,
}

export function UsersPage() {
  const toast = useToast()
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search)
  const [page, setPage] = useState(0)
  const size = PAGE_SIZE_DEFAULT

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<UserDto | null>(null)
  const [form, setForm] = useState<UserWriteDto>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [toggleTarget, setToggleTarget] = useState<UserDto | null>(null)

  const params = { page, size, search: debouncedSearch || undefined }
  const { data, isLoading, isError, error, refetch, isFetching } = useUsers(params)
  const createMut = useCreateUser()
  const updateMut = useUpdateUser()
  const setStatusMut = useSetUserStatus()

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm })
    setFieldErrors({})
    setSubmitError(null)
    setModalOpen(true)
  }

  const openEdit = (user: UserDto) => {
    setEditing(user)
    setForm({
      email: user.email,
      password: '',
      fullName: user.fullName,
      role: user.role,
      active: user.active,
    })
    setFieldErrors({})
    setSubmitError(null)
    setModalOpen(true)
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFieldErrors({})
    setSubmitError(null)
    const payload: UserWriteDto = {
      ...form,
      password: form.password?.trim() || undefined,
    }
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, data: payload })
        toast.success('Usuario actualizado.')
      } else {
        await createMut.mutateAsync(payload)
        toast.success('Usuario creado.')
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

  const columns: Column<UserDto>[] = [
    { key: 'fullName', header: 'Nombre', render: (u) => u.fullName },
    { key: 'email', header: 'Correo', render: (u) => u.email },
    { key: 'role', header: 'Rol', render: (u) => ROLE_LABELS[u.role] },
    {
      key: 'active',
      header: 'Estado',
      render: (u) => (
        <span
          className={clsx(
            BADGE_BASE,
            u.active ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-800',
          )}
        >
          {u.active ? 'Activo' : 'Inactivo'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Acciones',
      render: (u) => (
        <div className="flex flex-wrap gap-2">
          <button type="button" className={`${BTN_SECONDARY} ${BTN_SM}`} onClick={() => openEdit(u)}>
            Editar
          </button>
          <button
            type="button"
            className={`${BTN_SECONDARY} ${BTN_SM}`}
            onClick={() => setToggleTarget(u)}
          >
            {u.active ? 'Desactivar' : 'Activar'}
          </button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Usuarios"
        subtitle="Gestión de usuarios y roles"
        actions={
          <button type="button" className={BTN_PRIMARY} onClick={openCreate}>
            Nuevo usuario
          </button>
        }
      />

      <div className="mb-4 flex flex-wrap items-end gap-2.5">
        <Input
          label="Buscar"
          name="search"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(0)
          }}
          placeholder="Nombre o correo…"
          wrapperClassName="min-w-[240px]"
        />
      </div>

      {isLoading && <LoadingState />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {!isLoading && !isError && data && (
        data.content.length === 0 ? (
          <EmptyState title="Sin usuarios" message="No se encontraron usuarios." />
        ) : (
          <DataTable
            columns={columns}
            data={data.content}
            rowKey={(u) => u.id}
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
        open={modalOpen}
        title={editing ? 'Editar usuario' : 'Nuevo usuario'}
        onClose={() => setModalOpen(false)}
        size="md"
      >
        <form onSubmit={onSubmit} noValidate>
          {submitError && (
            <div className={FORM_ALERT_ERROR} role="alert">{submitError}</div>
          )}
          <Input
            label="Nombre completo"
            name="fullName"
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            error={fieldErrors.fullName}
            required
          />
          <Input
            label="Correo electrónico"
            name="email"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            error={fieldErrors.email}
            required
          />
          <Input
            label={editing ? 'Contraseña (dejar en blanco para no cambiar)' : 'Contraseña'}
            name="password"
            type="password"
            value={form.password ?? ''}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            error={fieldErrors.password}
            required={!editing}
          />
          <div className={FORM_ROW}>
            <Select
              label="Rol"
              name="role"
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value as UserRole })}
              error={fieldErrors.role}
              required
            >
              <option value="ADMIN">Administrador</option>
              <option value="RECEPCIONISTA">Recepcionista</option>
            </Select>
            <Select
              label="Estado"
              name="active"
              value={form.active ? 'true' : 'false'}
              onChange={(e) => setForm({ ...form, active: e.target.value === 'true' })}
            >
              <option value="true">Activo</option>
              <option value="false">Inactivo</option>
            </Select>
          </div>
          <div className={FORM_ACTIONS}>
            <button type="button" className={BTN_SECONDARY} onClick={() => setModalOpen(false)}>
              Cancelar
            </button>
            <ModalSubmitButton loading={createMut.isPending || updateMut.isPending} />
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!toggleTarget}
        title={toggleTarget?.active ? 'Desactivar usuario' : 'Activar usuario'}
        message={
          toggleTarget
            ? `¿Confirmar el cambio de estado del usuario "${toggleTarget.fullName}"?`
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
