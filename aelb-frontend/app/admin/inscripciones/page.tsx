'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../../components/Navbar'
import {
  getAllChampionships,
  getChampionshipRegistrations,
  downloadChampionshipRegistrationsPdf,
  updatePaymentStatus,
  changeRegistrationCategory,
  getCategories,
  type Championship,
  type AdminRegistrationItem,
  type Category,
} from '../../lib/api'
import { getSession } from '../../lib/auth'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function InscripcionesAdminPage() {
  const t = useTranslations('AdminInscripciones')
  const locale = useLocale()
  const SHIFT_LABEL: Record<string, string> = {
    MORNING:   t('turnoManana'),
    AFTERNOON: t('turnoTarde'),
  }
  const ARM_LABEL: Record<string, string> = {
    RIGHT: t('brazoDerechoCorto'),
    LEFT:  t('brazoIzquierdoCorto'),
    BOTH:  t('brazoAmbos'),
  }
  const PAYMENT_LABEL: Record<string, string> = {
    PENDING:   t('pagoPendiente'),
    PAID:      t('pagoPagado'),
    CANCELLED: t('pagoCancelada'),
  }
  const router = useRouter()

  const [championships,  setChampionships]  = useState<Championship[]>([])
  const [selectedId,     setSelectedId]     = useState<string>('')
  const [registrations,  setRegistrations]  = useState<AdminRegistrationItem[]>([])
  const [categories,     setCategories]     = useState<Category[]>([])
  const [loadingCh,      setLoadingCh]      = useState(true)
  const [loadingRegs,    setLoadingRegs]    = useState(false)
  const [loadError,      setLoadError]      = useState<string | null>(null)
  const [actionError,    setActionError]    = useState<string | null>(null)
  const [updatingId,     setUpdatingId]     = useState<string | null>(null)
  const [changingCategoryId, setChangingCategoryId] = useState<string | null>(null)
  const [downloadingPdf, setDownloadingPdf] = useState(false)

  useEffect(() => {
    const session = getSession()
    if (!session) { router.push('/login'); return }
    if (session.role === 'USER') { router.push('/'); return }
  }, [router])

  useEffect(() => {
    setLoadingCh(true)
    getAllChampionships()
      .then(champs => {
        setChampionships(champs)
        if (champs.length > 0) setSelectedId(champs[0].id)
      })
      .catch((err: { detail?: string }) => setLoadError(err.detail ?? t('errorCargarCampeonatos')))
      .finally(() => setLoadingCh(false))
    getCategories().then(setCategories).catch(() => {})
  }, [t])

  const loadRegistrations = useCallback(async (championshipId: string) => {
    if (!championshipId) return
    setLoadingRegs(true)
    setActionError(null)
    try {
      const regs = await getChampionshipRegistrations(championshipId)
      setRegistrations(regs)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setActionError(e.detail ?? t('errorCargarInscripciones'))
      setRegistrations([])
    } finally {
      setLoadingRegs(false)
    }
  }, [t])

  useEffect(() => {
    if (selectedId) loadRegistrations(selectedId)
  }, [selectedId, loadRegistrations])

  async function handlePaymentToggle(reg: AdminRegistrationItem) {
    const newStatus = reg.paymentStatus === 'PAID' ? 'PENDING' : 'PAID'
    setUpdatingId(reg.id)
    setActionError(null)
    try {
      await updatePaymentStatus(reg.id, newStatus)
      setRegistrations(prev => prev.map(r => r.id === reg.id ? { ...r, paymentStatus: newStatus } : r))
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setActionError(e.detail ?? t('errorActualizarPago'))
    } finally {
      setUpdatingId(null)
    }
  }

  async function handleCategoryChange(reg: AdminRegistrationItem, newCategoryId: string) {
    if (newCategoryId === reg.categoryId) return
    setChangingCategoryId(reg.id)
    setActionError(null)
    try {
      await changeRegistrationCategory(reg.id, newCategoryId)
      await loadRegistrations(selectedId)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setActionError(e.detail ?? t('errorCambiarCategoria'))
    } finally {
      setChangingCategoryId(null)
    }
  }

  async function handleDownloadPdf() {
    if (!selectedId) return
    setDownloadingPdf(true)
    setActionError(null)
    try { await downloadChampionshipRegistrationsPdf(selectedId) }
    catch (err: unknown) { const e = err as { detail?: string }; setActionError(e.detail ?? t('errorDescargarPdf')) }
    finally { setDownloadingPdf(false) }
  }

  const selectedChampionship = championships.find(ch => ch.id === selectedId)
  const availableCategories = selectedChampionship
    ? categories.filter(cat => selectedChampionship.categoryIds.includes(cat.id))
    : []

  // Group by shift
  const turno1 = registrations.filter(r => r.categoryShift === 'MORNING')
  const turno2 = registrations.filter(r => r.categoryShift === 'AFTERNOON')

  const stats = {
    total:   registrations.length,
    paid:    registrations.filter(r => r.paymentStatus === 'PAID').length,
    pending: registrations.filter(r => r.paymentStatus === 'PENDING').length,
  }

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        <div className="mb-8">
          <h1 className="font-extrabold text-3xl lg:text-4xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.03em' }}>
            {t('titulo')}
          </h1>
          <p className="mt-2 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
            {t('subtitulo')}
          </p>
        </div>

        {/* Championship selector */}
        <div className="mb-6 flex flex-wrap items-end gap-3">
          <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
            style={{ color: 'var(--color-on-surface-variant)' }}>
            {t('campeonato')}
          </label>
          {loadingCh ? (
            <div className="w-6 h-6 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
          ) : (
            <select
              value={selectedId}
              onChange={e => setSelectedId(e.target.value)}
              className="w-full sm:w-auto px-3 py-2 rounded-[0.25rem] border text-sm font-medium"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface)' }}>
              {championships.map(ch => (
                <option key={ch.id} value={ch.id}>{ch.name}</option>
              ))}
            </select>
          )}
          <button onClick={handleDownloadPdf} disabled={!selectedId || downloadingPdf}
            className="px-4 py-2 text-xs font-bold uppercase tracking-widest rounded-[0.25rem] text-white disabled:opacity-50"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}>
            {downloadingPdf ? t('generandoPdf') : t('descargarPdf')}
          </button>
        </div>

        {loadError && (
          <div className="flex items-center gap-2 px-4 py-3 rounded-[0.25rem] text-sm mb-6"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            {loadError}
          </div>
        )}

        {actionError && (
          <div className="flex items-center gap-2 px-4 py-3 rounded-[0.25rem] text-sm mb-4"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            {actionError}
          </div>
        )}

        {loadingRegs ? (
          <div className="flex items-center justify-center py-24">
            <div className="w-8 h-8 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <>
            {/* Stats bar */}
            {registrations.length > 0 && (
              <div className="flex gap-4 mb-6">
                <StatChip label={t('statTotal')} value={stats.total} color="var(--color-on-surface-variant)" />
                <StatChip label={t('statPagados')} value={stats.paid} color="#047857" bg="rgba(5,150,105,0.1)" />
                <StatChip label={t('statPendientes')} value={stats.pending} color="#b45309" bg="rgba(217,119,6,0.1)" />
              </div>
            )}

            {/* No registrations */}
            {registrations.length === 0 && !loadingRegs && selectedId && (
              <div className="text-center py-20">
                <p className="text-sm font-medium" style={{ color: 'var(--color-on-surface-variant)' }}>
                  {t('sinInscripciones')}
                </p>
              </div>
            )}

            {/* Tables per shift */}
            {[{ key: 'MORNING', rows: turno1 }, { key: 'AFTERNOON', rows: turno2 }].map(({ key, rows }) => {
              if (rows.length === 0) return null
              return (
                <section key={key} className="mb-8">
                  <h2 className="text-[11px] font-bold uppercase tracking-widest mb-3"
                    style={{ color: 'var(--color-on-surface-variant)' }}>
                    {SHIFT_LABEL[key]}
                  </h2>
                  <div className="rounded-[0.25rem] border overflow-x-auto"
                    style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
                    <table className="w-full text-sm min-w-[640px]">
                      <thead>
                        <tr className="border-b" style={{ borderColor: 'var(--color-outline-variant)' }}>
                          {(t.raw('tableHeaders') as string[]).map(h => (
                            <th key={h} className="px-4 py-3 text-left text-[10px] font-bold uppercase tracking-wider"
                              style={{ color: 'var(--color-on-surface-variant)' }}>
                              {h}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody className="divide-y" style={{ borderColor: 'var(--color-outline-variant)' }}>
                        {rows.map(reg => (
                          <tr key={reg.id} style={{ borderColor: 'var(--color-outline-variant)' }}>
                            <td className="px-4 py-3 font-medium whitespace-nowrap" style={{ color: 'var(--color-on-surface)' }}>
                              {reg.userFirstName} {reg.userLastName}
                            </td>
                            <td className="px-4 py-3 font-mono text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                              {reg.userDni}
                            </td>
                            <td className="px-4 py-3 text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                              {reg.userEmail}
                            </td>
                            <td className="px-4 py-3 whitespace-nowrap" style={{ color: 'var(--color-on-surface)' }}>
                              {reg.categoryName}
                              <span className="ml-1.5 text-[10px]" style={{ color: 'var(--color-on-surface-variant)' }}>
                                ({ARM_LABEL[reg.categoryArmSide]})
                              </span>
                            </td>
                            <td className="px-4 py-3 text-xs whitespace-nowrap" style={{ color: 'var(--color-on-surface-variant)' }}>
                              {new Date(reg.createdAt).toLocaleDateString(dateLocale(locale), { day: 'numeric', month: 'short' })}
                            </td>
                            <td className="px-4 py-3">
                              <button
                                onClick={() => handlePaymentToggle(reg)}
                                disabled={updatingId === reg.id || reg.paymentStatus === 'CANCELLED'}
                                className="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider transition-opacity disabled:opacity-50"
                                style={{
                                  backgroundColor: reg.paymentStatus === 'PAID'
                                    ? 'rgba(5,150,105,0.1)'
                                    : reg.paymentStatus === 'CANCELLED'
                                    ? 'rgba(100,100,100,0.1)'
                                    : 'rgba(217,119,6,0.1)',
                                  color: reg.paymentStatus === 'PAID'
                                    ? '#047857'
                                    : reg.paymentStatus === 'CANCELLED'
                                    ? 'var(--color-on-surface-variant)'
                                    : '#b45309',
                                }}>
                                {updatingId === reg.id
                                  ? <span className="w-3 h-3 border-2 border-current border-t-transparent rounded-full animate-spin inline-block" />
                                  : PAYMENT_LABEL[reg.paymentStatus]}
                              </button>
                            </td>
                            <td className="px-4 py-3">
                              <select
                                value={reg.categoryId}
                                onChange={e => handleCategoryChange(reg, e.target.value)}
                                disabled={changingCategoryId === reg.id || reg.paymentStatus === 'CANCELLED' || availableCategories.length === 0}
                                className="px-2 py-1.5 rounded-[0.25rem] border text-xs font-medium disabled:opacity-50"
                                style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface)' }}>
                                {!availableCategories.some(cat => cat.id === reg.categoryId) && (
                                  <option value={reg.categoryId}>{reg.categoryName}</option>
                                )}
                                {availableCategories.map(cat => (
                                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                                ))}
                              </select>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </section>
              )
            })}
          </>
        )}
      </div>
    </div>
  )
}

function StatChip({ label, value, color, bg }: { label: string; value: number; color: string; bg?: string }) {
  return (
    <div className="flex items-center gap-2 px-3 py-2 rounded-[0.25rem] border"
      style={{ backgroundColor: bg ?? 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
      <span className="text-[11px] font-bold uppercase tracking-wider" style={{ color: 'var(--color-on-surface-variant)' }}>
        {label}
      </span>
      <span className="font-extrabold text-base" style={{ color }}>
        {value}
      </span>
    </div>
  )
}
