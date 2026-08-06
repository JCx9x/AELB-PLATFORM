'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../../components/Navbar'
import { getSession } from '../../lib/auth'
import {
  adminGetUsers,
  adminUpdateUser,
  adminDeleteUser,
  type UserProfile,
  type AdminUpdateUserData,
  getUserQuotas,
  markQuotaPaid,
  type UserQuota,
} from '../../lib/api'

const inputCls   = 'w-full px-3 py-2.5 rounded-[0.25rem] border text-sm outline-none transition-colors focus:ring-2 focus:ring-[#d32f2f]/30'
const inputStyle = {
  backgroundColor: 'var(--color-surface)',
  borderColor:     'var(--color-outline-variant)',
  color:           'var(--color-on-surface)',
}

const ROLES = ['USER', 'GESTOR', 'ADMIN']
const PAGE_SIZE = 20

function roleBadge(role: string) {
  const map: Record<string, { bg: string; color: string }> = {
    ADMIN:  { bg: 'rgba(211,47,47,0.12)',   color: '#d32f2f' },
    GESTOR: { bg: 'rgba(25,118,210,0.12)',  color: '#1565c0' },
    USER:   { bg: 'rgba(100,100,100,0.10)', color: 'var(--color-on-surface-variant)' },
  }
  const s = map[role.toUpperCase()] ?? map.USER
  return (
    <span
      className="inline-flex items-center text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
      style={{ backgroundColor: s.bg, color: s.color }}>
      {role}
    </span>
  )
}

function blockedBadge(blocked: boolean, t: (key: string) => string) {
  return blocked ? (
    <span className="inline-flex items-center text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
      style={{ backgroundColor: 'rgba(211,47,47,0.08)', color: '#d32f2f' }}>
      {t('bloqueado')}
    </span>
  ) : (
    <span className="inline-flex items-center text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
      style={{ backgroundColor: 'rgba(5,150,105,0.10)', color: '#047857' }}>
      {t('activo')}
    </span>
  )
}

function initials(u: UserProfile) {
  return `${u.firstName[0] ?? ''}${u.lastName[0] ?? ''}`.toUpperCase()
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-xs font-bold uppercase tracking-widest mb-1.5"
        style={{ color: 'var(--color-on-surface-variant)' }}>
        {label}
      </label>
      {children}
    </div>
  )
}

function ReadOnlyField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <label className="block text-xs font-bold uppercase tracking-widest mb-1.5"
        style={{ color: 'var(--color-on-surface-variant)' }}>
        {label}
      </label>
      <p className="px-3 py-2.5 rounded-[0.25rem] border text-sm"
        style={{
          backgroundColor: 'var(--color-surface)',
          borderColor:     'var(--color-outline-variant)',
          color:           'var(--color-on-surface-variant)',
        }}>
        {value || '—'}
      </p>
    </div>
  )
}

// ── Icons ─────────────────────────────────────────────────────────────────────

function EditIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
    </svg>
  )
}

function TrashIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/>
      <path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/>
    </svg>
  )
}

function CloseIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  )
}

function ChevronLeftIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="15 18 9 12 15 6"/>
    </svg>
  )
}

function ChevronRightIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="9 18 15 12 9 6"/>
    </svg>
  )
}

function UsersIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <svg className={className} style={style} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
      <circle cx="9" cy="7" r="4"/>
      <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
      <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    </svg>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function AdminUsuariosPage() {
  const t = useTranslations('AdminUsuarios')
  const locale = useLocale()
  const router = useRouter()
  const [myId,   setMyId]   = useState<string>('')
  const [myRole, setMyRole] = useState<string>('')

  const [users,      setUsers]      = useState<UserProfile[]>([])
  const [total,      setTotal]      = useState(0)
  const [page,       setPage]       = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading,    setLoading]    = useState(true)
  const [error,      setError]      = useState<string | null>(null)

  const [search,     setSearch]     = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [quotaFilter, setQuotaFilter] = useState<'PAID' | 'PENDING' | ''>('')

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [selected,   setSelected]   = useState<UserProfile | null>(null)
  const [saving,     setSaving]     = useState(false)
  const [saveError,  setSaveError]  = useState<string | null>(null)
  const [deleting,   setDeleting]   = useState<string | null>(null)
  const [selectedQuotas, setSelectedQuotas] = useState<UserQuota[]>([])
  const [quotaLoading, setQuotaLoading] = useState(false)
  const [markingQuota, setMarkingQuota] = useState(false)
  const [quotaActionError, setQuotaActionError] = useState<string | null>(null)

  // Drawer form state
  const [firstName, setFirstName] = useState('')
  const [lastName,  setLastName]  = useState('')
  const [phone,     setPhone]     = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [role,      setRole]      = useState('')
  const [blocked,   setBlocked]   = useState(false)

  // Auth guard
  useEffect(() => {
    const session = getSession()
    if (!session) { router.push('/login'); return }
    if (session.role === 'USER') { router.push('/'); return }
    setMyId(session.userId)
    setMyRole(session.role)
  }, [router])

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    adminGetUsers({ search, role: roleFilter, currentQuota: quotaFilter || undefined, page, size: PAGE_SIZE })
      .then((res) => {
        setUsers(res.users)
        setTotal(res.total)
        setTotalPages(res.totalPages)
      })
      .catch((err: { detail?: string }) => setError(err.detail ?? t('errorCargarUsuarios')))
      .finally(() => setLoading(false))
  }, [search, roleFilter, quotaFilter, page, t])

  useEffect(() => { load() }, [load])

  // Reset page when filters change
  useEffect(() => { setPage(0) }, [search, roleFilter, quotaFilter])

  function openEdit(user: UserProfile) {
    setSelected(user)
    setFirstName(user.firstName)
    setLastName(user.lastName)
    setPhone(user.phone ?? '')
    setBirthDate(user.birthDate ?? '')
    setRole(user.role)
    setBlocked(user.blocked)
    setSaveError(null)
    setSelectedQuotas([])
    setQuotaActionError(null)
    setDrawerOpen(true)
    setQuotaLoading(true)
    getUserQuotas(user.id)
      .then(setSelectedQuotas)
      .catch((err: { detail?: string }) => setQuotaActionError(err.detail ?? t('errorCargarCuota')))
      .finally(() => setQuotaLoading(false))
  }

  async function handleMarkCurrentQuotaPaid() {
    if (!selected) return
    const year = new Date().getFullYear()
    if (!confirm(t('confirmMarcarCuota', { year, name: `${selected.firstName} ${selected.lastName}` }))) return
    setMarkingQuota(true)
    setQuotaActionError(null)
    try {
      const quota = await markQuotaPaid(selected.id, year)
      setSelectedQuotas(current => [quota, ...current.filter(item => item.year !== year)])
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setQuotaActionError(e.detail ?? t('errorMarcarCuota'))
    } finally {
      setMarkingQuota(false)
    }
  }

  function closeDrawer() {
    setDrawerOpen(false)
    setSelected(null)
  }

  async function handleSave() {
    if (!firstName.trim() || !lastName.trim()) {
      setSaveError(t('errorNombreApellidosObligatorios'))
      return
    }
    setSaving(true)
    setSaveError(null)
    try {
      const data: AdminUpdateUserData = {
        firstName: firstName.trim(),
        lastName:  lastName.trim(),
        phone:     phone.trim() || null,
        birthDate: birthDate || null,
        role:      myRole === 'ADMIN' ? role : null,
        blocked,
      }
      await adminUpdateUser(selected!.id, data)
      closeDrawer()
      load()
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setSaveError(e.detail ?? t('errorGuardar'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(userId: string) {
    if (!confirm(t('confirmarEliminarUsuario'))) return
    setDeleting(userId)
    try {
      await adminDeleteUser(userId)
      setUsers((prev) => prev.filter((u) => u.id !== userId))
      setTotal((n) => n - 1)
    } catch {
      alert(t('errorEliminarUsuario'))
    } finally {
      setDeleting(null)
    }
  }

  const isSelf = (userId: string) => userId === myId
  const currentQuota = selectedQuotas.find(quota => quota.year === new Date().getFullYear())

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        {/* Header */}
        <div className="mb-8">
          <h1 className="font-extrabold text-2xl lg:text-3xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.02em' }}>
            {t('titulo')}
          </h1>
          <p className="mt-1 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
            {total > 0 ? t('usuariosRegistrados', { count: total }) : t('subtitulo')}
          </p>
        </div>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-3 mb-6">
          <input
            className={`${inputCls} sm:max-w-xs`}
            style={inputStyle}
            placeholder={t('placeholderBuscar')}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select
            className={`${inputCls} sm:max-w-[160px]`}
            style={inputStyle}
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">{t('todosLosRoles')}</option>
            {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
          <select
            className={`${inputCls} sm:max-w-[210px]`}
            style={inputStyle}
            value={quotaFilter}
            onChange={(e) => setQuotaFilter(e.target.value as 'PAID' | 'PENDING' | '')}>
            <option value="">{t('cuotaTodas')}</option>
            <option value="PAID">{t('cuotaPagada')}</option>
            <option value="PENDING">{t('cuotaPendiente')}</option>
          </select>
        </div>

        {error && (
          <div className="px-4 py-3 rounded-[0.25rem] text-sm mb-6"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            {error}
          </div>
        )}

        {loading && (
          <div className="flex items-center justify-center py-24">
            <div className="w-8 h-8 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!loading && users.length === 0 && (
          <div className="text-center py-24">
            <UsersIcon className="w-10 h-10 mx-auto mb-3" style={{ color: 'var(--color-outline)' }} />
            <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('sinUsuarios')}
            </p>
          </div>
        )}

        {!loading && users.length > 0 && (
          <>
            <div className="rounded-[0.25rem] border overflow-hidden" style={{ borderColor: 'var(--color-outline-variant)' }}>
              <table className="w-full text-sm">
                <thead>
                  <tr style={{ backgroundColor: 'var(--color-surface)', borderBottom: '1px solid var(--color-outline-variant)' }}>
                    <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px]"
                      style={{ color: 'var(--color-on-surface-variant)' }}>{t('colUsuario')}</th>
                    <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px] hidden md:table-cell"
                      style={{ color: 'var(--color-on-surface-variant)' }}>{t('colEmail')}</th>
                    <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px] hidden lg:table-cell"
                      style={{ color: 'var(--color-on-surface-variant)' }}>{t('colDni')}</th>
                    <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px]"
                      style={{ color: 'var(--color-on-surface-variant)' }}>{t('colRol')}</th>
                    <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px] hidden sm:table-cell"
                      style={{ color: 'var(--color-on-surface-variant)' }}>{t('colEstado')}</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody>
                  {users.map((user, i) => (
                    <tr key={user.id}
                      style={{
                        backgroundColor: i % 2 === 0 ? 'var(--color-surface-card)' : 'var(--color-surface)',
                        borderBottom: '1px solid var(--color-outline-variant)',
                      }}>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-[11px] font-bold text-white"
                            style={{ backgroundColor: '#d32f2f' }}>
                            {initials(user)}
                          </div>
                          <div>
                            <p className="font-semibold leading-tight" style={{ color: 'var(--color-on-surface)' }}>
                              {user.firstName} {user.lastName}
                              {isSelf(user.id) && (
                                <span className="ml-1.5 text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded"
                                  style={{ backgroundColor: 'rgba(211,47,47,0.1)', color: '#d32f2f' }}>
                                  {t('tu')}
                                </span>
                              )}
                            </p>
                            <p className="text-xs mt-0.5 md:hidden" style={{ color: 'var(--color-on-surface-variant)' }}>
                              {user.email}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 hidden md:table-cell">
                        <span className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {user.email}
                        </span>
                      </td>
                      <td className="px-4 py-3 hidden lg:table-cell">
                        <span className="text-xs font-mono" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {user.dni}
                        </span>
                      </td>
                      <td className="px-4 py-3">{roleBadge(user.role)}</td>
                      <td className="px-4 py-3 hidden sm:table-cell">{blockedBadge(user.blocked, t)}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => openEdit(user)}
                            className="p-1.5 rounded transition-colors hover:bg-black/5"
                            title={t('editar')}
                            style={{ color: 'var(--color-on-surface-variant)' }}>
                            <EditIcon className="w-4 h-4" />
                          </button>
                          {myRole === 'ADMIN' && (
                            <button
                              onClick={() => handleDelete(user.id)}
                              disabled={deleting === user.id || isSelf(user.id)}
                              className="p-1.5 rounded transition-colors hover:bg-red-50 disabled:opacity-30 disabled:cursor-not-allowed"
                              title={isSelf(user.id) ? t('noPuedesEliminarte') : t('eliminar')}
                              style={{ color: 'var(--color-error)' }}>
                              {deleting === user.id
                                ? <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
                                : <TrashIcon className="w-4 h-4" />}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-5">
                <p className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                  {t('paginaDe', { page: page + 1, totalPages, total })}
                </p>
                <div className="flex gap-1">
                  <button
                    onClick={() => setPage((p) => p - 1)}
                    disabled={page === 0}
                    className="p-2 rounded-[0.25rem] border transition-colors hover:bg-black/5 disabled:opacity-30 disabled:cursor-not-allowed"
                    style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                    <ChevronLeftIcon className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= totalPages - 1}
                    className="p-2 rounded-[0.25rem] border transition-colors hover:bg-black/5 disabled:opacity-30 disabled:cursor-not-allowed"
                    style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                    <ChevronRightIcon className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* ── Drawer ────────────────────────────────────────────────────────────── */}
      {drawerOpen && selected && (
        <div className="fixed inset-0 z-50 flex">
          <div className="flex-1 bg-black/40" onClick={closeDrawer} />

          <aside className="w-full max-w-lg flex flex-col shadow-2xl overflow-y-auto"
            style={{ backgroundColor: 'var(--color-surface-card)' }}>

            <div className="flex items-center justify-between px-6 py-4 border-b"
              style={{ borderColor: 'var(--color-outline-variant)' }}>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold text-white"
                  style={{ backgroundColor: '#d32f2f' }}>
                  {initials(selected)}
                </div>
                <div>
                  <h2 className="font-bold text-base leading-tight" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                    {selected.firstName} {selected.lastName}
                  </h2>
                  <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {selected.email}
                  </p>
                </div>
              </div>
              <button onClick={closeDrawer}
                className="p-1.5 rounded transition-colors hover:bg-black/5"
                style={{ color: 'var(--color-on-surface-variant)' }}>
                <CloseIcon className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 px-6 py-6 space-y-5">

              {saveError && (
                <div className="px-4 py-3 rounded-[0.25rem] text-sm"
                  style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
                  {saveError}
                </div>
              )}

              {isSelf(selected.id) && (
                <div className="px-4 py-3 rounded-[0.25rem] text-sm"
                  style={{ backgroundColor: 'rgba(211,47,47,0.06)', color: '#d32f2f', border: '1px solid rgba(211,47,47,0.2)' }}>
                  {t('noPuedesModificarte')}
                </div>
              )}

              {/* Read-only fields */}
              <div className="grid grid-cols-2 gap-4">
                <ReadOnlyField label={t('campoEmail')} value={selected.email} />
                <ReadOnlyField label={t('campoDni')} value={selected.dni} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <ReadOnlyField label={t('campoEquipo')} value={selected.teamId ?? '—'} />
                <ReadOnlyField label={t('campoRegistrado')} value={selected.createdAt ? new Date(selected.createdAt).toLocaleDateString(dateLocale(locale)) : '—'} />
              </div>

              <hr style={{ borderColor: 'var(--color-outline-variant)' }} />

              {/* Annual fee trace */}
              <div className="rounded-[0.25rem] border px-4 py-4" style={{ borderColor: 'var(--color-outline-variant)' }}>
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>{t('cuotaAnual', { year: new Date().getFullYear() })}</p>
                    {quotaLoading ? <p className="mt-1 text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>{t('cargandoCuota')}</p>
                      : currentQuota?.paymentStatus === 'PAID' ? <>
                        <p className="mt-1 text-xs font-semibold text-green-700">{t('pagadaMonto', { amount: currentQuota.amount.toFixed(2) })}</p>
                        <p className="mt-1 text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {currentQuota.paymentSource === 'STRIPE' ? t('confirmadaStripe') : currentQuota.paymentSource === 'MANUAL' ? t('marcadaManualmente', { id: currentQuota.paidByUserId?.substring(0, 8) ?? '—' }) : t('pagoHistorico')}
                        </p>
                      </> : <p className="mt-1 text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>{t('pendienteOSinGenerar')}</p>}
                  </div>
                  {currentQuota?.paymentStatus !== 'PAID' && !quotaLoading && (
                    <button onClick={handleMarkCurrentQuotaPaid} disabled={markingQuota || isSelf(selected.id)}
                      className="shrink-0 px-3 py-2 rounded-[0.25rem] text-xs font-bold uppercase tracking-wider text-white disabled:opacity-50"
                      style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                      {markingQuota ? t('guardando') : t('marcarPagada')}
                    </button>
                  )}
                </div>
                {quotaActionError && <p className="mt-3 text-xs" style={{ color: 'var(--color-error)' }}>{quotaActionError}</p>}
              </div>

              <hr style={{ borderColor: 'var(--color-outline-variant)' }} />

              {/* Editable fields */}
              <div className="grid grid-cols-2 gap-4">
                <Field label={t('campoNombre')}>
                  <input
                    className={inputCls}
                    style={inputStyle}
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    disabled={isSelf(selected.id)}
                    maxLength={100}
                  />
                </Field>
                <Field label={t('campoApellidos')}>
                  <input
                    className={inputCls}
                    style={inputStyle}
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    disabled={isSelf(selected.id)}
                    maxLength={100}
                  />
                </Field>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <Field label={t('campoTelefono')}>
                  <input
                    className={inputCls}
                    style={inputStyle}
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    disabled={isSelf(selected.id)}
                    maxLength={20}
                    placeholder="+34 600 000 000"
                  />
                </Field>
                <Field label={t('campoFechaNacimiento')}>
                  <input
                    type="date"
                    className={inputCls}
                    style={inputStyle}
                    value={birthDate}
                    onChange={(e) => setBirthDate(e.target.value)}
                    disabled={isSelf(selected.id)}
                  />
                </Field>
              </div>

              {/* Role — only ADMIN can change */}
              {myRole === 'ADMIN' && (
                <Field label={t('campoRol')}>
                  <select
                    className={inputCls}
                    style={inputStyle}
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    disabled={isSelf(selected.id)}>
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </Field>
              )}

              {/* Blocked toggle */}
              <div className="flex items-center justify-between rounded-[0.25rem] border px-4 py-3"
                style={{ borderColor: 'var(--color-outline-variant)' }}>
                <div>
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>
                    {t('cuentaBloqueada')}
                  </p>
                  <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('cuentaBloqueadaHint')}
                  </p>
                </div>
                <button
                  type="button"
                  disabled={isSelf(selected.id)}
                  onClick={() => setBlocked((b) => !b)}
                  className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors focus:outline-none disabled:opacity-30 disabled:cursor-not-allowed ${blocked ? 'bg-[#d32f2f]' : 'bg-gray-300'}`}>
                  <span className={`inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform ${blocked ? 'translate-x-5' : 'translate-x-0'}`} />
                </button>
              </div>
            </div>

            {/* Footer */}
            <div className="px-6 py-4 border-t flex justify-end gap-3"
              style={{ borderColor: 'var(--color-outline-variant)' }}>
              <button
                onClick={closeDrawer}
                className="px-4 py-2.5 rounded-[0.25rem] text-sm font-bold uppercase tracking-widest border transition-colors hover:bg-black/5"
                style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                {t('cancelar')}
              </button>
              <button
                onClick={handleSave}
                disabled={saving || isSelf(selected.id)}
                className="flex items-center gap-2 px-4 py-2.5 rounded-[0.25rem] text-sm font-bold uppercase tracking-widest text-white transition-opacity hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                {saving
                  ? <><div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />{t('guardando')}</>
                  : t('guardarCambios')}
              </button>
            </div>
          </aside>
        </div>
      )}
    </div>
  )
}
