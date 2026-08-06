'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'
import Navbar from '../../components/Navbar'
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  type Category,
  type CategoryData,
} from '../../lib/api'
import { getSession } from '../../lib/auth'

const AGE_OPTION_IDS = [
  'SUB_JUNIOR', 'JUNIOR', 'YOUTH', 'AMATEUR', 'SENIOR', 'MASTER',
  'GRAND_MASTER', 'SENIOR_GRAND_MASTER', 'SUPER_SENIOR_GRAND_MASTER',
] as const

// ── Empty form ─────────────────────────────────────────────────────────────────

const EMPTY_FORM: CategoryData = {
  gender:      'MALE',
  armSide:     'RIGHT',
  weightLimit: null,
  ageGroup:    null,
  ageCategory: 'SENIOR',
  shift:       'AFTERNOON',
}

// ── Component ──────────────────────────────────────────────────────────────────

export default function CategoriasPage() {
  const t = useTranslations('AdminCategorias')
  const router = useRouter()

  const GENDER_LABELS: Record<string, string> = {
    MALE:   t('generoMasculino'),
    FEMALE: t('generoFemenino'),
    OPEN:   t('generoAbierto'),
  }
  const ARM_LABELS: Record<string, string> = {
    RIGHT: t('brazoDerecho'),
    LEFT:  t('brazoIzquierdo'),
    BOTH:  t('brazoAmbos'),
  }
  const ageOptionInfo = t.raw('edadOpciones') as Record<string, { label: string; detail: string }>
  const AGE_OPTIONS = AGE_OPTION_IDS.map((id) => ({ id, ...ageOptionInfo[id] }))

  const [categories,  setCategories]  = useState<Category[]>([])
  const [loading,     setLoading]     = useState(true)
  const [loadError,   setLoadError]   = useState<string | null>(null)

  // panel state
  const [panelOpen,   setPanelOpen]   = useState(false)
  const [editing,     setEditing]     = useState<Category | null>(null)
  const [form,        setForm]        = useState<CategoryData>(EMPTY_FORM)
  const [saving,      setSaving]      = useState(false)
  const [formError,   setFormError]   = useState<string | null>(null)

  const generatedTitle = `${AGE_OPTIONS.find((item) => item.id === form.ageCategory)?.label ?? form.ageGroup ?? ''} ${GENDER_LABELS[form.gender]} ${ARM_LABELS[form.armSide]}${form.weightLimit == null ? '' : ` -${Number(form.weightLimit)}kg`}`.trim()

  // delete confirm
  const [toDelete,    setToDelete]    = useState<Category | null>(null)
  const [deleting,    setDeleting]    = useState(false)

  // ── Auth guard ────────────────────────────────────────────────────────────
  useEffect(() => {
    const session = getSession()
    if (!session) { router.push('/login'); return }
    if (session.role === 'USER') { router.push('/'); return }
  }, [router])

  // ── Load ──────────────────────────────────────────────────────────────────
  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      setCategories(await getCategories())
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setLoadError(e.detail ?? t('errorCargar'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => { load() }, [load])

  // ── Panel helpers ─────────────────────────────────────────────────────────
  function openCreate() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError(null)
    setPanelOpen(true)
  }

  function openEdit(cat: Category) {
    setEditing(cat)
    setForm({
      gender:      cat.gender,
      armSide:     cat.armSide,
      weightLimit: cat.weightLimit,
      ageGroup:    cat.ageGroup,
      ageCategory: cat.ageCategory,
      shift:       cat.shift,
    })
    setFormError(null)
    setPanelOpen(true)
  }

  function closePanel() {
    setPanelOpen(false)
    setEditing(null)
    setFormError(null)
  }

  function setField<K extends keyof CategoryData>(key: K, value: CategoryData[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function setAgeCategory(ageCategory: CategoryData['ageCategory']) {
    const option = AGE_OPTIONS.find((item) => item.id === ageCategory)
    if (!option) return
    setForm((prev) => ({ ...prev, ageCategory, ageGroup: ageCategory, shift: ageCategory === 'SENIOR' ? 'AFTERNOON' : 'MORNING' }))
  }

  // ── Submit ────────────────────────────────────────────────────────────────
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      if (editing) {
        const updated = await updateCategory(editing.id, form)
        setCategories((prev) => prev.map((c) => (c.id === updated.id ? updated : c)))
      } else {
        const created = await createCategory(form)
        setCategories((prev) => [...prev, created])
      }
      closePanel()
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setFormError(e.detail ?? t('errorGuardar'))
    } finally {
      setSaving(false)
    }
  }

  // ── Delete ────────────────────────────────────────────────────────────────
  async function handleDelete() {
    if (!toDelete) return
    setDeleting(true)
    try {
      await deleteCategory(toDelete.id)
      setCategories((prev) => prev.filter((c) => c.id !== toDelete.id))
      setToDelete(null)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setLoadError(e.detail ?? t('errorEliminar'))
      setToDelete(null)
    } finally {
      setDeleting(false)
    }
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-10">

        {/* Page header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="font-extrabold text-2xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.02em' }}>
              {t('titulo')}
            </h1>
            <p className="text-sm mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('countRegistradas', { count: categories.length })}
            </p>
          </div>
          <button
            onClick={openCreate}
            className="flex items-center gap-2 px-5 py-2.5 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
            <PlusIcon className="w-4 h-4" />
            {t('nuevaCategoria')}
          </button>
        </div>

        {/* Error banner */}
        {loadError && (
          <div className="mb-4 flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm font-medium"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            <ExclamationIcon className="w-4 h-4 flex-shrink-0" />
            {loadError}
          </div>
        )}

        {/* Table card */}
        <div className="rounded-[0.25rem] border overflow-hidden"
          style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

          {loading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-7 h-7 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : categories.length === 0 ? (
            <div className="text-center py-20">
              <p className="text-sm font-medium" style={{ color: 'var(--color-on-surface-variant)' }}>
                {t('sinCategorias')}
              </p>
              <button onClick={openCreate}
                className="mt-3 text-sm font-bold underline"
                style={{ color: 'var(--color-primary-cta)' }}>
                {t('crearPrimera')}
              </button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b" style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                    {(t.raw('tableHeaders') as string[]).map((h) => (
                      <th key={h} className="px-5 py-3 text-left text-[11px] font-bold uppercase tracking-widest"
                        style={{ color: 'var(--color-on-surface-variant)' }}>
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y" style={{ borderColor: 'var(--color-outline-variant)' }}>
                  {categories.map((cat) => (
                    <tr key={cat.id} className="group transition-colors"
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-surface)')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}>
                      <td className="px-5 py-3.5 font-semibold" style={{ color: 'var(--color-on-surface)' }}>
                        {cat.name}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="px-2 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider"
                          style={{
                            backgroundColor: cat.gender === 'MALE' ? 'rgba(37,99,235,0.1)' : cat.gender === 'FEMALE' ? 'rgba(219,39,119,0.1)' : 'rgba(5,150,105,0.1)',
                            color:           cat.gender === 'MALE' ? '#1d4ed8' : cat.gender === 'FEMALE' ? '#be185d' : '#047857',
                          }}>
                          {GENDER_LABELS[cat.gender]}
                        </span>
                      </td>
                      <td className="px-5 py-3.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {ARM_LABELS[cat.armSide]}
                      </td>
                      <td className="px-5 py-3.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {cat.weightLimit != null ? `${cat.weightLimit} kg` : <span className="text-[11px] uppercase tracking-wider opacity-50">{t('open')}</span>}
                      </td>
                      <td className="px-5 py-3.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {cat.ageGroup ?? <span className="text-[11px] uppercase tracking-wider opacity-50">—</span>}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
                          style={cat.shift === 'MORNING'
                            ? { backgroundColor: 'rgba(245,158,11,0.12)', color: '#b45309' }
                            : { backgroundColor: 'rgba(99,102,241,0.12)', color: '#4338ca' }}>
                          {cat.shift === 'MORNING' ? `☀ ${t('turnoManana')}` : `🌙 ${t('turnoTarde')}`}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => openEdit(cat)}
                            className="p-1.5 rounded transition-colors"
                            style={{ color: 'var(--color-on-surface-variant)' }}
                            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-on-surface)')}
                            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-on-surface-variant)')}
                            aria-label={t('editar')}>
                            <PencilIcon className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => setToDelete(cat)}
                            className="p-1.5 rounded transition-colors"
                            style={{ color: 'var(--color-outline)' }}
                            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-error)')}
                            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-outline)')}
                            aria-label={t('eliminar')}>
                            <TrashIcon className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* ── Side panel: create / edit ────────────────────────────────────── */}
      {panelOpen && (
        <>
          {/* Backdrop */}
          <div className="fixed inset-0 z-40 bg-black/40" onClick={closePanel} />

          {/* Drawer */}
          <aside className="fixed inset-y-0 right-0 z-50 w-full max-w-md flex flex-col shadow-2xl"
            style={{ backgroundColor: 'var(--color-surface-card)' }}>

            {/* Header */}
            <div className="flex items-center justify-between px-6 py-5 border-b"
              style={{ borderColor: 'var(--color-outline-variant)' }}>
              <h2 className="font-bold text-lg" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                {editing ? t('editarCategoria') : t('nuevaCategoria')}
              </h2>
              <button onClick={closePanel}
                className="p-1.5 rounded transition-colors"
                style={{ color: 'var(--color-outline)' }}
                onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-on-surface)')}
                onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-outline)')}>
                <XIcon className="w-5 h-5" />
              </button>
            </div>

            {/* Form */}
            <form id="cat-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto px-6 py-6 space-y-5">

              {formError && (
                <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm"
                  style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
                  <ExclamationIcon className="w-4 h-4 flex-shrink-0" />
                  {formError}
                </div>
              )}

              {/* Título generado */}
              <div>
                <label
                  className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('tituloGenerado')}
                </label>
                <div
                  className="w-full px-4 py-3 text-base rounded-[0.25rem]"
                  style={{
                    backgroundColor: 'var(--color-surface-container)',
                    color: 'var(--color-on-surface)',
                    border: '1px solid var(--color-outline)',
                  }}
                >
                  {generatedTitle}
                </div>
              </div>

              {/* Género */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoGenero')} <span style={{ color: 'var(--color-error)' }}>*</span>
                </label>
                <div className="flex gap-2">
                  {(['MALE', 'FEMALE', 'OPEN'] as const).map((g) => (
                    <button key={g} type="button"
                      onClick={() => setField('gender', g)}
                      className="flex-1 py-2.5 text-xs font-bold uppercase tracking-widest rounded-[0.25rem] border-2 transition-colors"
                      style={{
                        borderColor:     form.gender === g ? 'var(--color-primary-cta)' : 'var(--color-outline)',
                        backgroundColor: form.gender === g ? 'var(--color-primary-cta)' : 'transparent',
                        color:           form.gender === g ? 'white' : 'var(--color-on-surface-variant)',
                      }}>
                      {GENDER_LABELS[g]}
                    </button>
                  ))}
                </div>
              </div>

              {/* Brazo */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoBrazo')} <span style={{ color: 'var(--color-error)' }}>*</span>
                </label>
                <div className="flex gap-2">
                  {(['RIGHT', 'LEFT', 'BOTH'] as const).map((a) => (
                    <button key={a} type="button"
                      onClick={() => setField('armSide', a)}
                      className="flex-1 py-2.5 text-xs font-bold uppercase tracking-widest rounded-[0.25rem] border-2 transition-colors"
                      style={{
                        borderColor:     form.armSide === a ? 'var(--color-primary-cta)' : 'var(--color-outline)',
                        backgroundColor: form.armSide === a ? 'var(--color-primary-cta)' : 'transparent',
                        color:           form.armSide === a ? 'white' : 'var(--color-on-surface-variant)',
                      }}>
                      {ARM_LABELS[a]}
                    </button>
                  ))}
                </div>
              </div>

              {/* Categoría WAF: el turno se deriva de esta selección. */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoCategoriaEdad')} <span style={{ color: 'var(--color-error)' }}>*</span>
                </label>
                <div className="grid grid-cols-1 gap-2">
                  {AGE_OPTIONS.map((option) => (
                    <button key={option.id} type="button"
                      onClick={() => setAgeCategory(option.id)}
                      className="px-3 py-2.5 text-left text-xs font-bold tracking-widest rounded-[0.25rem] border-2 transition-colors flex items-center justify-between gap-2"
                      style={{
                        borderColor:     form.ageCategory === option.id
                          ? (option.id === 'SENIOR' ? '#4338ca' : '#b45309')
                          : 'var(--color-outline)',
                        backgroundColor: form.ageCategory === option.id
                          ? (option.id === 'SENIOR' ? 'rgba(99,102,241,0.12)' : 'rgba(245,158,11,0.12)')
                          : 'transparent',
                        color:           form.ageCategory === option.id
                          ? (option.id === 'SENIOR' ? '#4338ca' : '#b45309')
                          : 'var(--color-on-surface-variant)',
                      }}>
                      <span>{option.label}</span>
                      <span className="font-normal normal-case text-[10px]">{option.detail} · {option.id === 'SENIOR' ? t('turnoTarde') : t('turnoManana')}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Peso máximo */}
              <div>
                <label htmlFor="cat-weight"
                  className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoPesoMaximo')} <span className="normal-case font-normal tracking-normal text-[10px]"
                    style={{ color: 'var(--color-outline)' }}>{t('pesoHint')}</span>
                </label>
                <input
                  id="cat-weight"
                  type="number"
                  min={0}
                  max={500}
                  step={0.5}
                  value={form.weightLimit ?? ''}
                  onChange={(e) => setField('weightLimit', e.target.value === '' ? null : Number(e.target.value))}
                  placeholder={t('placeholderPeso')}
                  className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                    focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                  style={{
                    backgroundColor: 'var(--color-surface)',
                    color: 'var(--color-on-surface)',
                    border: '1px solid var(--color-outline)',
                  }}
                />
              </div>

              {/* Identificador usado por las reglas de precios heredadas */}
              <div>
                <label htmlFor="cat-age"
                  className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoIdentificador')} <span className="normal-case font-normal tracking-normal text-[10px]"
                    style={{ color: 'var(--color-outline)' }}>{t('opcional')}</span>
                </label>
                <input
                  id="cat-age"
                  type="text"
                  value={form.ageGroup ?? ''}
                  onChange={(e) => setField('ageGroup', e.target.value || null)}
                  maxLength={50}
                  placeholder={t('placeholderIdentificador')}
                  className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                    focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                  style={{
                    backgroundColor: 'var(--color-surface)',
                    color: 'var(--color-on-surface)',
                    border: '1px solid var(--color-outline)',
                  }}
                />
              </div>
            </form>

            {/* Footer */}
            <div className="px-6 py-4 border-t flex items-center gap-3"
              style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
              <button
                type="submit"
                form="cat-form"
                disabled={saving || !form.ageCategory}
                className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}
                onMouseEnter={(e) => { if (!saving) e.currentTarget.style.backgroundColor = 'var(--color-primary)' }}
                onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)' }}>
                {saving && <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
                {editing ? t('guardarCambios') : t('crearCategoria')}
              </button>
              <button type="button" onClick={closePanel}
                disabled={saving}
                className="px-6 py-3 text-sm font-bold uppercase tracking-widest rounded-[0.25rem] border-2 transition-colors disabled:opacity-40"
                style={{ borderColor: 'var(--color-on-surface)', color: 'var(--color-on-surface)' }}>
                {t('cancelar')}
              </button>
            </div>
          </aside>
        </>
      )}

      {/* ── Delete confirmation dialog ────────────────────────────────────── */}
      {toDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
          <div className="rounded-[0.25rem] border w-full max-w-sm p-6 shadow-2xl"
            style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
            <h3 className="font-bold text-base mb-2" style={{ color: 'var(--color-on-surface)' }}>
              {t('confirmEliminarTitulo')}
            </h3>
            <p className="text-sm mb-5" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('confirmEliminarPrefijo')} <strong className="font-semibold" style={{ color: 'var(--color-on-surface)' }}>
                {toDelete.name}
              </strong>. {t('confirmEliminarSufijo')}
            </p>
            <div className="flex gap-3">
              <button
                onClick={handleDelete}
                disabled={deleting}
                className="flex-1 py-2.5 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] flex items-center justify-center gap-2 disabled:opacity-50"
                style={{ backgroundColor: 'var(--color-error)' }}>
                {deleting && <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
                {t('eliminar')}
              </button>
              <button
                onClick={() => setToDelete(null)}
                disabled={deleting}
                className="flex-1 py-2.5 text-sm font-bold uppercase tracking-widest rounded-[0.25rem] border-2"
                style={{ borderColor: 'var(--color-outline)', color: 'var(--color-on-surface-variant)' }}>
                {t('cancelar')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Icons ──────────────────────────────────────────────────────────────────────
function PlusIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
    </svg>
  )
}

function PencilIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
    </svg>
  )
}

function TrashIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
    </svg>
  )
}

function XIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
  )
}

function ExclamationIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  )
}
