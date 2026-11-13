'use client'

import { useState, useEffect } from 'react'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../components/Navbar'
import { getNews, newsImageUrl, type NewsItem } from '../lib/api'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

const PAGE_SIZE = 12
const SEARCH_DEBOUNCE_MS = 300

export default function NoticiasPage() {
  const t = useTranslations('Noticias')
  const [news,       setNews]       = useState<NewsItem[]>([])
  const [loading,    setLoading]    = useState(true)
  const [error,      setError]      = useState<string | null>(null)
  const [selected,   setSelected]   = useState<NewsItem | null>(null)
  const [search,     setSearch]     = useState('')
  const [query,      setQuery]      = useState('')
  const [page,       setPage]       = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [total,      setTotal]      = useState(0)

  // Debounce the search box so we don't hit the API on every keystroke.
  useEffect(() => {
    const next = search.trim()
    if (next === query) return
    const id = setTimeout(() => {
      setLoading(true)
      setError(null)
      setPage(0)
      setQuery(next)
    }, SEARCH_DEBOUNCE_MS)
    return () => clearTimeout(id)
  }, [search, query])

  useEffect(() => {
    let cancelled = false
    getNews(page, PAGE_SIZE, query)
      .then(result => {
        if (cancelled) return
        setNews(result.items)
        setTotalPages(result.totalPages)
        setTotal(result.totalElements)
      })
      .catch((err: { detail?: string }) => { if (!cancelled) setError(err.detail ?? t('errorCarga')) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [page, query, t])

  function goToPage(next: number) {
    setLoading(true)
    setError(null)
    setPage(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  if (selected) {
    return <ArticleView item={selected} onBack={() => setSelected(null)} />
  }

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="font-extrabold text-3xl lg:text-4xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.03em' }}>
              {t('titulo')}
            </h1>
            <p className="mt-2 text-sm lg:text-base" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('subtitulo')}
            </p>
          </div>

          <div className="relative w-full md:w-80">
            <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2" style={{ color: 'var(--color-outline)' }} />
            <input
              type="search"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder={t('buscarPlaceholder')}
              aria-label={t('buscarLabel')}
              maxLength={100}
              className="w-full rounded-[0.25rem] border py-2.5 pl-9 pr-9 text-sm outline-none focus:border-[#d32f2f]"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface)' }}
            />
            {search && (
              <button
                type="button"
                onClick={() => setSearch('')}
                aria-label={t('limpiarBusqueda')}
                className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 hover:opacity-70"
                style={{ color: 'var(--color-on-surface-variant)' }}>
                <CloseIcon className="h-4 w-4" />
              </button>
            )}
          </div>
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
              {query ? t('sinResultados', { query }) : t('sinNoticias')}
            </p>
          </div>
        )}

        {!loading && news.length > 0 && (
          <>
            {query && (
              <p className="mb-4 text-xs font-bold uppercase tracking-widest" style={{ color: 'var(--color-on-surface-variant)' }}>
                {t('resultados', { count: total })}
              </p>
            )}
            <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
              {news.map((item) => (
                <NewsCard key={item.id} item={item} onClick={() => setSelected(item)} />
              ))}
            </div>
            <Pagination page={page} totalPages={totalPages} onChange={goToPage} />
          </>
        )}
      </div>
    </div>
  )
}

// ── Pagination ────────────────────────────────────────────────────────────────

/** Page indices to render, with null standing for an ellipsis gap. */
function pageWindow(page: number, totalPages: number): (number | null)[] {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i)
  const pages = new Set([0, totalPages - 1, page - 1, page, page + 1].filter(p => p >= 0 && p < totalPages))
  const sorted = [...pages].sort((a, b) => a - b)
  const result: (number | null)[] = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push(null)
    result.push(p)
  })
  return result
}

function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  const t = useTranslations('Noticias')
  if (totalPages <= 1) return null

  const navButton = 'px-3 py-2 text-xs font-bold uppercase tracking-widest rounded-[0.25rem] border transition-opacity disabled:opacity-40 disabled:cursor-not-allowed'
  const border = { borderColor: 'var(--color-outline-variant)', color: 'var(--color-on-surface)' }

  return (
    <nav aria-label={t('paginacion')} className="mt-10 flex flex-wrap items-center justify-center gap-2">
      <button type="button" className={navButton} style={border} disabled={page === 0} onClick={() => onChange(page - 1)}>
        ← {t('anterior')}
      </button>
      {pageWindow(page, totalPages).map((p, i) => p === null ? (
        <span key={`gap-${i}`} className="px-1 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>…</span>
      ) : (
        <button
          key={p}
          type="button"
          onClick={() => onChange(p)}
          aria-label={t('pagina', { page: p + 1 })}
          aria-current={p === page ? 'page' : undefined}
          className="min-w-9 px-3 py-2 text-sm font-bold rounded-[0.25rem] border"
          style={p === page
            ? { backgroundColor: 'var(--color-primary-cta)', borderColor: 'var(--color-primary-cta)', color: '#fff' }
            : border}>
          {p + 1}
        </button>
      ))}
      <button type="button" className={navButton} style={border} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        {t('siguiente')} →
      </button>
    </nav>
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
function SearchIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11a6 6 0 11-12 0 6 6 0 0112 0z" /></svg>
}
function CloseIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
}
function ArrowLeftIcon({ className }: { className?: string }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
}
