'use client'

import { useEffect, useState } from 'react'
import { useTranslations } from 'next-intl'
import Navbar from '../../components/Navbar'
import { getAnnualQuotaPrices, saveAnnualQuotaPrices, type AnnualQuotaPrice, type ApiError } from '../../lib/api'
import { getSession } from '../../lib/auth'

const CATEGORY_KEYS = [
  'SUB_JUNIOR', 'JUNIOR', 'YOUTH', 'SENIOR', 'MASTERS', 'GRAND_MASTERS', 'SENIOR_GRAND_MASTERS',
] as const

export default function AdminCuotasPage() {
  const t = useTranslations('AdminCuotas')
  const categoryInfo = t.raw('categorias') as Record<string, { label: string; range: string }>
  const [year, setYear] = useState(new Date().getFullYear())
  const [prices, setPrices] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  async function load(targetYear: number) {
    setLoading(true); setMessage(null)
    try { const data = await getAnnualQuotaPrices(targetYear); setPrices(Object.fromEntries(data.map((p: AnnualQuotaPrice) => [p.ageCategory, String(p.amount)]))) }
    catch (e) { setMessage((e as ApiError).detail) } finally { setLoading(false) }
  }
  // La configuración debe recargarse al cambiar de ejercicio.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { if (!getSession()) return; void load(year) }, [year])
  async function save() {
    setSaving(true); setMessage(null)
    try { await saveAnnualQuotaPrices(year, CATEGORY_KEYS.map((ageCategory) => ({ ageCategory, amount: Number(prices[ageCategory] ?? 0) }))); setMessage(t('preciosGuardados', { year })) }
    catch (e) { setMessage((e as ApiError).detail) } finally { setSaving(false) }
  }
  return <div className="min-h-screen" style={{ backgroundColor: 'var(--color-surface)' }}><Navbar />
    <main className="max-w-3xl mx-auto px-4 py-10"><h1 className="text-3xl font-extrabold">{t('titulo')}</h1><p className="mt-2 text-sm text-gray-600">{t('subtitulo')}</p>
      <div className="mt-6 flex items-center gap-3"><label htmlFor="year" className="font-semibold">{t('anio')}</label><input id="year" type="number" value={year} onChange={e => setYear(Number(e.target.value))} className="w-28 rounded border px-3 py-2" /></div>
      {message && <p className="mt-4 rounded bg-blue-50 p-3 text-sm text-blue-800">{message}</p>}
      <div className="mt-6 overflow-hidden rounded-lg border bg-white">{loading ? <p className="p-5">{t('cargando')}</p> : CATEGORY_KEYS.map((key) => <div key={key} className="flex items-center justify-between gap-5 border-b p-4 last:border-0"><div><p className="font-semibold">{categoryInfo[key].label}</p><p className="text-sm text-gray-500">{categoryInfo[key].range}</p></div><label className="flex items-center gap-2"><input aria-label={t('precioAria', { label: categoryInfo[key].label })} min="0" step="0.01" type="number" value={prices[key] ?? ''} onChange={e => setPrices(p => ({ ...p, [key]: e.target.value }))} className="w-28 rounded border px-2 py-1.5 text-right" /> €</label></div>)}</div>
      <button onClick={save} disabled={loading || saving} className="mt-6 rounded bg-[#d32f2f] px-5 py-2.5 font-semibold text-white disabled:opacity-60">{saving ? t('guardando') : t('guardarPrecios')}</button>
    </main></div>
}
