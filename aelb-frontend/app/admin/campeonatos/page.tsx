'use client'

import { useState, useEffect, useCallback, useRef } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../../components/Navbar'
import {
  getAllChampionships,
  createChampionship,
  updateChampionship,
  deleteChampionship,
  getCategories,
  requestUploadUrl,
  uploadToS3,
  getChampionshipCategoryPrices,
  saveChampionshipCategoryPrices,
  type Championship,
  type ChampionshipData,
  type Category,
  type ChampionshipAgeCategoryPrice,
} from '../../lib/api'
import { getSession } from '../../lib/auth'

const EMPTY_FORM: ChampionshipData = {
  name:                 '',
  location:             '',
  eventDate:            '',
  registrationDeadline: '',
  price:                0,
  imageKey:             null,
  description:          null,
  requiresCurrentQuota: false,
  visible:              false,
  categoryIds:          [],
}

const DEFAULT_CATEGORY_PRICES: ChampionshipAgeCategoryPrice[] = [
  'SUB_JUNIOR', 'JUNIOR', 'YOUTH', 'AMATEUR', 'SENIOR', 'MASTER', 'GRAND_MASTER', 'SENIOR_GRAND_MASTER', 'SUPER_SENIOR_GRAND_MASTER',
].map((ageCategory) => ({ ageCategory: ageCategory as ChampionshipAgeCategoryPrice['ageCategory'], pricePerArm: 20, combinationPrice: 10 }))

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function CampeonatosAdminPage() {
  const t = useTranslations('AdminCampeonatos')
  const locale = useLocale()
  const router = useRouter()

  const [championships, setChampionships] = useState<Championship[]>([])
  const [categories,    setCategories]    = useState<Category[]>([])
  const [loading,       setLoading]       = useState(true)
  const [loadError,     setLoadError]     = useState<string | null>(null)

  const [panelOpen,    setPanelOpen]    = useState(false)
  const [editing,      setEditing]      = useState<Championship | null>(null)
  const [form,         setForm]         = useState<ChampionshipData>(EMPTY_FORM)
  const [saving,       setSaving]       = useState(false)
  const [formError,    setFormError]    = useState<string | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [uploading,    setUploading]    = useState(false)
  const [categoryPrices, setCategoryPrices] = useState<ChampionshipAgeCategoryPrice[]>(DEFAULT_CATEGORY_PRICES)

  const [toDelete,   setToDelete]   = useState<Championship | null>(null)
  const [deleting,   setDeleting]   = useState(false)

  const fileRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const session = getSession()
    if (!session) { router.push('/login'); return }
    if (session.role === 'USER') { router.push('/'); return }
  }, [router])

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const [champs, cats] = await Promise.all([getAllChampionships(), getCategories()])
      setChampionships(champs)
      setCategories(cats)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setLoadError(e.detail ?? t('errorCargar'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => { load() }, [load])

  function openCreate() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setImagePreview(null)
    setCategoryPrices(DEFAULT_CATEGORY_PRICES)
    setFormError(null)
    setPanelOpen(true)
  }

  function openEdit(c: Championship) {
    setEditing(c)
    setForm({
      name:                 c.name,
      location:             c.location,
      eventDate:            c.eventDate,
      registrationDeadline: c.registrationDeadline,
      price:                c.price,
      imageKey:             null,   // keep existing image unless user uploads a new one
      description:          c.description,
      requiresCurrentQuota: c.requiresCurrentQuota,
      visible:              c.visible,
      categoryIds:          [...c.categoryIds],
    })
    setImagePreview(c.imageUrl)   // imageUrl from backend is already a resolved URL
    setFormError(null)
    setPanelOpen(true)
    getChampionshipCategoryPrices(c.id).then(setCategoryPrices).catch(() => setCategoryPrices(DEFAULT_CATEGORY_PRICES))
  }

  function closePanel() {
    setPanelOpen(false)
    setEditing(null)
    setImagePreview(null)
    setFormError(null)
  }

  function setField<K extends keyof ChampionshipData>(key: K, value: ChampionshipData[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function toggleCategory(id: string) {
    setForm((prev) => ({
      ...prev,
      categoryIds: prev.categoryIds.includes(id)
        ? prev.categoryIds.filter((c) => c !== id)
        : [...prev.categoryIds, id],
    }))
  }

  async function handleImageChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setFormError(null)
    try {
      const { uploadUrl, key } = await requestUploadUrl('championships', file.type)
      await uploadToS3(uploadUrl, file)
      setField('imageKey', key)
      setImagePreview(URL.createObjectURL(file))
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setFormError(e.detail ?? t('errorSubirImagen'))
    } finally {
      setUploading(false)
    }
  }

  function handleRemoveImage() {
    setField('imageKey', '')  // "" = explicit removal signal for backend
    setImagePreview(null)
    if (fileRef.current) fileRef.current.value = ''
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      if (editing) {
        // imageKey null = keep existing (backend reads current value)
        // imageKey ""   = remove image
        // imageKey "x"  = new key (uploaded this session)
        const updated = await updateChampionship(editing.id, form)
        await saveChampionshipCategoryPrices(updated.id, categoryPrices)
        setChampionships((prev) => prev.map((c) => (c.id === updated.id ? updated : c)))
      } else {
        const created = await createChampionship(form)
        await saveChampionshipCategoryPrices(created.id, categoryPrices)
        setChampionships((prev) => [...prev, created])
      }
      closePanel()
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setFormError(e.detail ?? t('errorGuardar'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!toDelete) return
    setDeleting(true)
    try {
      await deleteChampionship(toDelete.id)
      setChampionships((prev) => prev.filter((c) => c.id !== toDelete.id))
      setToDelete(null)
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setLoadError(e.detail ?? t('errorEliminar'))
      setToDelete(null)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-10">

        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="font-extrabold text-2xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.02em' }}>
              {t('titulo')}
            </h1>
            <p className="text-sm mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('countTotal', { count: championships.length })}
            </p>
          </div>
          <button
            onClick={openCreate}
            className="flex items-center gap-2 px-5 py-2.5 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
            <PlusIcon className="w-4 h-4" />
            {t('nuevoCampeonato')}
          </button>
        </div>

        {loadError && (
          <div className="mb-4 flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm font-medium"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            <ExclamationIcon className="w-4 h-4 flex-shrink-0" />
            {loadError}
          </div>
        )}

        {/* Table */}
        <div className="rounded-[0.25rem] border overflow-hidden"
          style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-7 h-7 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
            </div>
          ) : championships.length === 0 ? (
            <div className="text-center py-20">
              <p className="text-sm font-medium" style={{ color: 'var(--color-on-surface-variant)' }}>
                {t('sinCampeonatos')}
              </p>
              <button onClick={openCreate}
                className="mt-3 text-sm font-bold underline"
                style={{ color: 'var(--color-primary-cta)' }}>
                {t('crearPrimero')}
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
                  {championships.map((ch) => (
                    <tr key={ch.id} className="group transition-colors"
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-surface)')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}>
                      <td className="px-5 py-3.5 font-semibold" style={{ color: 'var(--color-on-surface)' }}>
                        {ch.name}
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {new Date(ch.eventDate).toLocaleDateString(dateLocale(locale), { day: '2-digit', month: 'short', year: 'numeric' })}
                      </td>
                      <td className="px-5 py-3.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {ch.location}
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {ch.price === 0 ? t('gratuito') : `${ch.price} €`}
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="text-[11px]" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {t('catCorto', { count: ch.categoryIds.length })}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="px-2 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider"
                          style={{
                            backgroundColor: ch.visible ? 'rgba(5,150,105,0.1)' : 'rgba(100,100,100,0.1)',
                            color:           ch.visible ? '#047857' : 'var(--color-on-surface-variant)',
                          }}>
                          {ch.visible ? t('publicado') : t('borrador')}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => openEdit(ch)}
                            className="p-1.5 rounded transition-colors"
                            style={{ color: 'var(--color-on-surface-variant)' }}
                            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-on-surface)')}
                            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-on-surface-variant)')}
                            aria-label={t('editar')}>
                            <PencilIcon className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => setToDelete(ch)}
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

      {/* ── Side panel ── */}
      {panelOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/40" onClick={closePanel} />
          <aside className="fixed inset-y-0 right-0 z-50 w-full max-w-lg flex flex-col shadow-2xl"
            style={{ backgroundColor: 'var(--color-surface-card)' }}>

            {/* Panel header */}
            <div className="flex items-center justify-between px-6 py-5 border-b"
              style={{ borderColor: 'var(--color-outline-variant)' }}>
              <h2 className="font-bold text-lg" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                {editing ? t('editarCampeonato') : t('nuevoCampeonato')}
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
            <form id="champ-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto px-6 py-6 space-y-5">

              {formError && (
                <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm"
                  style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
                  <ExclamationIcon className="w-4 h-4 flex-shrink-0" />
                  {formError}
                </div>
              )}

              {/* Nombre */}
              <Field label={t('campoNombre')} required>
                <input id="ch-name" type="text" required maxLength={200}
                  value={form.name}
                  onChange={(e) => setField('name', e.target.value)}
                  placeholder={t('placeholderNombre')}
                  className={inputCls} style={inputStyle} />
              </Field>

              {/* Ubicación */}
              <Field label={t('campoUbicacion')} required>
                <input id="ch-loc" type="text" required maxLength={200}
                  value={form.location}
                  onChange={(e) => setField('location', e.target.value)}
                  placeholder={t('placeholderUbicacion')}
                  className={inputCls} style={inputStyle} />
              </Field>

              {/* Fechas */}
              <div className="grid grid-cols-2 gap-4">
                <Field label={t('campoFechaEvento')} required>
                  <input type="date" required
                    value={form.eventDate}
                    onChange={(e) => setField('eventDate', e.target.value)}
                    className={inputCls} style={inputStyle} />
                </Field>
                <Field label={t('campoLimiteInscripcion')} required>
                  <input type="date" required
                    value={form.registrationDeadline}
                    onChange={(e) => setField('registrationDeadline', e.target.value)}
                    className={inputCls} style={inputStyle} />
                </Field>
              </div>

              {/* El importe se configura por categoría WAF tras crear el campeonato. */}
              <Field label="Precios de inscripción">
                <div className="space-y-2">
                  {categoryPrices.map((price) => (
                    <label key={price.ageCategory} className="grid grid-cols-[1fr_88px_88px] items-center gap-2 text-xs">
                      <span className="font-semibold">{price.ageCategory.replaceAll('_', ' ')}</span>
                      <input type="number" min={0} step={0.01} value={price.pricePerArm}
                        onChange={(e) => setCategoryPrices((current) => current.map((item) => item.ageCategory === price.ageCategory ? { ...item, pricePerArm: Number(e.target.value) } : item))}
                        className={inputCls} style={inputStyle} aria-label={`${price.ageCategory} por brazo`} />
                      <input type="number" min={0} step={0.01} value={price.combinationPrice}
                        onChange={(e) => setCategoryPrices((current) => current.map((item) => item.ageCategory === price.ageCategory ? { ...item, combinationPrice: Number(e.target.value) } : item))}
                        className={inputCls} style={inputStyle} aria-label={`${price.ageCategory} combinación`} />
                    </label>
                  ))}
                  <p className="text-[11px]" style={{ color: 'var(--color-outline)' }}>Columnas: precio por brazo · suplemento fijo al combinar con otra categoría tras dos brazos.</p>
                </div>
              </Field>

              {/* Imagen */}
              <Field label={t('campoImagen')} hint={t('opcional')}>
                {imagePreview ? (
                  <div className="relative rounded-[0.25rem] overflow-hidden border"
                    style={{ borderColor: 'var(--color-outline-variant)' }}>
                    <img src={imagePreview} alt={t('vistaPrevia')}
                      className="w-full h-36 object-cover" />
                    <button type="button" onClick={handleRemoveImage}
                      className="absolute top-2 right-2 p-1.5 rounded-full bg-black/60 text-white hover:bg-black/80 transition-colors">
                      <XIcon className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <label className="flex flex-col items-center justify-center h-28 rounded-[0.25rem] border-2 border-dashed cursor-pointer transition-colors"
                    style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}
                    onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--color-primary-cta)')}
                    onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--color-outline-variant)')}>
                    {uploading ? (
                      <div className="flex flex-col items-center gap-2">
                        <div className="w-5 h-5 border-2 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
                        <span className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>{t('subiendo')}</span>
                      </div>
                    ) : (
                      <>
                        <ImageIcon className="w-6 h-6 mb-1" style={{ color: 'var(--color-outline)' }} />
                        <span className="text-sm font-medium" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {t('clicParaSubir')}
                        </span>
                        <span className="text-xs mt-0.5" style={{ color: 'var(--color-outline)' }}>
                          JPG, PNG, WebP
                        </span>
                      </>
                    )}
                    <input ref={fileRef} type="file" accept="image/*" className="sr-only"
                      onChange={handleImageChange} disabled={uploading} />
                  </label>
                )}
              </Field>

              {/* Descripción */}
              <Field label={t('campoDescripcion')} hint={t('opcional')}>
                <textarea rows={3}
                  value={form.description ?? ''}
                  onChange={(e) => setField('description', e.target.value || null)}
                  placeholder={t('placeholderDescripcion')}
                  className={`${inputCls} resize-none`} style={inputStyle} />
              </Field>

              {/* Categorías */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoCategorias')}{' '}
                  <span className="normal-case font-normal tracking-normal text-[10px]"
                    style={{ color: 'var(--color-outline)' }}>
                    ({t('categoriasSeleccionadas', { count: form.categoryIds.length })})
                  </span>
                </label>
                {categories.length === 0 ? (
                  <p className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('sinCategoriasDisponibles')}
                  </p>
                ) : (
                  <div className="flex flex-wrap gap-2 max-h-40 overflow-y-auto pr-1">
                    {categories.map((cat) => {
                      const selected = form.categoryIds.includes(cat.id)
                      return (
                        <button key={cat.id} type="button"
                          onClick={() => toggleCategory(cat.id)}
                          className="px-3 py-1.5 text-xs font-semibold rounded-[0.25rem] border transition-colors"
                          style={{
                            borderColor:     selected ? 'var(--color-primary-cta)' : 'var(--color-outline-variant)',
                            backgroundColor: selected ? 'var(--color-primary-cta)' : 'transparent',
                            color:           selected ? 'white' : 'var(--color-on-surface-variant)',
                          }}>
                          {cat.name}
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>

              {/* Cuota anual */}
              <div className="flex items-center justify-between py-3 px-4 rounded-[0.25rem] border"
                style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                <div>
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>{t('exigirCuota')}</p>
                  <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('exigirCuotaHint')}
                  </p>
                </div>
                <button type="button"
                  onClick={() => setField('requiresCurrentQuota', !form.requiresCurrentQuota)}
                  className="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors"
                  style={{ backgroundColor: form.requiresCurrentQuota ? 'var(--color-primary-cta)' : 'var(--color-outline)' }}
                  role="switch" aria-checked={form.requiresCurrentQuota}>
                  <span className="pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow ring-0 transition-transform"
                    style={{ transform: form.requiresCurrentQuota ? 'translateX(20px)' : 'translateX(0)' }} />
                </button>
              </div>

              {/* Visible */}
              <div className="flex items-center justify-between py-3 px-4 rounded-[0.25rem] border"
                style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                <div>
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>{t('publicado')}</p>
                  <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('publicadoHint')}
                  </p>
                </div>
                <button type="button"
                  onClick={() => setField('visible', !form.visible)}
                  className="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors"
                  style={{ backgroundColor: form.visible ? 'var(--color-primary-cta)' : 'var(--color-outline)' }}
                  role="switch" aria-checked={form.visible}>
                  <span className="pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow ring-0 transition-transform"
                    style={{ transform: form.visible ? 'translateX(20px)' : 'translateX(0)' }} />
                </button>
              </div>
            </form>

            {/* Footer */}
            <div className="px-6 py-4 border-t flex items-center gap-3"
              style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
              <button
                type="submit" form="champ-form"
                disabled={saving || uploading || !form.name.trim() || !form.location.trim() || !form.eventDate || !form.registrationDeadline}
                className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}
                onMouseEnter={(e) => { if (!saving) e.currentTarget.style.backgroundColor = 'var(--color-primary)' }}
                onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)' }}>
                {saving && <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
                {editing ? t('guardarCambios') : t('crearCampeonato')}
              </button>
              <button type="button" onClick={closePanel} disabled={saving}
                className="px-6 py-3 text-sm font-bold uppercase tracking-widest rounded-[0.25rem] border-2 transition-colors disabled:opacity-40"
                style={{ borderColor: 'var(--color-on-surface)', color: 'var(--color-on-surface)' }}>
                {t('cancelar')}
              </button>
            </div>
          </aside>
        </>
      )}

      {/* ── Delete confirmation ── */}
      {toDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
          <div className="rounded-[0.25rem] border w-full max-w-sm p-6 shadow-2xl"
            style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
            <h3 className="font-bold text-base mb-2" style={{ color: 'var(--color-on-surface)' }}>
              {t('confirmEliminarTitulo')}
            </h3>
            <p className="text-sm mb-5" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('confirmEliminarPrefijo')}{' '}
              <strong className="font-semibold" style={{ color: 'var(--color-on-surface)' }}>
                {toDelete.name}
              </strong>{' '}
              {t('confirmEliminarSufijo')}
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

// ── Field wrapper ─────────────────────────────────────────────────────────────
function Field({ label, hint, required, children }: {
  label: string
  hint?: string
  required?: boolean
  children: React.ReactNode
}) {
  return (
    <div>
      <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
        style={{ color: 'var(--color-on-surface)' }}>
        {label}
        {required && <span style={{ color: 'var(--color-error)' }}> *</span>}
        {hint && (
          <span className="normal-case font-normal tracking-normal text-[10px] ml-1"
            style={{ color: 'var(--color-outline)' }}>
            ({hint})
          </span>
        )}
      </label>
      {children}
    </div>
  )
}

const inputCls = `w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
  focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent`

const inputStyle = {
  backgroundColor: 'var(--color-surface)',
  color:           'var(--color-on-surface)',
  border:          '1px solid var(--color-outline)',
}

// ── Icons ─────────────────────────────────────────────────────────────────────
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
function ImageIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
}
