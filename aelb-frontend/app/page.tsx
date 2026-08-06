'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from './components/Navbar'
import ChampionshipPricingSummary from './components/ChampionshipPricingSummary'
import {
  getChampionships,
  getNews,
  newsImageUrl,
  type Championship,
  type NewsItem,
} from './lib/api'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function HomePage() {
  const t = useTranslations('HomePage')
  const [championships, setChampionships] = useState<Championship[]>([])
  const [news,          setNews]          = useState<NewsItem[]>([])
  const [loading,       setLoading]       = useState(true)

  useEffect(() => {
    Promise.all([getChampionships(), getNews()])
      .then(([champs, items]) => {
        setChampionships(champs)
        setNews(items)
      })
      .catch(() => {/* silent — sections simply stay empty */})
      .finally(() => setLoading(false))
  }, [])

  const now = new Date()

  const sorted   = [...championships].sort(
    (a, b) => new Date(a.eventDate).getTime() - new Date(b.eventDate).getTime()
  )
  const upcoming  = sorted.filter((c) => new Date(c.eventDate) >= now)
  const past      = sorted.filter((c) => new Date(c.eventDate) <  now).reverse()

  const nextChamp = upcoming[0] ?? null
  const pastSlice = past.slice(0, 3)
  const newsSlice = news.slice(0, 3)

  const hasContent = !loading && (nextChamp || pastSlice.length > 0 || newsSlice.length > 0)

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      {/* ── Hero ─────────────────────────────────────────────────────────────── */}
      <section className="relative overflow-hidden flex items-end"
        style={{ backgroundColor: 'var(--color-dark)', minHeight: '20rem' }}>
        <div className="absolute inset-0 bg-gradient-to-br from-[#1a1c1c] via-[#2d1515] to-[#af101a]/40" />

        <div className="relative z-10 w-full max-w-7xl mx-auto px-4 lg:px-8 py-12 lg:py-16">
          {loading ? (
            <div className="flex items-center gap-3">
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              <span className="text-white/40 text-sm">{t('cargando')}</span>
            </div>
          ) : nextChamp ? (
            <HeroChampionship championship={nextChamp} />
          ) : (
            <HeroEmpty />
          )}
        </div>
      </section>

      {/* ── Body ─────────────────────────────────────────────────────────────── */}
      {loading ? (
        <div className="flex items-center justify-center py-24">
          <div className="w-8 h-8 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
        </div>
      ) : !hasContent ? (
        <EmptyState />
      ) : (
        <div className="max-w-7xl mx-auto px-4 lg:px-8 py-10 lg:py-14 space-y-14 lg:space-y-20">

          {/* Próximos campeonatos */}
          {upcoming.length > 1 && (
            <Section
              eyebrow={t('eyebrowCampeonatos')}
              title={t('tituloProximos')}
              seeAll={{ href: '/campeonatos', label: t('verTodos') }}>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {upcoming.slice(1, 4).map((c) => (
                  <ChampionshipCard key={c.id} championship={c} />
                ))}
              </div>
            </Section>
          )}

          {/* Campeonatos anteriores */}
          {pastSlice.length > 0 && (
            <Section
              eyebrow={t('eyebrowHistorial')}
              title={t('tituloAnteriores')}
              seeAll={{ href: '/campeonatos', label: t('verTodos') }}>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {pastSlice.map((c) => (
                  <ChampionshipCard key={c.id} championship={c} past />
                ))}
              </div>
            </Section>
          )}

          {/* Últimas noticias */}
          {newsSlice.length > 0 && (
            <Section
              eyebrow={t('eyebrowNoticias')}
              title={t('tituloUltimasNoticias')}
              seeAll={{ href: '/noticias', label: t('verTodas') }}>
              <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                {newsSlice.map((item) => (
                  <NewsCard key={item.id} item={item} />
                ))}
              </div>
            </Section>
          )}
        </div>
      )}

    </div>
  )
}

// ── Hero variants ─────────────────────────────────────────────────────────────

function HeroChampionship({ championship: c }: { championship: Championship }) {
  const t = useTranslations('HomePage')
  const locale = useLocale()
  const eventDate      = new Date(c.eventDate)
  const deadlineDate   = new Date(c.registrationDeadline)
  const registrationOpen = new Date() <= deadlineDate

  const dateStr = eventDate.toLocaleDateString(dateLocale(locale), {
    day: 'numeric', month: 'long', year: 'numeric',
  })

  return (
    <>
      <p className="text-[11px] font-bold uppercase tracking-widest mb-2"
        style={{ color: 'var(--color-primary-cta)' }}>
        {t('proximoCampeonato')}
      </p>
      <h1 className="text-white font-extrabold leading-tight mb-2"
        style={{ fontSize: 'clamp(1.6rem, 4vw, 2.6rem)', letterSpacing: '-0.02em', maxWidth: '36rem' }}>
        {c.name}
      </h1>
      <p className="text-white/60 text-sm mb-1">
        {dateStr}
      </p>
      <p className="text-white/50 text-sm mb-6">
        {c.location}
      </p>

      <div className="flex flex-wrap items-center gap-3">
        {registrationOpen && (
          <Link href={`/campeonatos/${c.id}?scroll=inscripcion`}
            className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
            {t('inscribirse')}
          </Link>
        )}
        <Link href={`/campeonatos/${c.id}`}
          className="px-6 py-3 text-sm font-bold uppercase tracking-widest rounded-[0.25rem] border-2 border-white/30 text-white hover:border-white/70 transition-colors">
          {t('verDetalles')}
        </Link>

        {/* Metas inline */}
        <div className="flex items-center gap-4 ml-2">
          {c.categoryIds.length > 0 && (
            <div className="text-white/60 text-xs font-semibold">
              <span className="text-white font-bold">{c.categoryIds.length}</span> {t('categorias')}
            </div>
          )}
          <ChampionshipPricingSummary dark />
          {!registrationOpen && (
            <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-1 rounded-full"
              style={{ backgroundColor: 'rgba(220,38,38,0.25)', color: '#fca5a5' }}>
              {t('inscripcionesCerradas')}
            </span>
          )}
        </div>
      </div>
    </>
  )
}

function HeroEmpty() {
  const t = useTranslations('HomePage')
  return (
    <>
      <p className="text-[11px] font-bold uppercase tracking-widest mb-3"
        style={{ color: 'var(--color-primary-cta)' }}>
        {t('heroEmptyEyebrow')}
      </p>
      <h1 className="text-white font-extrabold leading-tight mb-4"
        style={{ fontSize: 'clamp(1.6rem, 4vw, 2.6rem)', letterSpacing: '-0.02em' }}>
        {t('heroEmptyTitulo')}
      </h1>
      <p className="text-white/50 text-sm mb-6 max-w-md">
        {t('heroEmptySubtitulo')}
      </p>
      <Link href="/campeonatos"
        className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] inline-block transition-colors"
        style={{ backgroundColor: 'var(--color-primary-cta)' }}>
        {t('heroEmptyVerCampeonatos')}
      </Link>
    </>
  )
}

// ── Section wrapper ───────────────────────────────────────────────────────────

function Section({
  eyebrow, title, seeAll, children,
}: {
  eyebrow: string
  title: string
  seeAll: { href: string; label: string }
  children: React.ReactNode
}) {
  return (
    <section>
      <div className="flex items-end justify-between mb-6">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-widest mb-1"
            style={{ color: 'var(--color-primary-cta)' }}>
            {eyebrow}
          </p>
          <h2 className="font-bold text-xl lg:text-2xl leading-tight"
            style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
            {title}
          </h2>
        </div>
        <Link href={seeAll.href}
          className="text-sm font-bold uppercase tracking-wider transition-opacity hover:opacity-70 hidden sm:block"
          style={{ color: 'var(--color-primary-cta)' }}>
          {seeAll.label} →
        </Link>
      </div>
      {children}
      <div className="mt-6 text-center sm:hidden">
        <Link href={seeAll.href}
          className="text-sm font-bold uppercase tracking-wider"
          style={{ color: 'var(--color-primary-cta)' }}>
          {seeAll.label} →
        </Link>
      </div>
    </section>
  )
}

// ── Championship card ─────────────────────────────────────────────────────────

function ChampionshipCard({ championship: c, past }: { championship: Championship; past?: boolean }) {
  const t = useTranslations('HomePage')
  const locale = useLocale()
  const eventDate      = new Date(c.eventDate)
  const deadlineDate   = new Date(c.registrationDeadline)
  const registrationOpen = !past && new Date() <= deadlineDate

  const dateStr = eventDate.toLocaleDateString(dateLocale(locale), {
    day: 'numeric', month: 'short', year: 'numeric',
  })

  return (
    <article className="rounded-[0.25rem] border overflow-hidden flex flex-col"
      style={{
        backgroundColor: 'var(--color-surface-card)',
        borderColor: 'var(--color-outline-variant)',
        opacity: past ? 0.82 : 1,
      }}>

      {/* Accent bar */}
      <div className="h-0.5" style={{
        backgroundColor: past
          ? 'var(--color-outline)'
          : registrationOpen
          ? 'var(--color-primary-cta)'
          : 'rgba(234,179,8,0.7)',
      }} />

      <div className="p-5 flex flex-col gap-3 flex-1">
        {/* Status badge */}
        <div>
          <span className="text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full"
            style={
              past
                ? { backgroundColor: 'rgba(100,100,100,0.12)', color: 'var(--color-on-surface-variant)' }
                : registrationOpen
                ? { backgroundColor: 'rgba(5,150,105,0.1)', color: '#047857' }
                : { backgroundColor: 'rgba(234,179,8,0.12)', color: '#92400e' }
            }>
            {past ? t('finalizado') : registrationOpen ? t('inscripcionesAbiertas') : t('inscripcionesCerradas')}
          </span>
        </div>

        <h3 className="font-bold text-base leading-tight"
          style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
          {c.name}
        </h3>

        <div className="space-y-1">
          <p className="text-xs flex items-center gap-1.5" style={{ color: 'var(--color-on-surface-variant)' }}>
            <CalendarIcon className="w-3.5 h-3.5 flex-shrink-0" />
            {dateStr}
          </p>
          <p className="text-xs flex items-center gap-1.5" style={{ color: 'var(--color-on-surface-variant)' }}>
            <LocationIcon className="w-3.5 h-3.5 flex-shrink-0" />
            {c.location}
          </p>
        </div>

        <div className="flex gap-4 pt-2 border-t mt-auto" style={{ borderColor: 'var(--color-outline-variant)' }}>
          <div>
            <p className="text-sm font-bold" style={{ color: 'var(--color-on-surface)' }}>
              {c.categoryIds.length}
            </p>
            <p className="text-[10px] uppercase tracking-wider" style={{ color: 'var(--color-outline)' }}>
              {t('categoriasLabel')}
            </p>
          </div>
          <div>
            <p className="text-sm font-bold" style={{ color: past ? 'var(--color-on-surface)' : 'var(--color-primary-cta)' }}>
              {c.price === 0 ? t('gratis') : `${c.price} €`}
            </p>
            <p className="text-[10px] uppercase tracking-wider" style={{ color: 'var(--color-outline)' }}>
              {t('precioLabel')}
            </p>
          </div>
        </div>
      </div>
    </article>
  )
}

// ── News card ─────────────────────────────────────────────────────────────────

function NewsCard({ item }: { item: NewsItem }) {
  const t = useTranslations('HomePage')
  const locale = useLocale()
  const dateStr = new Date(item.publishedAt ?? item.createdAt).toLocaleDateString(dateLocale(locale), {
    day: 'numeric', month: 'long', year: 'numeric',
  })
  const preview = item.content.replace(/\n+/g, ' ').slice(0, 120)

  return (
    <Link href="/noticias">
      <article className="rounded-[0.25rem] border overflow-hidden flex flex-col h-full cursor-pointer transition-all hover:shadow-md hover:-translate-y-0.5"
        style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

        {item.hasImage ? (
          <div className="h-40 overflow-hidden flex-shrink-0">
            <img src={item.imageUrl ?? newsImageUrl(item.id)} alt={item.title}
              className="w-full h-full object-cover" />
          </div>
        ) : (
          <div className="h-40 flex-shrink-0 flex items-center justify-center"
            style={{ backgroundColor: 'var(--color-surface)' }}>
            <NewsIconSvg className="w-8 h-8" style={{ color: 'var(--color-outline)' }} />
          </div>
        )}

        <div className="p-4 flex flex-col gap-2 flex-1">
          <p className="text-[10px] font-bold uppercase tracking-widest"
            style={{ color: 'var(--color-primary-cta)' }}>
            {dateStr}
          </p>
          <h3 className="font-bold text-sm leading-snug line-clamp-2"
            style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
            {item.title}
          </h3>
          <p className="text-xs line-clamp-2 flex-1" style={{ color: 'var(--color-on-surface-variant)' }}>
            {preview}{item.content.length > 120 ? '…' : ''}
          </p>
          <span className="text-[11px] font-bold uppercase tracking-widest mt-1"
            style={{ color: 'var(--color-primary-cta)' }}>
            {t('leerMas')} →
          </span>
        </div>
      </article>
    </Link>
  )
}

// ── Empty state ───────────────────────────────────────────────────────────────

function EmptyState() {
  const t = useTranslations('HomePage')
  return (
    <div className="max-w-7xl mx-auto px-4 lg:px-8 py-16 text-center">
      <p className="text-base font-semibold" style={{ color: 'var(--color-on-surface-variant)' }}>
        {t('emptyTitulo')}
      </p>
      <p className="text-sm mt-2" style={{ color: 'var(--color-outline)' }}>
        {t('emptySubtitulo')}
      </p>
    </div>
  )
}

// ── Icons ─────────────────────────────────────────────────────────────────────
function CalendarIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
    </svg>
  )
}
function LocationIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  )
}
function NewsIconSvg({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
    </svg>
  )
}
