'use client'

import { useState, useEffect } from 'react'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../components/Navbar'
import { getNews, newsImageUrl, type NewsItem } from '../lib/api'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function NoticiasPage() {
  const t = useTranslations('Noticias')
  const [news,    setNews]    = useState<NewsItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState<string | null>(null)
  const [selected, setSelected] = useState<NewsItem | null>(null)

  useEffect(() => {
    getNews()
      .then(setNews)
      .catch((err: { detail?: string }) => setError(err.detail ?? t('errorCarga')))
      .finally(() => setLoading(false))
  }, [t])

  if (selected) {
    return <ArticleView item={selected} onBack={() => setSelected(null)} />
  }

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

        {!loading && !error && news.length === 0 && (
          <div className="text-center py-24">
            <NewsIcon className="w-12 h-12 mx-auto mb-4" style={{ color: 'var(--color-outline)' }} />
            <p className="text-base font-semibold" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('sinNoticias')}
            </p>
          </div>
        )}

        {!loading && news.length > 0 && (
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {news.map((item) => (
              <NewsCard key={item.id} item={item} onClick={() => setSelected(item)} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

// ── News card ─────────────────────────────────────────────────────────────────

function NewsCard({ item, onClick }: { item: NewsItem; onClick: () => void }) {
  const t = useTranslations('Noticias')
  const locale = useLocale()
  const dateStr = new Date(item.publishedAt ?? item.createdAt).toLocaleDateString(dateLocale(locale), {
    day: 'numeric', month: 'long', year: 'numeric',
  })
  const preview = item.content.replace(/\n+/g, ' ').slice(0, 160)

  return (
    <article
      onClick={onClick}
      className="rounded-[0.25rem] border overflow-hidden flex flex-col cursor-pointer transition-all hover:shadow-md hover:-translate-y-0.5"
      style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

      {item.hasImage ? (
        <div className="h-44 overflow-hidden flex-shrink-0">
          <img src={item.imageUrl ?? newsImageUrl(item.id)} alt={item.title}
            className="w-full h-full object-cover transition-transform duration-300 hover:scale-105" />
        </div>
      ) : (
        <div className="h-44 flex-shrink-0 flex items-center justify-center"
          style={{ backgroundColor: 'var(--color-surface)' }}>
          <NewsIcon className="w-10 h-10" style={{ color: 'var(--color-outline)' }} />
        </div>
      )}

      <div className="p-5 flex flex-col flex-1 gap-2">
        <p className="text-[11px] font-bold uppercase tracking-widest"
          style={{ color: 'var(--color-primary-cta)' }}>
          {dateStr}
        </p>
        <h2 className="font-bold text-base leading-snug line-clamp-2"
          style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
          {item.title}
        </h2>
        <p className="text-sm line-clamp-3 flex-1" style={{ color: 'var(--color-on-surface-variant)' }}>
          {preview}{item.content.length > 160 ? '…' : ''}
        </p>
        <span className="mt-2 text-xs font-bold uppercase tracking-widest"
          style={{ color: 'var(--color-primary-cta)' }}>
          {t('leerMas')} →
        </span>
      </div>
    </article>
  )
}

// ── Article view (full content) ───────────────────────────────────────────────

function ArticleView({ item, onBack }: { item: NewsItem; onBack: () => void }) {
  const t = useTranslations('Noticias')
  const locale = useLocale()
  const dateStr = new Date(item.publishedAt ?? item.createdAt).toLocaleDateString(dateLocale(locale), {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
  })

  // Split content into paragraphs for readability
  const paragraphs = item.content.split(/\n+/).filter(Boolean)

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-3xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        {/* Back */}
        <button onClick={onBack}
          className="flex items-center gap-2 text-sm font-bold uppercase tracking-widest mb-8 transition-opacity hover:opacity-70"
          style={{ color: 'var(--color-on-surface-variant)' }}>
          <ArrowLeftIcon className="w-4 h-4" />
          {t('volver')}
        </button>

        {/* Hero image */}
        {item.hasImage && (
          <div className="rounded-[0.25rem] overflow-hidden mb-8"
            style={{ border: '1px solid var(--color-outline-variant)' }}>
            <img src={item.imageUrl ?? newsImageUrl(item.id)} alt={item.title}
              className="w-full max-h-80 object-cover" />
          </div>
        )}

        {/* Date */}
        <p className="text-[11px] font-bold uppercase tracking-widest mb-3 capitalize"
          style={{ color: 'var(--color-primary-cta)' }}>
          {dateStr}
        </p>

        {/* Title */}
        <h1 className="font-extrabold text-2xl lg:text-3xl leading-tight mb-6"
          style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.02em' }}>
          {item.title}
        </h1>

        {/* Content */}
        <div className="space-y-4">
          {paragraphs.map((para, i) => (
            <p key={i} className="text-base leading-relaxed"
              style={{ color: 'var(--color-on-surface)' }}>
              {para}
            </p>
          ))}
        </div>
      </div>
    </div>
  )
}

// ── Icons ─────────────────────────────────────────────────────────────────────
function NewsIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" /></svg>
}
function ArrowLeftIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
}
