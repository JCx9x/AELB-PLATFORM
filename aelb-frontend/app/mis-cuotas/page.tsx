'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import Navbar from '../components/Navbar'
import { generateMyQuota, getMyQuotas, type ApiError, type UserQuota } from '../lib/api'
import { getSession } from '../lib/auth'

export default function MisCuotasPage() {
  const t = useTranslations('MisCuotas')
  const year = new Date().getFullYear()
  const [quotas, setQuotas] = useState<UserQuota[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)

  async function load() {
    if (!getSession()) return
    try { setQuotas(await getMyQuotas()) } catch (e) { setError((e as ApiError).detail) } finally { setLoading(false) }
  }
  // La carga inicial sincroniza este cliente estático con la API.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void load() }, [])
  async function createCurrent() {
    setCreating(true); setError(null)
    try { const quota = await generateMyQuota(year); setQuotas(current => [quota, ...current.filter(q => q.year !== year)]) }
    catch (e) { setError((e as ApiError).detail) }
    finally { setCreating(false) }
  }
  const current = quotas.find(q => q.year === year)
  return <div className="min-h-screen" style={{ backgroundColor: 'var(--color-surface)' }}>
    <Navbar />
    <main className="max-w-3xl mx-auto px-4 py-10">
      <Link href="/perfil" className="text-sm text-[#d32f2f]">← {t('volverPerfil')}</Link>
      <h1 className="mt-4 text-3xl font-extrabold">{t('titulo')}</h1>
      <p className="mt-2 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>{t('subtitulo')}</p>
      {error && <p className="mt-5 rounded bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {!loading && !current && <section className="mt-7 rounded-lg border bg-white p-6 shadow-sm">
        <h2 className="font-bold">{t('cuotaYear', { year })}</h2><p className="mt-1 text-sm text-gray-600">{t('generarSubtitulo')}</p>
        <button onClick={createCurrent} disabled={creating} className="mt-4 rounded bg-[#d32f2f] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">{creating ? t('generando') : t('generarCuota', { year })}</button>
      </section>}
      <section className="mt-7 space-y-3">
        {loading && <p>{t('cargandoCuotas')}</p>}
        {quotas.map(quota => <article key={quota.id} className="rounded-lg border bg-white p-5 shadow-sm flex items-center justify-between gap-4">
          <div><h2 className="font-bold">{t('cuotaYear', { year: quota.year })}</h2><p className="text-sm text-gray-600">{quota.ageCategoryLabel} · {quota.amount.toFixed(2)} €</p></div>
          <span className={`rounded-full px-3 py-1 text-xs font-bold ${quota.paymentStatus === 'PAID' ? 'bg-green-100 text-green-800' : 'bg-amber-100 text-amber-800'}`}>{quota.paymentStatus === 'PAID' ? t('pagada') : t('pendienteDePago')}</span>
        </article>)}
      </section>
    </main>
  </div>
}
