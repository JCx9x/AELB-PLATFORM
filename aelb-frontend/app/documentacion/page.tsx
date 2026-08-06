'use client'

import { useState, useEffect } from 'react'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../components/Navbar'
import { getDocuments, documentFileUrl, type DocumentItem } from '../lib/api'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function DocumentacionPage() {
  const t = useTranslations('Documentacion')
  const [documents, setDocuments] = useState<DocumentItem[]>([])
  const [loading,   setLoading]   = useState(true)
  const [error,     setError]     = useState<string | null>(null)

  useEffect(() => {
    getDocuments()
      .then(setDocuments)
      .catch((err: { detail?: string }) => setError(err.detail ?? t('errorCarga')))
      .finally(() => setLoading(false))
  }, [t])

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        <div className="mb-8">
          <h1 className="font-extrabold text-3xl lg:text-4xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.03em' }}>
            {t('titulo')}
          </h1>
          <p className="mt-2 text-sm lg:text-base" style={{ color: 'var(--color-on-surface-variant)' }}>
            {t('subtitulo')}
          </p>
        </div>

        {loading && (
          <div className="flex items-center justify-center py-24">
            <div className="w-8 h-8 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && (
          <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm mb-6"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            {error}
          </div>
        )}

        {!loading && !error && documents.length === 0 && (
          <div className="text-center py-24">
            <DocIcon className="w-12 h-12 mx-auto mb-4" style={{ color: 'var(--color-outline)' }} />
            <p className="text-base font-semibold" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('sinDocumentos')}
            </p>
          </div>
        )}

        {!loading && documents.length > 0 && (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {documents.map((item) => (
              <DocumentCard key={item.id} item={item} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function DocumentCard({ item }: { item: DocumentItem }) {
  const t = useTranslations('Documentacion')
  const locale = useLocale()
  const dateStr = new Date(item.publishedAt ?? item.createdAt).toLocaleDateString(dateLocale(locale), {
    day: 'numeric', month: 'long', year: 'numeric',
  })
  const sizeStr = formatBytes(item.fileOriginalSize)
  const ext     = fileExt(item.fileName, t('archivo'))

  return (
    <article
      className="rounded-[0.25rem] border flex flex-col overflow-hidden transition-all hover:shadow-md hover:-translate-y-0.5"
      style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

      <div className="h-0.5" style={{ backgroundColor: 'var(--color-primary-cta)' }} />

      <div className="p-5 flex flex-col gap-3 flex-1">
        <div className="flex items-start gap-3">
          <div className="flex-shrink-0 w-10 h-10 rounded-[0.25rem] flex items-center justify-center"
            style={{ backgroundColor: 'rgba(211,47,47,0.08)' }}>
            <DocIcon className="w-5 h-5" style={{ color: 'var(--color-primary-cta)' }} />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[11px] font-bold uppercase tracking-widest mb-1"
              style={{ color: 'var(--color-primary-cta)' }}>
              {dateStr}
            </p>
            <h2 className="font-bold text-base leading-snug"
              style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
              {item.title}
            </h2>
          </div>
        </div>

        {item.description && (
          <p className="text-sm leading-relaxed line-clamp-3"
            style={{ color: 'var(--color-on-surface-variant)' }}>
            {item.description}
          </p>
        )}

        <div className="mt-auto flex items-center justify-between pt-3 border-t"
          style={{ borderColor: 'var(--color-outline-variant)' }}>
          <span className="text-xs" style={{ color: 'var(--color-on-surface-variant)' }}>
            {ext} · {sizeStr}
          </span>
          <a
            href={documentFileUrl(item.id)}
            download={item.fileName ?? 'documento'}
            className="flex items-center gap-1.5 px-4 py-2 rounded-[0.25rem] text-sm font-bold uppercase tracking-widest text-white transition-opacity hover:opacity-90"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}>
            <DownloadIcon className="w-4 h-4" />
            {t('descargar')}
          </a>
        </div>
      </div>
    </article>
  )
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '—'
  if (bytes < 1024)        return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function fileExt(name: string | null, fallback: string): string {
  if (!name) return fallback
  const ext = name.split('.').pop()?.toUpperCase()
  return ext ?? fallback
}

function DocIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
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
