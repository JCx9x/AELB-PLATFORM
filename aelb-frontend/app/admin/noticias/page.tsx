'use client'

import { useState, useEffect, useCallback, useRef } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../../components/Navbar'
import {
  getAllNews,
  createNews,
  updateNews,
  deleteNews,
  newsImageUrl,
  requestUploadUrl,
  uploadToS3,
  type NewsItem,
  type NewsData,
} from '../../lib/api'
import { getSession } from '../../lib/auth'

type FormState = Omit<NewsData, 'removeImage'> & { removeImage: boolean }

const EMPTY_FORM: FormState = {
  title:       '',
  content:     '',
  imageKey:    null,
  removeImage: false,
  published:   false,
}

// ── Component ─────────────────────────────────────────────────────────────────

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function NoticiasAdminPage() {
  const t = useTranslations('AdminNoticias')
  const locale = useLocale()
  const router = useRouter()

  const [news,      setNews]      = useState<NewsItem[]>([])
  const [loading,   setLoading]   = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [panelOpen, setPanelOpen] = useState(false)
  const [editing,   setEditing]   = useState<NewsItem | null>(null)
  const [form,      setForm]      = useState<FormState>(EMPTY_FORM)
  const [saving,    setSaving]    = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [uploading,    setUploading]    = useState(false)

  const [toDelete,  setToDelete]  = useState<NewsItem | null>(null)
  const [deleting,  setDeleting]  = useState(false)

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
      setNews(await getAllNews())
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
    setFormError(null)
    setPanelOpen(true)
  }

  function openEdit(item: NewsItem) {
    setEditing(item)
    setForm({
      title:       item.title,
      content:     item.content,
      imageKey:    null,
      removeImage: false,
      published:   item.published,
    })
    // Show existing image: prefer presigned URL from response, fallback to legacy endpoint
    const existingImg = item.imageUrl ?? (item.hasImage ? newsImageUrl(item.id) : null)
    setImagePreview(existingImg)
    setFormError(null)
    setPanelOpen(true)
  }

  function closePanel() {
    setPanelOpen(false)
    setEditing(null)
    setImagePreview(null)
    setFormError(null)
  }

  async function handleImageChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setFormError(null)
    try {
      const { uploadUrl, key } = await requestUploadUrl('news', file.type)
      await uploadToS3(uploadUrl, file)
      setForm((prev) => ({ ...prev, imageKey: key, removeImage: false }))
      // Show local preview — no round-trip needed
      setImagePreview(URL.createObjectURL(file))
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setFormError(e.detail ?? t('errorSubirImagen'))
    } finally {
      setUploading(false)
    }
  }

  function handleRemoveImage() {
    setForm((prev) => ({ ...prev, imageKey: null, removeImage: true }))
    setImagePreview(null)
    if (fileRef.current) fileRef.current.value = ''
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      if (editing) {
        const updated = await updateNews(editing.id, form)
        setNews((prev) => prev.map((n) => (n.id === updated.id ? updated : n)))
      } else {
        const { removeImage: _, ...createData } = form
        const created = await createNews(createData)
        setNews((prev) => [created, ...prev])
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
      await deleteNews(toDelete.id)
      setNews((prev) => prev.filter((n) => n.id !== toDelete.id))
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
              {t('countTotal', { count: news.length })}
            </p>
          </div>
          <button
            onClick={openCreate}
            className="flex items-center gap-2 px-5 py-2.5 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
            <PlusIcon className="w-4 h-4" />
            {t('nuevaNoticia')}
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
          ) : news.length === 0 ? (
            <div className="text-center py-20">
              <p className="text-sm font-medium" style={{ color: 'var(--color-on-surface-variant)' }}>
                {t('sinNoticias')}
              </p>
              <button onClick={openCreate} className="mt-3 text-sm font-bold underline"
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
                  {news.map((item) => {
                    const imgSrc = item.imageUrl ?? (item.hasImage ? newsImageUrl(item.id) : null)
                    return (
                      <tr key={item.id} className="group transition-colors"
                        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-surface)')}
                        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}>
                        <td className="px-5 py-3">
                          {imgSrc ? (
                            <img src={imgSrc} alt=""
                              className="w-12 h-12 object-cover rounded-[0.25rem]"
                              style={{ border: '1px solid var(--color-outline-variant)' }} />
                          ) : (
                            <div className="w-12 h-12 rounded-[0.25rem] flex items-center justify-center"
                              style={{ backgroundColor: 'var(--color-surface)', border: '1px solid var(--color-outline-variant)' }}>
                              <ImageIcon className="w-5 h-5" style={{ color: 'var(--color-outline)' }} />
                            </div>
                          )}
                        </td>
                        <td className="px-5 py-3.5 font-semibold max-w-xs" style={{ color: 'var(--color-on-surface)' }}>
                          <span className="line-clamp-2">{item.title}</span>
                        </td>
                        <td className="px-5 py-3.5 whitespace-nowrap text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {new Date(item.createdAt).toLocaleDateString(dateLocale(locale), { day: '2-digit', month: 'short', year: 'numeric' })}
                        </td>
                        <td className="px-5 py-3.5">
                          <span className="px-2 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider"
                            style={{
                              backgroundColor: item.published ? 'rgba(5,150,105,0.1)' : 'rgba(100,100,100,0.1)',
                              color:           item.published ? '#047857' : 'var(--color-on-surface-variant)',
                            }}>
                            {item.published ? t('publicada') : t('borrador')}
                          </span>
                        </td>
                        <td className="px-5 py-3.5">
                          <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                            <button onClick={() => openEdit(item)}
                              className="p-1.5 rounded transition-colors"
                              style={{ color: 'var(--color-on-surface-variant)' }}
                              onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-on-surface)')}
                              onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-on-surface-variant)')}
                              aria-label={t('editar')}>
                              <PencilIcon className="w-4 h-4" />
                            </button>
                            <button onClick={() => setToDelete(item)}
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
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* ── Side panel ────────────────────────────────────────────────────── */}
      {panelOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/40" onClick={closePanel} />
          <aside className="fixed inset-y-0 right-0 z-50 w-full max-w-2xl flex flex-col shadow-2xl"
            style={{ backgroundColor: 'var(--color-surface-card)' }}>

            {/* Panel header */}
            <div className="flex items-center justify-between px-6 py-5 border-b flex-shrink-0"
              style={{ borderColor: 'var(--color-outline-variant)' }}>
              <h2 className="font-bold text-lg" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                {editing ? t('editarNoticia') : t('nuevaNoticia')}
              </h2>
              <button onClick={closePanel} className="p-1.5 rounded transition-colors"
                style={{ color: 'var(--color-outline)' }}
                onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-on-surface)')}
                onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-outline)')}>
                <XIcon className="w-5 h-5" />
              </button>
            </div>

            {/* Form */}
            <form id="news-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto px-6 py-6 space-y-5">

              {formError && (
                <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm"
                  style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
                  <ExclamationIcon className="w-4 h-4 flex-shrink-0" />
                  {formError}
                </div>
              )}

              {/* Título */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoTitulo')} <span style={{ color: 'var(--color-error)' }}>*</span>
                </label>
                <input type="text" required maxLength={300}
                  value={form.title}
                  onChange={(e) => setForm((p) => ({ ...p, title: e.target.value }))}
                  placeholder={t('placeholderTitulo')}
                  className={inputCls} style={inputStyle} />
              </div>

              {/* Imagen */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoImagen')}{' '}
                  <span className="normal-case font-normal tracking-normal text-[10px]"
                    style={{ color: 'var(--color-outline)' }}>
                    {t('imagenHint')}
                  </span>
                </label>

                {imagePreview ? (
                  <div className="relative rounded-[0.25rem] overflow-hidden border"
                    style={{ borderColor: 'var(--color-outline-variant)' }}>
                    <img src={imagePreview} alt={t('vistaPrevia')}
                      className="w-full h-48 object-cover" />
                    <button type="button" onClick={handleRemoveImage}
                      className="absolute top-2 right-2 p-1.5 rounded-full bg-black/60 text-white hover:bg-black/80 transition-colors">
                      <XIcon className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <label className="flex flex-col items-center justify-center h-36 rounded-[0.25rem] border-2 border-dashed cursor-pointer transition-colors"
                    style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}
                    onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--color-primary-cta)')}
                    onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--color-outline-variant)')}>
                    {uploading ? (
                      <div className="flex flex-col items-center gap-2">
                        <div className="w-6 h-6 border-2 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
                        <span className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>{t('subiendo')}</span>
                      </div>
                    ) : (
                      <>
                        <ImageIcon className="w-8 h-8 mb-2" style={{ color: 'var(--color-outline)' }} />
                        <span className="text-sm font-medium" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {t('clicParaSubir')}
                        </span>
                        <span className="text-xs mt-1" style={{ color: 'var(--color-outline)' }}>
                          JPG, PNG, WebP
                        </span>
                      </>
                    )}
                    <input ref={fileRef} type="file" accept="image/*" className="sr-only"
                      onChange={handleImageChange} disabled={uploading} />
                  </label>
                )}
              </div>

              {/* Contenido */}
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('campoContenido')} <span style={{ color: 'var(--color-error)' }}>*</span>
                </label>
                <textarea required rows={16}
                  value={form.content}
                  onChange={(e) => setForm((p) => ({ ...p, content: e.target.value }))}
                  placeholder={t('placeholderContenido')}
                  className={`${inputCls} resize-y`} style={{ ...inputStyle, minHeight: '16rem' }} />
                <p className="text-[10px] mt-1" style={{ color: 'var(--color-outline)' }}>
                  {t('caracteresCount', { count: form.content.length })}
                </p>
              </div>

              {/* Publicada */}
              <div className="flex items-center justify-between py-3 px-4 rounded-[0.25rem] border"
                style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                <div>
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>{t('publicada')}</p>
                  <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('visibleEnWeb')}
                  </p>
                </div>
                <button type="button"
                  onClick={() => setForm((p) => ({ ...p, published: !p.published }))}
                  className="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors"
                  style={{ backgroundColor: form.published ? 'var(--color-primary-cta)' : 'var(--color-outline)' }}
                  role="switch" aria-checked={form.published}>
                  <span className="pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow ring-0 transition-transform"
                    style={{ transform: form.published ? 'translateX(20px)' : 'translateX(0)' }} />
                </button>
              </div>
            </form>

            {/* Footer */}
            <div className="px-6 py-4 border-t flex items-center gap-3 flex-shrink-0"
              style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
              <button type="submit" form="news-form"
                disabled={saving || uploading || !form.title.trim() || !form.content.trim()}
                className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}
                onMouseEnter={(e) => { if (!saving) e.currentTarget.style.backgroundColor = 'var(--color-primary)' }}
                onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)' }}>
                {saving && <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
                {editing ? t('guardarCambios') : t('publicarNoticia')}
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

      {/* ── Delete confirmation ───────────────────────────────────────────── */}
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
                {toDelete.title}
              </strong>. {t('confirmEliminarSufijo')}
            </p>
            <div className="flex gap-3">
              <button onClick={handleDelete} disabled={deleting}
                className="flex-1 py-2.5 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] flex items-center justify-center gap-2 disabled:opacity-50"
                style={{ backgroundColor: 'var(--color-error)' }}>
                {deleting && <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
                {t('eliminar')}
              </button>
              <button onClick={() => setToDelete(null)} disabled={deleting}
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

// ── Shared styles ─────────────────────────────────────────────────────────────
const inputCls = `w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
  focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent`
const inputStyle = {
  backgroundColor: 'var(--color-surface)',
  color:           'var(--color-on-surface)',
  border:          '1px solid var(--color-outline)',
}

// ── Icons ─────────────────────────────────────────────────────────────────────
function PlusIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}><path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" /></svg>
}
function PencilIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
}
function TrashIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
}
function XIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
}
function ExclamationIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
}
function ImageIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
}
