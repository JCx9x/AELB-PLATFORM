'use client'

import { useState, useEffect } from 'react'
import { useParams, useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../../components/Navbar'
import ChampionshipPricingSummary from '../../components/ChampionshipPricingSummary'
import {
  getChampionship,
  getCategories,
  getUserRegistrations,
  registerForChampionship,
  createStripeRegistrationCheckout,
  previewChampionshipRegistrationPayment,
  createChampionshipRegistrationCheckout,
  type Championship,
  type Category,
  type RegistrationItem,
  type ChampionshipRegistrationPaymentPreview,
} from '../../lib/api'
import { getSession, type Session } from '../../lib/auth'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function ChampionshipDetailClient() {
  const t            = useTranslations('CampeonatoDetalle')
  const locale       = useLocale()
  const SHIFT_LABEL: Record<string, string> = {
    MORNING:   t('turnoManana'),
    AFTERNOON: t('turnoTarde'),
  }
  const ARM_LABEL: Record<string, string> = {
    RIGHT: t('brazoDerecho'),
    LEFT:  t('brazoIzquierdo'),
    BOTH:  t('brazoAmbos'),
  }
  const params       = useParams()
  const router       = useRouter()
  const searchParams = useSearchParams()
  const id           = params.id as string
  const scrollTarget = searchParams.get('scroll')

  // Session loaded client-side only — evita discordancia en hidratación SSR
  const [session, setSession] = useState<Session | null>(null)

  useEffect(() => {
    setSession(getSession())
  }, [])

  // Datos públicos del campeonato
  const [championship,  setChampionship]  = useState<Championship | null>(null)
  const [allCategories, setAllCategories] = useState<Category[]>([])
  const [loading,       setLoading]       = useState(true)
  const [error,         setError]         = useState<string | null>(null)

  // Inscripciones del usuario para este campeonato
  const [myRegistrations, setMyRegistrations] = useState<RegistrationItem[]>([])

  // Estado de las acciones (inscribirse / cancelar)
  const [registering,  setRegistering]  = useState<string | null>(null)
  const [actionError,  setActionError]  = useState<string | null>(null)
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<string[]>([])
  const [basketTotal, setBasketTotal] = useState<number | null>(null)
  const [basketPreview, setBasketPreview] = useState<ChampionshipRegistrationPaymentPreview | null>(null)
  const [previewingPayment, setPreviewingPayment] = useState(false)
  const [payingBasket, setPayingBasket] = useState(false)
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false)
  const [acceptedImageRights, setAcceptedImageRights] = useState(false)

  // Carga de datos públicos (campeonato + categorías)
  useEffect(() => {
    setLoading(true)
    setError(null)
    Promise.all([getChampionship(id), getCategories()])
      .then(([ch, cats]) => {
        setChampionship(ch)
        setAllCategories(cats)
      })
      .catch((err: { detail?: string }) => {
        setError(err.detail ?? t('errorCarga'))
      })
      .finally(() => setLoading(false))
  }, [id, t])

  // Scroll al bloque de inscripción cuando viene de un enlace "Inscribirse"
  useEffect(() => {
    if (loading) return
    if (scrollTarget === 'inscripcion') {
      document.getElementById('inscripcion')?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [loading, scrollTarget])

  // Carga de inscripciones del usuario (se ejecuta cuando session está lista)
  useEffect(() => {
    if (!session) return
    getUserRegistrations(session.userId)
      .then(regs => setMyRegistrations(regs.filter(r => r.championshipId === id)))
      .catch(() => {}) // Silencioso: el usuario simplemente ve sin inscripciones
  }, [id, session])

  // El importe se recalcula automáticamente con la misma lógica del Checkout.
  useEffect(() => {
    let active = true
    if (!session || selectedCategoryIds.length === 0) {
      setBasketTotal(null)
      setBasketPreview(null)
      return () => { active = false }
    }

    setPreviewingPayment(true)
    previewChampionshipRegistrationPayment(id, selectedCategoryIds)
      .then((quote) => {
        if (!active) return
        setBasketTotal(quote.total)
        setBasketPreview(quote)
      })
      .catch((err: unknown) => {
        if (!active) return
        const e = err as { detail?: string }
        setBasketTotal(null)
        setBasketPreview(null)
        setActionError(e.detail ?? t('errorInscribirse'))
      })
      .finally(() => { if (active) setPreviewingPayment(false) })

    return () => { active = false }
  }, [id, selectedCategoryIds, session, t])

  function handleRegister(categoryId: string) {
    if (!session) {
      router.push(`/login?returnTo=${encodeURIComponent(`/campeonatos/${id}?scroll=inscripcion`)}`)
      return
    }
    setSelectedCategoryIds((current) => current.includes(categoryId) ? current.filter((item) => item !== categoryId) : [...current, categoryId])
  }

  async function handleBasketPayment() {
    setPayingBasket(true); setActionError(null)
    try { const checkout = await createChampionshipRegistrationCheckout(id, selectedCategoryIds); window.location.assign(checkout.checkoutUrl) }
    catch (err: unknown) { const e = err as { detail?: string }; setActionError(e.detail ?? 'No se ha podido iniciar el pago.'); setPayingBasket(false) }
  }

  async function handlePayRegistration(registrationId: string) {
    setRegistering(registrationId)
    setActionError(null)
    try {
      const checkout = await createStripeRegistrationCheckout(registrationId)
      window.location.assign(checkout.checkoutUrl)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setActionError(e.detail ?? 'No se ha podido iniciar el pago.')
      setRegistering(null)
    }
  }

  // ── Render: estados de carga ───────────────────────────────────────────────

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

  if (error || !championship) {
    return (
      <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
        <Navbar />
        <div className="max-w-lg mx-auto px-4 py-20 text-center">
          <p className="text-sm font-medium" style={{ color: 'var(--color-error)' }}>
            {error ?? t('noEncontrado')}
          </p>
          <Link href="/campeonatos"
            className="mt-4 inline-block px-6 py-2 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem]"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}>
            {t('volver')}
          </Link>
        </div>
      </div>
    )
  }

  // ── Datos derivados ────────────────────────────────────────────────────────

  const now              = new Date()
  const eventDate        = new Date(championship.eventDate)
  const deadlineDate     = new Date(championship.registrationDeadline)
  // Compara como cadena YYYY-MM-DD en hora local para que el día completo del deadline cuente
  const todayStr         = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const registrationOpen = todayStr <= championship.registrationDeadline && championship.visible
  const isPast           = eventDate < now

  const chCategories = allCategories.filter(c => championship.categoryIds.includes(c.id))
  const turno1       = chCategories.filter(c => c.shift === 'MORNING')
  const turno2       = chCategories.filter(c => c.shift === 'AFTERNOON')

  const registeredCategoryIds = new Set(myRegistrations.map(r => r.categoryId))
  const registrationById      = new Map(myRegistrations.map(r => [r.categoryId, r]))
  const selectedCategories    = allCategories.filter(category => selectedCategoryIds.includes(category.id))
  const summaryItems          = basketPreview?.items ?? selectedCategories

  // Devuelve si una categoría está disponible para inscripción (regla dos-brazos)
  function isCategoryAvailable(cat: Category): boolean {
    const shiftRegs = myRegistrations.filter(r => r.categoryShift === cat.shift)
    if (shiftRegs.length === 0) return true
    if (shiftRegs.length >= 2) return false
    // Exactamente 1 inscripción en el mismo turno: comprobar emparejamiento
    const existing = allCategories.find(c => c.id === shiftRegs[0].categoryId)
    if (!existing) return false
    const sameBase     = existing.gender      === cat.gender
                      && existing.weightLimit === cat.weightLimit
                      && existing.ageGroup    === cat.ageGroup
    const differentArm  = existing.armSide !== cat.armSide
    const neitherIsBoth = existing.armSide !== 'BOTH' && cat.armSide !== 'BOTH'
    return sameBase && differentArm && neitherIsBoth
  }

  function canSelectWithBasket(cat: Category): boolean {
    const sameShift = selectedCategories.filter(selected => selected.shift === cat.shift)
    if (sameShift.length === 0) return true
    if (sameShift.length >= 2) return false
    const selected = sameShift[0]
    const sameBase = selected.gender === cat.gender
      && selected.weightLimit === cat.weightLimit
      && selected.ageCategory === cat.ageCategory
    return sameBase
      && selected.armSide !== cat.armSide
      && selected.armSide !== 'BOTH'
      && cat.armSide !== 'BOTH'
  }

  // ── Render principal ───────────────────────────────────────────────────────

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-4xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        {/* Volver */}
        <Link href="/campeonatos"
          className="inline-flex items-center gap-1.5 text-sm font-medium mb-6"
          style={{ color: 'var(--color-on-surface-variant)' }}>
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
          {t('todosLosCampeonatos')}
        </Link>

        {/* Cabecera del campeonato */}
        <div className="rounded-[0.25rem] border overflow-hidden mb-6"
          style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

          {championship.imageUrl && (
            <div className="h-48 lg:h-64 overflow-hidden">
              <img src={championship.imageUrl} alt={championship.name} className="w-full h-full object-cover" />
            </div>
          )}

          <div className="p-6">
            <div className="flex items-center gap-2 mb-3">
              {isPast ? (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider"
                  style={{ backgroundColor: 'rgba(100,100,100,0.1)', color: 'var(--color-on-surface-variant)' }}>
                  {t('finalizado')}
                </span>
              ) : registrationOpen ? (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider"
                  style={{ backgroundColor: 'rgba(5,150,105,0.1)', color: '#047857' }}>
                  {t('inscripcionesAbiertas')}
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider"
                  style={{ backgroundColor: 'rgba(220,38,38,0.1)', color: 'var(--color-error)' }}>
                  {t('inscripcionesCerradas')}
                </span>
              )}
            </div>

            <h1 className="font-extrabold text-2xl lg:text-3xl mb-4"
              style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.02em' }}>
              {championship.name}
            </h1>

            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
              <InfoBlock label={t('infoFecha')}  value={eventDate.toLocaleDateString(dateLocale(locale), { day: 'numeric', month: 'long', year: 'numeric' })} />
              <InfoBlock label={t('infoLugar')}  value={championship.location} />
              <div>
                <p className="text-[10px] font-bold uppercase tracking-widest mb-1" style={{ color: 'var(--color-outline)' }}>{t('infoPrecio')}</p>
                <ChampionshipPricingSummary />
              </div>
              <InfoBlock label={t('infoPlazo')}  value={deadlineDate.toLocaleDateString(dateLocale(locale), { day: 'numeric', month: 'short', year: 'numeric' })} />
            </div>

            {championship.description && (
              <p className="text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
                {championship.description}
              </p>
            )}
          </div>
        </div>

        {/* Error de acción */}
        {actionError && (
          <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm font-medium mb-4"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {actionError}
          </div>
        )}

        {/* Categorías */}
        {chCategories.length === 0 ? (
          <p className="text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
            {t('sinCategorias')}
          </p>
        ) : (
          <div id="inscripcion" className="flex flex-col gap-6">

            {/* Título de sección */}
            {registrationOpen && !isPast && (
              <div>
                <h2 className="font-extrabold text-xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.02em' }}>
                  {t('inscripcionTitulo')}
                </h2>
                <p className="text-sm mt-1" style={{ color: 'var(--color-on-surface-variant)' }}>
                  {t('inscripcionSubtitulo')}
                </p>
              </div>
            )}

            {/* Aviso de inicio de sesión */}
            {!session && registrationOpen && !isPast && (
              <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm"
                style={{ backgroundColor: 'var(--color-surface-card)', border: '1px solid var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>
                  <Link href="/login" className="font-semibold underline" style={{ color: 'var(--color-primary-cta)' }}>
                    {t('iniciaSesion')}
                  </Link>
                  {' '}{t('paraInscribirte')}
                </span>
              </div>
            )}

            {[turno1, turno2].map((cats, idx) => {
              if (cats.length === 0) return null
              const shiftKey = idx === 0 ? 'MORNING' : 'AFTERNOON'
              return (
                <section key={shiftKey}>
                  <h2 className="text-[11px] font-bold uppercase tracking-widest mb-3"
                    style={{ color: 'var(--color-on-surface-variant)' }}>
                    {SHIFT_LABEL[shiftKey]}
                  </h2>
                  <div className="rounded-[0.25rem] border divide-y"
                    style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
                    {cats.map(cat => {
                      const reg        = registrationById.get(cat.id)
                      const registered = !!reg
                      const canRegister = !!session && !registered && registrationOpen && !isPast && isCategoryAvailable(cat)
                      const isBlocked   = !!session && !registered && registrationOpen && !isPast && !isCategoryAvailable(cat)
                      const needsLogin  = !session  && !registered && registrationOpen && !isPast
                      const selected    = selectedCategoryIds.includes(cat.id)
                      const selectionCompatible = selected || canSelectWithBasket(cat)
                      const canSelect = canRegister && selectionCompatible

                      return (
                        <div key={cat.id}
                          className="flex items-center justify-between px-5 py-4 gap-4"
                          style={{ borderColor: 'var(--color-outline-variant)' }}>

                          {/* Info de categoría */}
                          <div className="flex-1 min-w-0">
                            <p className="font-semibold text-sm" style={{ color: 'var(--color-on-surface)' }}>
                              {cat.name}
                            </p>
                            <p className="text-[11px] mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                              {ARM_LABEL[cat.armSide]}
                              {cat.weightLimit != null ? ` · ${cat.weightLimit} kg` : ''}
                              {cat.ageGroup ? ` · ${cat.ageGroup}` : ''}
                            </p>
                          </div>

                          {/* Acción */}
                          <div className="flex-shrink-0">
                            {registered && reg ? (
                              <div className="flex items-center gap-2">
                                <span className="px-2 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider"
                                  style={{ backgroundColor: 'rgba(5,150,105,0.1)', color: '#047857' }}>
                                  {reg.paymentStatus === 'PAID' ? t('inscrito') : 'Pago pendiente'}
                                </span>
                                {reg.paymentStatus === 'PENDING' && (
                                  <button
                                    onClick={() => handlePayRegistration(reg.id)}
                                    disabled={!!registering}
                                    className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider rounded-[0.25rem] text-white disabled:opacity-50"
                                    style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                                    {registering === reg.id ? 'Preparando…' : `Pagar ${reg.amount.toFixed(2)} €`}
                                  </button>
                                )}
                              </div>
                            ) : needsLogin ? null
                            : isBlocked || !selectionCompatible ? (
                              <span className="px-2 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider"
                                style={{ backgroundColor: 'rgba(100,100,100,0.08)', color: 'var(--color-outline)' }}>
                                {isBlocked ? t('noDisponible') : t('noCompatibleSeleccion')}
                              </span>
                            ) : canSelect ? (
                              <button
                                onClick={() => handleRegister(cat.id)}
                                disabled={selectedCategoryIds.length >= 4 && !selected}
                                className="px-4 py-2 text-xs font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-50"
                                style={{ backgroundColor: 'var(--color-primary-cta)' }}
                                onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = 'var(--color-primary)' }}
                                onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)' }}>
                                {selected ? 'Quitar' : 'Seleccionar'}
                              </button>
                            ) : null}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </section>
              )
            })}
            {selectedCategoryIds.length > 0 && (
              <section className="rounded-[0.25rem] border p-5" style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
                <h2 className="font-bold" style={{ color: 'var(--color-on-surface)' }}>Resumen de inscripción</h2>
                <p className="mt-1 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>{selectedCategoryIds.length} categoría{selectedCategoryIds.length === 1 ? '' : 's'} seleccionada{selectedCategoryIds.length === 1 ? '' : 's'}.</p>
                <ul className="mt-3 space-y-1 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
                  {summaryItems.map(category => (
                    <li key={'categoryId' in category ? category.categoryId : category.id}>
                      • {category.name} · {ARM_LABEL[category.armSide]}{category.weightLimit != null ? ` · ${category.weightLimit} kg` : ''}
                    </li>
                  ))}
                </ul>
                {previewingPayment && <p className="mt-3 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>Calculando total…</p>}
                {basketTotal != null && <p className="mt-3 text-lg font-extrabold" style={{ color: 'var(--color-on-surface)' }}>Total: {basketTotal.toFixed(2)} €</p>}

                {/* Consentimientos obligatorios antes de pagar */}
                <div className="mt-4 space-y-2.5">
                  <label className="flex items-start gap-2.5 text-sm cursor-pointer" style={{ color: 'var(--color-on-surface-variant)' }}>
                    <input type="checkbox" checked={acceptedPrivacy}
                      onChange={(e) => setAcceptedPrivacy(e.target.checked)}
                      className="mt-0.5 w-4 h-4 flex-shrink-0 rounded-[0.125rem] cursor-pointer accent-[#d32f2f]" />
                    <span>
                      {t('consentimientoPrivacidadPrefijo')}{' '}
                      <Link href="/privacidad" target="_blank" rel="noopener noreferrer"
                        className="font-semibold underline" style={{ color: 'var(--color-primary-cta)' }}>
                        {t('politicaPrivacidadLink')}
                      </Link>
                    </span>
                  </label>
                  <label className="flex items-start gap-2.5 text-sm cursor-pointer" style={{ color: 'var(--color-on-surface-variant)' }}>
                    <input type="checkbox" checked={acceptedImageRights}
                      onChange={(e) => setAcceptedImageRights(e.target.checked)}
                      className="mt-0.5 w-4 h-4 flex-shrink-0 rounded-[0.125rem] cursor-pointer accent-[#d32f2f]" />
                    <span>{t('consentimientoImagenTexto')}</span>
                  </label>
                </div>

                <div className="mt-4 flex gap-3">
                  {basketTotal != null && (
                    <button onClick={handleBasketPayment}
                      disabled={payingBasket || !acceptedPrivacy || !acceptedImageRights}
                      className="px-4 py-2 text-xs font-bold uppercase tracking-widest text-white rounded-[0.25rem] disabled:opacity-50 disabled:cursor-not-allowed"
                      style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                      {payingBasket ? 'Preparando…' : 'Realizar pago'}
                    </button>
                  )}
                </div>
              </section>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function InfoBlock({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-[10px] font-bold uppercase tracking-widest mb-0.5"
        style={{ color: 'var(--color-on-surface-variant)' }}>
        {label}
      </p>
      <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>
        {value}
      </p>
    </div>
  )
}
