'use client'

import { useState, useEffect, useRef } from 'react'
import { useTranslations } from 'next-intl'
import Navbar from '../../components/Navbar'
import {
  getAllDocuments,
  createDocument,
  updateDocument,
  deleteDocument,
  documentFileUrl,
  type DocumentItem,
  type DocumentData,
} from '../../lib/api'

const inputCls   = 'w-full px-3 py-2.5 rounded-[0.25rem] border text-sm outline-none transition-colors focus:ring-2 focus:ring-[#d32f2f]/30'
const inputStyle = {
  backgroundColor: 'var(--color-surface)',
  borderColor:     'var(--color-outline-variant)',
  color:           'var(--color-on-surface)',
}

type DrawerMode = 'create' | 'edit'

interface UploadedFile {
  base64:   string
  fileName: string
  size:     number
}

export default function AdminDocumentacionPage() {
  const t = useTranslations('AdminDocumentacion')
  const [documents,   setDocuments]   = useState<DocumentItem[]>([])
  const [loading,     setLoading]     = useState(true)
  const [error,       setError]       = useState<string | null>(null)
  const [drawerOpen,  setDrawerOpen]  = useState(false)
  const [drawerMode,  setDrawerMode]  = useState<DrawerMode>('create')
  const [selected,    setSelected]    = useState<DocumentItem | null>(null)
  const [saving,      setSaving]      = useState(false)
  const [saveError,   setSaveError]   = useState<string | null>(null)
  const [deleting,    setDeleting]    = useState<string | null>(null)

  const [title,       setTitle]       = useState('')
  const [description, setDescription] = useState('')
  const [published,   setPublished]   = useState(false)
  const [uploadFile,  setUploadFile]  = useState<UploadedFile | null>(null)
  const [dragOver,    setDragOver]    = useState(false)

  const fileInputRef = useRef<HTMLInputElement>(null)

  function load() {
    setLoading(true)
    getAllDocuments()
      .then(setDocuments)
      .catch((err: { detail?: string }) => setError(err.detail ?? t('errorCargar')))
      .finally(() => setLoading(false))
  }

  useEffect(load, [t])

  function openCreate() {
    setDrawerMode('create')
    setSelected(null)
    setTitle('')
    setDescription('')
    setPublished(false)
    setUploadFile(null)
    setSaveError(null)
    setDrawerOpen(true)
  }

  function openEdit(item: DocumentItem) {
    setDrawerMode('edit')
    setSelected(item)
    setTitle(item.title)
    setDescription(item.description ?? '')
    setPublished(item.published)
    setUploadFile(null)
    setSaveError(null)
    setDrawerOpen(true)
  }

  async function readFile(file: File): Promise<UploadedFile> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => {
        const dataUrl = reader.result as string
        const base64  = dataUrl.split(',')[1]
        resolve({ base64, fileName: file.name, size: file.size })
      }
      reader.onerror = () => reject(new Error(t('errorLeerArchivo')))
      reader.readAsDataURL(file)
    })
  }

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      setUploadFile(await readFile(file))
      setSaveError(null)
    } catch (err: unknown) {
      setSaveError(err instanceof Error ? err.message : t('errorLeerArchivo'))
    }
  }

  async function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files[0]
    if (!file) return
    try {
      setUploadFile(await readFile(file))
      setSaveError(null)
    } catch (err: unknown) {
      setSaveError(err instanceof Error ? err.message : t('errorLeerArchivo'))
    }
  }

  async function handleSave() {
    if (!title.trim()) { setSaveError(t('tituloObligatorio')); return }
    if (drawerMode === 'create' && !uploadFile) { setSaveError(t('debesSeleccionarArchivo')); return }

    setSaving(true)
    setSaveError(null)
    try {
      const data: DocumentData = {
        title:       title.trim(),
        description: description.trim() || null,
        fileBase64:  uploadFile?.base64   ?? null,
        fileName:    uploadFile?.fileName ?? null,
        published,
      }
      if (drawerMode === 'create') {
        await createDocument(data)
      } else {
        await updateDocument(selected!.id, data)
      }
      setDrawerOpen(false)
      load()
    } catch (err: unknown) {
      const e = err as { detail?: string }
      setSaveError(e.detail ?? t('errorGuardar'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(t('confirmarEliminar'))) return
    setDeleting(id)
    try {
      await deleteDocument(id)
      setDocuments((prev) => prev.filter((d) => d.id !== id))
    } catch {
      alert(t('errorEliminar'))
    } finally {
      setDeleting(null)
    }
  }

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="font-extrabold text-2xl lg:text-3xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.02em' }}>
              {t('titulo')}
            </h1>
            <p className="mt-1 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('subtitulo')}
            </p>
          </div>
          <button
            onClick={openCreate}
            className="flex items-center gap-2 px-4 py-2.5 rounded-[0.25rem] text-sm font-bold uppercase tracking-widest text-white transition-opacity hover:opacity-90"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}>
            <PlusIcon className="w-4 h-4" />
            {t('nuevoDocumento')}
          </button>
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

        {!loading && documents.length === 0 && (
          <div className="text-center py-24">
            <DocIcon className="w-10 h-10 mx-auto mb-3" style={{ color: 'var(--color-outline)' }} />
            <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('sinDocumentos')}
            </p>
          </div>
        )}

        {!loading && documents.length > 0 && (
          <div className="rounded-[0.25rem] border overflow-hidden" style={{ borderColor: 'var(--color-outline-variant)' }}>
            <table className="w-full text-sm">
              <thead>
                <tr style={{ backgroundColor: 'var(--color-surface)', borderBottom: '1px solid var(--color-outline-variant)' }}>
                  <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px]"
                    style={{ color: 'var(--color-on-surface-variant)' }}>{t('colTitulo')}</th>
                  <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px] hidden md:table-cell"
                    style={{ color: 'var(--color-on-surface-variant)' }}>{t('colArchivo')}</th>
                  <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px] hidden lg:table-cell"
                    style={{ color: 'var(--color-on-surface-variant)' }}>{t('colTamano')}</th>
                  <th className="px-4 py-3 text-left font-bold uppercase tracking-widest text-[11px]"
                    style={{ color: 'var(--color-on-surface-variant)' }}>{t('colEstado')}</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {documents.map((item, i) => (
                  <tr key={item.id}
                    style={{
                      backgroundColor: i % 2 === 0 ? 'var(--color-surface-card)' : 'var(--color-surface)',
                      borderBottom: '1px solid var(--color-outline-variant)',
                    }}>
                    <td className="px-4 py-3">
                      <p className="font-semibold line-clamp-1" style={{ color: 'var(--color-on-surface)' }}>
                        {item.title}
                      </p>
                      {item.description && (
                        <p className="text-xs mt-0.5 line-clamp-1" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {item.description}
                        </p>
                      )}
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell">
                      <span className="text-xs truncate max-w-[160px] block" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {item.fileName ?? '—'}
                      </span>
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell">
                      <span className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {formatBytes(item.fileOriginalSize)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
                        style={item.published
                          ? { backgroundColor: 'rgba(5,150,105,0.1)', color: '#047857' }
                          : { backgroundColor: 'rgba(100,100,100,0.1)', color: 'var(--color-on-surface-variant)' }}>
                        {item.published ? t('publicado') : t('borrador')}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <a
                          href={documentFileUrl(item.id)}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="p-1.5 rounded transition-colors hover:bg-black/5"
                          title={t('descargar')}
                          style={{ color: 'var(--color-on-surface-variant)' }}>
                          <DownloadIcon className="w-4 h-4" />
                        </a>
                        <button
                          onClick={() => openEdit(item)}
                          className="p-1.5 rounded transition-colors hover:bg-black/5"
                          title={t('editar')}
                          style={{ color: 'var(--color-on-surface-variant)' }}>
                          <EditIcon className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(item.id)}
                          disabled={deleting === item.id}
                          className="p-1.5 rounded transition-colors hover:bg-red-50"
                          title={t('eliminar')}
                          style={{ color: 'var(--color-error)' }}>
                          {deleting === item.id
                            ? <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
                            : <TrashIcon className="w-4 h-4" />}
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

      {/* ── Drawer ────────────────────────────────────────────────────────────── */}
      {drawerOpen && (
        <div className="fixed inset-0 z-50 flex">
          <div className="flex-1 bg-black/40" onClick={() => setDrawerOpen(false)} />

          <aside className="w-full max-w-lg flex flex-col shadow-2xl overflow-y-auto"
            style={{ backgroundColor: 'var(--color-surface-card)' }}>

            <div className="flex items-center justify-between px-6 py-4 border-b"
              style={{ borderColor: 'var(--color-outline-variant)' }}>
              <h2 className="font-bold text-lg" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                {drawerMode === 'create' ? t('nuevoDocumento') : t('editarDocumento')}
              </h2>
              <button onClick={() => setDrawerOpen(false)}
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

              <Field label={t('campoTitulo')}>
                <input
                  className={inputCls}
                  style={inputStyle}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder={t('placeholderNombreDocumento')}
                  maxLength={300}
                />
              </Field>

              <Field label={t('campoDescripcion')}>
                <textarea
                  className={inputCls}
                  style={inputStyle}
                  rows={4}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={t('placeholderDescripcion')}
                />
              </Field>

              <Field label={drawerMode === 'edit' ? t('campoReemplazarArchivo') : t('campoArchivo')}>
                <div
                  onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
                  onDragLeave={() => setDragOver(false)}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className="relative cursor-pointer rounded-[0.25rem] border-2 border-dashed p-6 text-center transition-colors"
                  style={{
                    borderColor:     dragOver ? 'var(--color-primary-cta)' : 'var(--color-outline-variant)',
                    backgroundColor: dragOver ? 'rgba(211,47,47,0.04)' : 'transparent',
                  }}>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg"
                    className="hidden"
                    onChange={handleFileChange}
                  />

                  {uploadFile ? (
                    <div className="flex items-center justify-center gap-3">
                      <DocIcon className="w-8 h-8 flex-shrink-0" style={{ color: 'var(--color-primary-cta)' }} />
                      <div className="text-left">
                        <p className="text-sm font-semibold truncate max-w-[240px]"
                          style={{ color: 'var(--color-on-surface)' }}>
                          {uploadFile.fileName}
                        </p>
                        <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                          {formatBytes(uploadFile.size)} · {t('hazClicParaCambiar')}
                        </p>
                      </div>
                    </div>
                  ) : (
                    <>
                      <UploadIcon className="w-8 h-8 mx-auto mb-3" style={{ color: 'var(--color-outline)' }} />
                      <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>
                        {t('arrastraArchivo')}
                      </p>
                      <p className="text-xs mt-1" style={{ color: 'var(--color-on-surface-variant)' }}>
                        {t('formatosAceptados')}
                      </p>
                      {drawerMode === 'edit' && selected?.fileName && (
                        <p className="text-xs mt-2 font-medium" style={{ color: 'var(--color-primary-cta)' }}>
                          {t('archivoActual', { name: selected.fileName, size: formatBytes(selected.fileOriginalSize) })}
                        </p>
                      )}
                    </>
                  )}
                </div>
              </Field>

              <div className="flex items-center justify-between py-3 px-4 rounded-[0.25rem]"
                style={{ backgroundColor: 'var(--color-surface)', border: '1px solid var(--color-outline-variant)' }}>
                <div>
                  <p className="text-sm font-semibold" style={{ color: 'var(--color-on-surface)' }}>{t('publicado')}</p>
                  <p className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('publicadoHint')}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setPublished((v) => !v)}
                  className="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors"
                  style={{ backgroundColor: published ? 'var(--color-primary-cta)' : 'var(--color-outline)' }}>
                  <span className="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform"
                    style={{ transform: published ? 'translateX(20px)' : 'translateX(0)' }} />
                </button>
              </div>
            </div>

            <div className="px-6 py-4 border-t flex gap-3 justify-end"
              style={{ borderColor: 'var(--color-outline-variant)' }}>
              <button
                onClick={() => setDrawerOpen(false)}
                className="px-4 py-2.5 rounded-[0.25rem] text-sm font-bold uppercase tracking-widest border transition-colors"
                style={{ borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface-variant)' }}>
                {t('cancelar')}
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex items-center gap-2 px-5 py-2.5 rounded-[0.25rem] text-sm font-bold uppercase tracking-widest text-white transition-opacity hover:opacity-90 disabled:opacity-60"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                {saving && <div className="w-4 h-4 border-2 border-white/50 border-t-white rounded-full animate-spin" />}
                {saving ? t('guardando') : drawerMode === 'create' ? t('crear') : t('guardar')}
              </button>
            </div>
          </aside>
        </div>
      )}
    </div>
  )
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

function formatBytes(bytes: number): string {
  if (bytes === 0) return '—'
  if (bytes < 1024)        return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function DocIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
    </svg>
  )
}
function UploadIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
    </svg>
  )
}
function DownloadIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
    </svg>
  )
}
function PlusIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
    </svg>
  )
}
function EditIcon({ className }: { className?: string }) {
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
function CloseIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
  )
}
