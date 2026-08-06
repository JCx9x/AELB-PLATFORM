'use client'

import { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../components/Navbar'
import {
  getUserProfile, updateUserProfile, changePassword,
  getUserRegistrations,
  getMyQuotas, createStripeQuotaCheckout,
  logoutUser,
  type UserProfile, type RegistrationItem, type UserQuota,
} from '../lib/api'
import { getSession, clearSession, updateSession } from '../lib/auth'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

function initials(firstName: string, lastName: string): string {
  return ((firstName[0] ?? '') + (lastName[0] ?? '')).toUpperCase()
}

function formatDate(iso: string | null): string {
  if (!iso) return ''
  // back returns 'YYYY-MM-DD' from LocalDate
  return iso.substring(0, 10)
}

export default function PerfilPage() {
  const t = useTranslations('Perfil')
  const locale = useLocale()
  const router = useRouter()

  const QUICK_ACCESS = [
    { label: t('cuotasMembresia'), href: '/mis-cuotas', icon: CardIcon },
  ]
  const SHIFT_LABEL: Record<string, string> = {
    MORNING: t('turnoManana'),
    AFTERNOON: t('turnoTarde'),
  }
  const ARM_LABEL: Record<string, string> = {
    RIGHT: t('brazoDerecho'),
    LEFT:  t('brazoIzquierdo'),
    BOTH:  t('brazoAmbos'),
  }

  // ── Remote state ────────────────────────────────────────────────────────────
  const [profile,        setProfile]        = useState<UserProfile | null>(null)
  const [registrations,  setRegistrations]  = useState<RegistrationItem[]>([])
  const [quotas,         setQuotas]         = useState<UserQuota[]>([])
  const [loading,        setLoading]        = useState(true)
  const [loadError,      setLoadError]      = useState<string | null>(null)
  const [startingPayment, setStartingPayment] = useState(false)
  const [quotaMessage,    setQuotaMessage]    = useState<string | null>(null)

  // ── Form state (mirrors editable backend fields) ─────────────────────────
  const [firstName, setFirstName] = useState('')
  const [lastName,  setLastName]  = useState('')
  const [phone,     setPhone]     = useState('')
  const [birthDate, setBirthDate] = useState('')

  // ── UI state ────────────────────────────────────────────────────────────────
  const [saving,          setSaving]          = useState(false)
  const [saved,           setSaved]           = useState(false)
  const [saveError,       setSaveError]       = useState<string | null>(null)
  const [showDeactivate,  setShowDeactivate]  = useState(false)

  // ── Password change state ────────────────────────────────────────────────
  const [currentPwd,     setCurrentPwd]     = useState('')
  const [newPwd,         setNewPwd]         = useState('')
  const [confirmPwd,     setConfirmPwd]     = useState('')
  const [showCurrentPwd, setShowCurrentPwd] = useState(false)
  const [showNewPwd,     setShowNewPwd]     = useState(false)
  const [showConfirmPwd, setShowConfirmPwd] = useState(false)
  const [pwdSaving,      setPwdSaving]      = useState(false)
  const [pwdSaved,       setPwdSaved]       = useState(false)
  const [pwdError,       setPwdError]       = useState<string | null>(null)

  // ── Load profile ─────────────────────────────────────────────────────────
  const loadProfile = useCallback(async () => {
    const session = getSession()
    if (!session) {
      router.push('/login')
      return
    }

    try {
      const [data, regs, userQuotas] = await Promise.all([
        getUserProfile(session.userId),
        getUserRegistrations(session.userId),
        getMyQuotas(),
      ])
      setProfile(data)
      setRegistrations(regs)
      setQuotas(userQuotas)
      setFirstName(data.firstName)
      setLastName(data.lastName)
      setPhone(data.phone ?? '')
      setBirthDate(formatDate(data.birthDate))
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setLoadError(e.detail ?? t('errorCarga'))
    } finally {
      setLoading(false)
    }
  }, [router, t])

  useEffect(() => {
    loadProfile()
  }, [loadProfile])

  // ── Dirty check ──────────────────────────────────────────────────────────
  const isDirty = profile
    ? firstName !== profile.firstName
      || lastName  !== profile.lastName
      || phone     !== (profile.phone ?? '')
      || birthDate !== formatDate(profile.birthDate)
    : false

  // ── Discard ──────────────────────────────────────────────────────────────
  function handleDiscard() {
    if (!profile) return
    setFirstName(profile.firstName)
    setLastName(profile.lastName)
    setPhone(profile.phone ?? '')
    setBirthDate(formatDate(profile.birthDate))
    setSaveError(null)
  }

  // ── Save ─────────────────────────────────────────────────────────────────
  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    if (!profile) return

    setSaving(true)
    setSaveError(null)

    try {
      const updated = await updateUserProfile(profile.id, {
        firstName,
        lastName,
        phone:     phone     || null,
        birthDate: birthDate || null,
      })
      setProfile(updated)
      setFirstName(updated.firstName)
      setLastName(updated.lastName)
      setPhone(updated.phone ?? '')
      setBirthDate(formatDate(updated.birthDate))

      updateSession({ firstName: updated.firstName, lastName: updated.lastName })

      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setSaveError(e.detail ?? t('errorGuardar'))
    } finally {
      setSaving(false)
    }
  }

  // ── Change password ───────────────────────────────────────────────────────
  async function handleChangePassword(e: React.FormEvent) {
    e.preventDefault()
    setPwdError(null)

    if (newPwd !== confirmPwd) {
      setPwdError(t('errorPasswordsNuevasNoCoinciden'))
      return
    }
    if (newPwd.length < 8) {
      setPwdError(t('errorPasswordMinimo'))
      return
    }

    setPwdSaving(true)
    try {
      await changePassword(profile!.id, { currentPassword: currentPwd, newPassword: newPwd })
      setCurrentPwd('')
      setNewPwd('')
      setConfirmPwd('')
      setPwdSaved(true)
      setTimeout(() => setPwdSaved(false), 2500)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setPwdError(e.detail ?? t('errorCambiarPassword'))
    } finally {
      setPwdSaving(false)
    }
  }

  async function handleStartQuotaPayment() {
    const year = new Date().getFullYear()
    setStartingPayment(true)
    setQuotaMessage(null)
    try {
      const checkout = await createStripeQuotaCheckout(year)
      window.location.assign(checkout.checkoutUrl)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setQuotaMessage(e.detail ?? t('errorIniciarPago'))
    } finally {
      setStartingPayment(false)
    }
  }

  // ── Deactivate ────────────────────────────────────────────────────────────
  async function handleDeactivate() {
    try {
      await logoutUser()
    } finally {
      clearSession()
      router.push('/login')
    }
  }

  // ── Render states ─────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
        <Navbar />
        <div className="flex items-center justify-center py-32">
          <div className="w-8 h-8 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    )
  }

  if (loadError) {
    return (
      <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
        <Navbar />
        <div className="max-w-lg mx-auto px-4 py-20 text-center">
          <p className="text-sm font-medium" style={{ color: 'var(--color-error)' }}>{loadError}</p>
          <button
            onClick={loadProfile}
            className="mt-4 px-6 py-2 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem]"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}>
            {t('reintentar')}
          </button>
        </div>
      </div>
    )
  }

  if (!profile) return null

  const currentYear = new Date().getFullYear()
  const currentQuota = quotas.find((quota) => quota.year === currentYear)

  return (
    <div style={{ backgroundColor: 'var(--color-surface)' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">
        <div className="lg:grid lg:grid-cols-3 lg:gap-8">

          {/* ── Left column: identity card + quick access ── */}
          <aside className="lg:col-span-1 flex flex-col gap-5 mb-8 lg:mb-0">

            {/* Identity card */}
            <div className="rounded-[0.25rem] border overflow-hidden"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

              {/* Dark header */}
              <div className="px-6 py-8 flex flex-col items-center text-center"
                style={{ backgroundColor: 'var(--color-dark)' }}>
                <div className="w-20 h-20 rounded-full flex items-center justify-center text-white text-2xl font-extrabold mb-4"
                  style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                  {initials(profile.firstName, profile.lastName)}
                </div>

                <p className="text-white font-bold text-lg leading-tight">
                  {profile.firstName} {profile.lastName}
                </p>
              </div>

              {/* Stats grid */}
              <div className="grid grid-cols-2 divide-x border-t"
                style={{ borderColor: 'var(--color-outline-variant)' }}>
                {[
                  { label: t('idMiembro'),    value: profile.id.substring(0, 8).toUpperCase() },
                  { label: t('miembroDesde'), value: profile.createdAt.substring(0, 4) },
                ].map(({ label, value }) => (
                  <div key={label} className="px-4 py-3">
                    <p className="font-bold text-base leading-none" style={{ color: 'var(--color-on-surface)' }}>{value}</p>
                    <p className="text-[10px] uppercase tracking-wider mt-0.5" style={{ color: 'var(--color-outline)' }}>{label}</p>
                  </div>
                ))}
              </div>
            </div>

            {/* Quick access */}
            <div className="rounded-[0.25rem] border overflow-hidden"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
              <div className="px-5 py-4 border-b" style={{ borderColor: 'var(--color-outline-variant)' }}>
                <p className="text-[11px] font-bold uppercase tracking-widest" style={{ color: 'var(--color-on-surface)' }}>
                  {t('accesosRapidos')}
                </p>
              </div>
              <nav className="divide-y" style={{ borderColor: 'var(--color-outline-variant)' }}>
                {QUICK_ACCESS.map(({ label, href, icon: Icon }) => (
                  <Link key={href} href={href}
                    className="flex items-center gap-3 px-5 py-3.5 transition-colors group"
                    style={{ color: 'var(--color-on-surface-variant)' }}
                    onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-surface)')}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}>
                    <Icon className="w-4 h-4 flex-shrink-0" />
                    <span className="text-sm font-medium">{label}</span>
                    <svg className="w-3.5 h-3.5 ml-auto opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                    </svg>
                  </Link>
                ))}
              </nav>
            </div>
          </aside>

          {/* ── Right column: edit form ── */}
          <main className="lg:col-span-2 flex flex-col gap-5">

            {/* Annual membership fee */}
            <section className="rounded-[0.25rem] border overflow-hidden"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
              <div className="px-6 py-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b"
                style={{ borderColor: 'var(--color-outline-variant)' }}>
                <div>
                  <h1 className="font-bold text-xl" style={{ color: 'var(--color-on-surface)' }}>{t('cuotaAnual', { year: currentYear })}</h1>
                  <p className="text-sm mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('cuotaCalculo')}
                  </p>
                </div>
                {currentQuota?.paymentStatus === 'PAID' ? (
                  <span className="self-start sm:self-auto px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider" style={{ backgroundColor: '#dcfce7', color: '#15803d' }}>{t('pagada')}</span>
                ) : !currentQuota ? (
                  <span className="self-start sm:self-auto px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider" style={{ backgroundColor: 'var(--color-surface)', color: 'var(--color-on-surface-variant)' }}>{t('sinGenerar')}</span>
                ) : (
                  <span className="self-start sm:self-auto px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider" style={{ backgroundColor: '#fef3c7', color: '#92400e' }}>{t('pendienteDePago')}</span>
                )}
              </div>
              <div className="px-6 py-5">
                {currentQuota ? (
                  <p className="text-sm" style={{ color: 'var(--color-on-surface)' }}>
                    {currentQuota.ageCategoryLabel} · <strong>{currentQuota.amount.toFixed(2)} €</strong>
                  </p>
                ) : (
                  <p className="text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>{t('cuotaSinGenerarTexto')}</p>
                )}
                {quotaMessage && <p className="mt-3 text-sm" style={{ color: currentQuota ? '#92400e' : 'var(--color-error)' }}>{quotaMessage}</p>}
                {currentQuota?.paymentStatus !== 'PAID' && (
                  <button onClick={handleStartQuotaPayment} disabled={startingPayment}
                    className="mt-4 px-5 py-2.5 rounded-[0.25rem] text-sm font-bold uppercase tracking-widest text-white disabled:opacity-50"
                    style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                    {startingPayment ? t('preparandoPago') : currentQuota ? t('pagarCuota') : t('generarYPagarCuota')}
                  </button>
                )}
              </div>
            </section>

            {/* Success toast */}
            {saved && (
              <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm font-medium"
                style={{ backgroundColor: '#dcfce7', color: '#15803d' }}>
                <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                </svg>
                {t('perfilGuardado')}
              </div>
            )}

            {/* Error toast */}
            {saveError && (
              <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm font-medium"
                style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
                <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {saveError}
              </div>
            )}

            {/* Form card */}
            <form onSubmit={handleSave}
              className="rounded-[0.25rem] border overflow-hidden"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

              {/* Card header */}
              <div className="px-6 py-5 border-b flex items-center justify-between"
                style={{ borderColor: 'var(--color-outline-variant)' }}>
                <div>
                  <h1 className="font-bold text-xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                    {t('editarPerfil')}
                  </h1>
                  <p className="text-sm mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {profile.email}
                  </p>
                </div>
              </div>

              {/* Form body */}
              <div className="px-6 py-6 space-y-6">

                {/* Email (read-only) */}
                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('email')} <span className="normal-case font-normal tracking-normal text-[10px]"
                      style={{ color: 'var(--color-outline)' }}>{t('noEditable')}</span>
                  </label>
                  <input
                    type="email"
                    value={profile.email}
                    readOnly
                    className="w-full px-4 py-3 text-base rounded-[0.25rem] cursor-not-allowed"
                    style={{
                      backgroundColor: 'var(--color-surface)',
                      color: 'var(--color-outline)',
                      border: '1px solid var(--color-outline-variant)',
                    }}
                  />
                </div>

                {/* DNI (read-only) */}
                <div>
                  <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('dni')} <span className="normal-case font-normal tracking-normal text-[10px]"
                      style={{ color: 'var(--color-outline)' }}>{t('inmutable')}</span>
                  </label>
                  <input
                    type="text"
                    value={profile.dni}
                    readOnly
                    className="w-full px-4 py-3 text-base rounded-[0.25rem] cursor-not-allowed"
                    style={{
                      backgroundColor: 'var(--color-surface)',
                      color: 'var(--color-outline)',
                      border: '1px solid var(--color-outline-variant)',
                    }}
                  />
                  <p className="text-[11px] mt-1.5" style={{ color: 'var(--color-outline)' }}>
                    {t('dniHint')}
                  </p>
                </div>

                {/* Nombre + Apellidos */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="firstName"
                      className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                      style={{ color: 'var(--color-on-surface)' }}>
                      {t('nombre')}
                    </label>
                    <input
                      id="firstName"
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      autoComplete="given-name"
                      required
                      className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                        focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                      style={{
                        backgroundColor: 'var(--color-surface)',
                        color: 'var(--color-on-surface)',
                        border: '1px solid var(--color-outline)',
                      }}
                    />
                  </div>
                  <div>
                    <label htmlFor="lastName"
                      className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                      style={{ color: 'var(--color-on-surface)' }}>
                      {t('apellidos')}
                    </label>
                    <input
                      id="lastName"
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      autoComplete="family-name"
                      required
                      className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                        focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                      style={{
                        backgroundColor: 'var(--color-surface)',
                        color: 'var(--color-on-surface)',
                        border: '1px solid var(--color-outline)',
                      }}
                    />
                  </div>
                </div>

                {/* Teléfono */}
                <div>
                  <label htmlFor="phone"
                    className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('telefono')} <span className="normal-case font-normal tracking-normal text-[10px]"
                      style={{ color: 'var(--color-outline)' }}>{t('opcional')}</span>
                  </label>
                  <input
                    id="phone"
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    autoComplete="tel"
                    className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                      focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                    style={{
                      backgroundColor: 'var(--color-surface)',
                      color: 'var(--color-on-surface)',
                      border: '1px solid var(--color-outline)',
                    }}
                  />
                </div>

                {/* Fecha de nacimiento */}
                <div>
                  <label htmlFor="birthDate"
                    className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('fechaNacimiento')} <span className="normal-case font-normal tracking-normal text-[10px]"
                      style={{ color: 'var(--color-outline)' }}>{t('opcional')}</span>
                  </label>
                  <input
                    id="birthDate"
                    type="date"
                    value={birthDate}
                    onChange={(e) => setBirthDate(e.target.value)}
                    className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                      focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                    style={{
                      backgroundColor: 'var(--color-surface)',
                      color: birthDate ? 'var(--color-on-surface)' : 'var(--color-outline)',
                      border: '1px solid var(--color-outline)',
                    }}
                  />
                </div>
              </div>

              {/* Card footer: actions */}
              <div className="px-6 py-4 border-t flex items-center gap-3 flex-wrap"
                style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                <button
                  type="submit"
                  disabled={saving || !isDirty}
                  className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                  style={{ backgroundColor: 'var(--color-primary-cta)' }}
                  onMouseEnter={(e) => { if (!saving && isDirty) e.currentTarget.style.backgroundColor = 'var(--color-primary)' }}
                  onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)' }}>
                  {saving && (
                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  )}
                  {t('guardarCambios')}
                </button>
                <button
                  type="button"
                  onClick={handleDiscard}
                  disabled={saving || !isDirty}
                  className="px-6 py-3 text-sm font-bold uppercase tracking-widest rounded-[0.25rem] border-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  style={{ borderColor: 'var(--color-on-surface)', color: 'var(--color-on-surface)' }}
                  onMouseEnter={(e) => {
                    if (!saving && isDirty) {
                      e.currentTarget.style.backgroundColor = 'var(--color-on-surface)'
                      e.currentTarget.style.color = 'var(--color-surface-card)'
                    }
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = 'transparent'
                    e.currentTarget.style.color = 'var(--color-on-surface)'
                  }}>
                  {t('descartar')}
                </button>
              </div>
            </form>

            {/* ── Password change card ── */}
            <form onSubmit={handleChangePassword}
              className="rounded-[0.25rem] border overflow-hidden"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

              <div className="px-6 py-5 border-b" style={{ borderColor: 'var(--color-outline-variant)' }}>
                <h2 className="font-bold text-base" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                  {t('cambiarContrasena')}
                </h2>
                <p className="text-sm mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                  {t('cambiarContrasenaSubtitulo')}
                </p>
              </div>

              <div className="px-6 py-6 space-y-5">

                {/* Feedback banners */}
                {pwdSaved && (
                  <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm font-medium"
                    style={{ backgroundColor: '#dcfce7', color: '#15803d' }}>
                    <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                    {t('passwordActualizada')}
                  </div>
                )}
                {pwdError && (
                  <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm font-medium"
                    style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
                    <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    {pwdError}
                  </div>
                )}

                {/* Current password */}
                <div>
                  <label htmlFor="currentPwd"
                    className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('contrasenaActual')}
                  </label>
                  <div className="relative">
                    <input
                      id="currentPwd"
                      type={showCurrentPwd ? 'text' : 'password'}
                      value={currentPwd}
                      onChange={(e) => setCurrentPwd(e.target.value)}
                      autoComplete="current-password"
                      required
                      className="w-full px-4 py-3 pr-12 text-base rounded-[0.25rem] outline-none transition-all
                        focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                      style={{
                        backgroundColor: 'var(--color-surface)',
                        color: 'var(--color-on-surface)',
                        border: '1px solid var(--color-outline)',
                      }}
                    />
                    <button type="button" tabIndex={-1}
                      onClick={() => setShowCurrentPwd((v) => !v)}
                      className="absolute inset-y-0 right-3 flex items-center px-1"
                      style={{ color: 'var(--color-outline)' }}>
                      {showCurrentPwd ? <EyeOffIcon className="w-5 h-5" /> : <EyeIcon className="w-5 h-5" />}
                    </button>
                  </div>
                </div>

                {/* New password */}
                <div>
                  <label htmlFor="newPwd"
                    className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('nuevaContrasena')}
                  </label>
                  <div className="relative">
                    <input
                      id="newPwd"
                      type={showNewPwd ? 'text' : 'password'}
                      value={newPwd}
                      onChange={(e) => setNewPwd(e.target.value)}
                      autoComplete="new-password"
                      required
                      minLength={8}
                      className="w-full px-4 py-3 pr-12 text-base rounded-[0.25rem] outline-none transition-all
                        focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                      style={{
                        backgroundColor: 'var(--color-surface)',
                        color: 'var(--color-on-surface)',
                        border: '1px solid var(--color-outline)',
                      }}
                    />
                    <button type="button" tabIndex={-1}
                      onClick={() => setShowNewPwd((v) => !v)}
                      className="absolute inset-y-0 right-3 flex items-center px-1"
                      style={{ color: 'var(--color-outline)' }}>
                      {showNewPwd ? <EyeOffIcon className="w-5 h-5" /> : <EyeIcon className="w-5 h-5" />}
                    </button>
                  </div>
                  <p className="text-[11px] mt-1.5" style={{ color: 'var(--color-outline)' }}>
                    {t('minimo8Caracteres')}
                  </p>
                </div>

                {/* Confirm new password */}
                <div>
                  <label htmlFor="confirmPwd"
                    className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('confirmarNuevaContrasena')}
                  </label>
                  <div className="relative">
                    <input
                      id="confirmPwd"
                      type={showConfirmPwd ? 'text' : 'password'}
                      value={confirmPwd}
                      onChange={(e) => setConfirmPwd(e.target.value)}
                      autoComplete="new-password"
                      required
                      className="w-full px-4 py-3 pr-12 text-base rounded-[0.25rem] outline-none transition-all
                        focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                      style={{
                        backgroundColor: 'var(--color-surface)',
                        color: 'var(--color-on-surface)',
                        border: confirmPwd && newPwd !== confirmPwd
                          ? '1px solid var(--color-error)'
                          : '1px solid var(--color-outline)',
                      }}
                    />
                    <button type="button" tabIndex={-1}
                      onClick={() => setShowConfirmPwd((v) => !v)}
                      className="absolute inset-y-0 right-3 flex items-center px-1"
                      style={{ color: 'var(--color-outline)' }}>
                      {showConfirmPwd ? <EyeOffIcon className="w-5 h-5" /> : <EyeIcon className="w-5 h-5" />}
                    </button>
                  </div>
                  {confirmPwd && newPwd !== confirmPwd && (
                    <p className="text-[11px] mt-1.5" style={{ color: 'var(--color-error)' }}>
                      {t('passwordsNoCoinciden')}
                    </p>
                  )}
                </div>
              </div>

              <div className="px-6 py-4 border-t"
                style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                <button
                  type="submit"
                  disabled={pwdSaving || !currentPwd || !newPwd || !confirmPwd}
                  className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                  style={{ backgroundColor: 'var(--color-primary-cta)' }}
                  onMouseEnter={(e) => { if (!pwdSaving) e.currentTarget.style.backgroundColor = 'var(--color-primary)' }}
                  onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)' }}>
                  {pwdSaving && (
                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  )}
                  {t('actualizarContrasena')}
                </button>
              </div>
            </form>

            {/* ── Registrations ── */}
            <div className="rounded-[0.25rem] border overflow-hidden"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
              <div className="px-6 py-5 border-b flex items-center justify-between"
                style={{ borderColor: 'var(--color-outline-variant)' }}>
                <div>
                  <h2 className="font-bold text-base" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                    {t('misInscripciones')}
                  </h2>
                  <p className="text-sm mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('categoriasInscrito')}
                  </p>
                </div>
              </div>

              {registrations.length === 0 ? (
                <div className="px-6 py-8 text-center">
                  <p className="text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('sinInscripciones')}
                  </p>
                  <Link href="/campeonatos"
                    className="mt-3 inline-block text-sm font-semibold"
                    style={{ color: 'var(--color-primary-cta)' }}>
                    {t('verCampeonatos')} →
                  </Link>
                </div>
              ) : (
                <div className="divide-y" style={{ borderColor: 'var(--color-outline-variant)' }}>
                  {registrations.map(reg => (
                    <div key={reg.id} className="px-6 py-4 flex items-start justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <p className="font-semibold text-sm" style={{ color: 'var(--color-on-surface)' }}>
                          {reg.categoryName}
                        </p>
                        <p className="text-[11px] mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {reg.championshipName}
                        </p>
                        <div className="flex items-center gap-2 mt-1.5">
                          <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded"
                            style={{ backgroundColor: reg.categoryShift === 'MORNING' ? 'rgba(251,191,36,0.15)' : 'rgba(99,102,241,0.12)',
                                     color:           reg.categoryShift === 'MORNING' ? '#92400e' : '#4338ca' }}>
                            {SHIFT_LABEL[reg.categoryShift]}
                          </span>
                          <span className="text-[10px]" style={{ color: 'var(--color-outline)' }}>
                            {ARM_LABEL[reg.categoryArmSide]}
                          </span>
                          <span className="text-[10px]" style={{ color: 'var(--color-outline)' }}>
                            {new Date(reg.eventDate).toLocaleDateString(dateLocale(locale), { day: 'numeric', month: 'short', year: 'numeric' })}
                          </span>
                        </div>
                      </div>
                      <div className="flex-shrink-0 flex items-center gap-2">
                        <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full"
                          style={{
                            backgroundColor: reg.paymentStatus === 'PAID' ? 'rgba(5,150,105,0.1)' : 'rgba(245,158,11,0.12)',
                            color:           reg.paymentStatus === 'PAID' ? '#047857' : '#b45309',
                          }}>
                          {reg.paymentStatus === 'PAID' ? t('pagado') : t('pendiente')}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Danger zone */}
            <div className="rounded-[0.25rem] border overflow-hidden"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: '#fca5a5' }}>
              <div className="px-6 py-5 border-b" style={{ borderColor: '#fca5a5' }}>
                <p className="text-[11px] font-bold uppercase tracking-widest" style={{ color: 'var(--color-error)' }}>
                  {t('zonaDePeligro')}
                </p>
              </div>
              <div className="px-6 py-5 flex items-start justify-between gap-6">
                <div>
                  <p className="font-semibold text-sm mb-1" style={{ color: 'var(--color-on-surface)' }}>
                    {t('cerrarSesionDispositivo')}
                  </p>
                  <p className="text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('cerrarSesionDescripcion')}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setShowDeactivate((v) => !v)}
                  className="flex-shrink-0 px-4 py-2 text-xs font-bold uppercase tracking-widest rounded-[0.25rem] border-2 transition-colors"
                  style={{ borderColor: 'var(--color-error)', color: 'var(--color-error)' }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.backgroundColor = 'var(--color-error)'
                    e.currentTarget.style.color = 'white'
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = 'transparent'
                    e.currentTarget.style.color = 'var(--color-error)'
                  }}>
                  {t('cerrarSesion')}
                </button>
              </div>
              {showDeactivate && (
                <div className="px-6 pb-5">
                  <div className="rounded-[0.25rem] p-4 text-sm" style={{ backgroundColor: '#fef2f2', color: '#991b1b' }}>
                    {t('confirmarCerrarSesion')}
                    <div className="flex gap-3 mt-3">
                      <button type="button" onClick={handleDeactivate}
                        className="px-4 py-2 text-xs font-bold uppercase tracking-widest bg-[#ba1a1a] text-white rounded-[0.25rem]">
                        {t('siCerrarSesion')}
                      </button>
                      <button type="button" onClick={() => setShowDeactivate(false)}
                        className="px-4 py-2 text-xs font-bold uppercase tracking-widest border border-[#ba1a1a] text-[#ba1a1a] rounded-[0.25rem]">
                        {t('cancelar')}
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </main>
        </div>
      </div>
    </div>
  )
}

// ── Icons ─────────────────────────────────────────────────────────────────────
function EyeIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
    </svg>
  )
}

function EyeOffIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
    </svg>
  )
}

function CardIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
    </svg>
  )
}
