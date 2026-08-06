'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { useTranslations, useLocale } from 'next-intl'
import Navbar from '../components/Navbar'
import ChampionshipPricingSummary from '../components/ChampionshipPricingSummary'
import { getChampionships, type Championship } from '../lib/api'

function dateLocale(locale: string) {
  return locale === 'en' ? 'en-US' : 'es-ES'
}

export default function CampeonatosPage() {
  const t = useTranslations('Campeonatos')
  const [championships, setChampionships] = useState<Championship[]>([])
  const [loading,       setLoading]       = useState(true)
  const [error,         setError]         = useState<string | null>(null)
  const [calendarOpen,  setCalendarOpen]  = useState(true)
  const [calendarYear,  setCalendarYear]  = useState(new Date().getFullYear())

  useEffect(() => {
    getChampionships()
      .then(setChampionships)
      .catch((err: { detail?: string }) => setError(err.detail ?? t('errorCarga')))
      .finally(() => setLoading(false))
  }, [t])

  const now      = new Date()
  const upcoming = championships.filter((c) => new Date(c.eventDate) >= now)
  const past     = championships.filter((c) => new Date(c.eventDate) < now)

  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-8 lg:py-12">

        {/* Page header */}
        <div className="mb-8">
          <h1 className="font-extrabold text-3xl lg:text-4xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.03em' }}>
            {t('titulo')}
          </h1>
          <p className="mt-2 text-sm lg:text-base" style={{ color: 'var(--color-on-surface-variant)' }}>
            {t('subtitulo')}
          </p>
        </div>

        {/* ── Annual calendar ──────────────────────────────────────────────── */}
        <section className="mb-10">
          {/* Collapsible header */}
          <button
            onClick={() => setCalendarOpen((o) => !o)}
            className="w-full flex items-center justify-between px-4 py-3 rounded-[0.25rem] border transition-colors"
            style={{
              backgroundColor: 'var(--color-surface-card)',
              borderColor:     'var(--color-outline-variant)',
              color:           'var(--color-on-surface)',
            }}>
            <div className="flex items-center gap-2">
              <CalendarIcon className="w-4 h-4" style={{ color: 'var(--color-primary)' }} />
              <span className="text-sm font-bold uppercase tracking-widest" style={{ color: 'var(--color-on-surface-variant)' }}>
                {t('calendarioTitulo')}
              </span>
            </div>
            <ChevronIcon open={calendarOpen} />
          </button>

          {/* Calendar body */}
          {calendarOpen && (
            <div
              className="mt-1 rounded-[0.25rem] border p-5"
              style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

              {/* Year navigation */}
              <div className="flex items-center justify-between mb-5">
                <button
                  onClick={() => setCalendarYear((y) => y - 1)}
                  className="flex items-center gap-1 px-3 py-1.5 rounded text-sm font-semibold transition-colors"
                  style={{ color: 'var(--color-on-surface-variant)' }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-surface)')}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}>
                  <ArrowLeftIcon className="w-4 h-4" /> {calendarYear - 1}
                </button>

                <span className="font-extrabold text-2xl" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.03em' }}>
                  {calendarYear}
                </span>

                <button
                  onClick={() => setCalendarYear((y) => y + 1)}
                  className="flex items-center gap-1 px-3 py-1.5 rounded text-sm font-semibold transition-colors"
                  style={{ color: 'var(--color-on-surface-variant)' }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-surface)')}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}>
                  {calendarYear + 1} <ArrowRightIcon className="w-4 h-4" />
                </button>
              </div>

              {loading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="w-6 h-6 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <AnnualCalendar championships={championships} year={calendarYear} />
              )}
            </div>
          )}
        </section>

        {/* ── Loading / error / empty ──────────────────────────────────────── */}
        {loading && (
          <div className="flex items-center justify-center py-24">
            <div className="w-8 h-8 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && (
          <div className="flex items-center gap-3 px-4 py-3 rounded-[0.25rem] text-sm mb-6"
            style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            <ExclamationIcon className="w-4 h-4 flex-shrink-0" />
            {error}
          </div>
        )}

        {!loading && !error && championships.length === 0 && (
          <div className="text-center py-24">
            <TrophyIcon className="w-12 h-12 mx-auto mb-4" style={{ color: 'var(--color-outline)' }} />
            <p className="text-base font-semibold" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('sinPublicar')}
            </p>
          </div>
        )}

        {/* ── Upcoming ──────────────────────────────────────────────────────── */}
        {!loading && upcoming.length > 0 && (
          <section className="mb-12">
            <h2 className="text-[11px] font-bold uppercase tracking-widest mb-4"
              style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('proximos')}
            </h2>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {upcoming.map((ch) => <ChampionshipCard key={ch.id} championship={ch} />)}
            </div>
          </section>
        )}

        {/* ── Past ─────────────────────────────────────────────────────────── */}
        {!loading && past.length > 0 && (
          <section>
            <h2 className="text-[11px] font-bold uppercase tracking-widest mb-4"
              style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('anteriores')}
            </h2>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {past.map((ch) => <ChampionshipCard key={ch.id} championship={ch} past />)}
            </div>
          </section>
        )}
      </div>
    </div>
  )
}

// ── Annual Calendar ────────────────────────────────────────────────────────────

function AnnualCalendar({ championships, year }: { championships: Championship[]; year: number }) {
  const t = useTranslations('Campeonatos')
  const router = useRouter()
  const MONTH_NAMES = t.raw('meses') as string[]
  const DOW_LABELS = t.raw('diasSemana') as string[]

  // "YYYY-MM-DD" → Championship[]
  const champsByDate: Record<string, Championship[]> = {}
  for (const ch of championships) {
    const key = ch.eventDate.slice(0, 10)
    if (!champsByDate[key]) champsByDate[key] = []
    champsByDate[key].push(ch)
  }

  const todayStr = new Date().toISOString().slice(0, 10)

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
      {MONTH_NAMES.map((monthName, mi) => {
        // Monday-first offset: JS getDay() 0=Sun → (d+6)%7 → Mon=0
        const startOffset = (new Date(year, mi, 1).getDay() + 6) % 7
        const daysInMonth = new Date(year, mi + 1, 0).getDate()

        const cells: (number | null)[] = Array(startOffset).fill(null)
        for (let d = 1; d <= daysInMonth; d++) cells.push(d)

        // Check if this month has any championship in this year
        const monthHasEvent = cells.some((day) => {
          if (!day) return false
          const key = `${year}-${String(mi + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
          return !!champsByDate[key]
        })

        return (
          <div
            key={mi}
            className="rounded p-3"
            style={{
              backgroundColor: monthHasEvent ? 'rgba(211,47,47,0.04)' : 'var(--color-surface)',
              border: monthHasEvent
                ? '1px solid rgba(211,47,47,0.18)'
                : '1px solid var(--color-outline-variant)',
            }}>
            {/* Month title */}
            <div className="text-[11px] font-bold uppercase tracking-wider mb-2"
              style={{ color: monthHasEvent ? 'var(--color-primary)' : 'var(--color-on-surface-variant)' }}>
              {monthName}
            </div>

            {/* Day-of-week header */}
            <div className="grid grid-cols-7 mb-1">
              {DOW_LABELS.map((d) => (
                <div key={d} className="text-center text-[9px] font-semibold"
                  style={{ color: 'var(--color-on-surface-variant)' }}>
                  {d}
                </div>
              ))}
            </div>

            {/* Day cells */}
            <div className="grid grid-cols-7 gap-y-0.5">
              {cells.map((day, i) => {
                if (!day) return <div key={i} />
                const key = `${year}-${String(mi + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
                const champs = champsByDate[key]
                const isToday = key === todayStr

                return (
                  <DayCell
                    key={i}
                    day={day}
                    champs={champs}
                    isToday={isToday}
                    onNavigate={(id) => router.push(`/campeonatos/${id}`)}
                  />
                )
              })}
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ── Day cell with tooltip ──────────────────────────────────────────────────────

function DayCell({
  day, champs, isToday, onNavigate,
}: {
  day: number
  champs?: Championship[]
  isToday: boolean
  onNavigate: (id: string) => void
}) {
  const [showTip, setShowTip] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  const hasEvent = !!champs && champs.length > 0

  return (
    <div
      ref={ref}
      className="relative flex items-center justify-center"
      style={{ height: 22 }}
      onMouseEnter={() => hasEvent && setShowTip(true)}
      onMouseLeave={() => setShowTip(false)}
      onClick={() => {
        if (!champs) return
        if (champs.length === 1) onNavigate(champs[0].id)
        else setShowTip((v) => !v)
      }}>

      {/* Day number */}
      <span
        className="flex items-center justify-center rounded-full text-[11px] font-medium select-none"
        style={{
          width:           22,
          height:          22,
          cursor:          hasEvent ? 'pointer' : 'default',
          backgroundColor: hasEvent ? 'var(--color-primary)' : isToday ? 'rgba(211,47,47,0.12)' : 'transparent',
          color:           hasEvent ? '#fff' : isToday ? 'var(--color-primary)' : 'var(--color-on-surface)',
          fontWeight:      hasEvent ? 700 : isToday ? 700 : 400,
          border:          isToday && !hasEvent ? '1.5px solid var(--color-primary)' : 'none',
        }}>
        {day}
      </span>

      {/* Tooltip */}
      {showTip && champs && (
        <div
          className="absolute z-50 bottom-full mb-1.5 left-1/2 -translate-x-1/2 px-2 py-1.5 rounded shadow-lg text-[11px] whitespace-nowrap pointer-events-none"
          style={{
            backgroundColor: 'var(--color-on-surface)',
            color:           'var(--color-surface)',
            minWidth:        120,
            maxWidth:        220,
            whiteSpace:      'normal',
            textAlign:       'center',
          }}>
          {champs.map((c) => (
            <div key={c.id} className="font-semibold leading-snug">{c.name}</div>
          ))}
          <div
            className="absolute left-1/2 -translate-x-1/2 top-full"
            style={{
              width: 0, height: 0,
              borderLeft:  '5px solid transparent',
              borderRight: '5px solid transparent',
              borderTop:   '5px solid var(--color-on-surface)',
            }}
          />
        </div>
      )}
    </div>
  )
}

// ── Championship card ──────────────────────────────────────────────────────────

function ChampionshipCard({ championship: ch, past }: { championship: Championship; past?: boolean }) {
  const t = useTranslations('Campeonatos')
  const locale = useLocale()
  const eventDate = new Date(ch.eventDate)
  const now       = new Date()
  const todayStr  = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  const registrationOpen = ch.visible && todayStr <= ch.registrationDeadline

  const dateStr = eventDate.toLocaleDateString(dateLocale(locale), {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
  })

  return (
    <article
      className="rounded-[0.25rem] border overflow-hidden flex flex-col transition-shadow hover:shadow-md"
      style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)', opacity: past ? 0.75 : 1 }}>

      {ch.imageUrl ? (
        <div className="h-40 overflow-hidden flex-shrink-0">
          <img src={ch.imageUrl} alt={ch.name} className="w-full h-full object-cover" />
        </div>
      ) : (
        <div className="h-40 flex-shrink-0 flex items-center justify-center"
          style={{ backgroundColor: 'var(--color-surface)' }}>
          <TrophyIcon className="w-12 h-12" style={{ color: 'var(--color-outline)' }} />
        </div>
      )}

      <div className="p-5 flex flex-col flex-1 gap-3">
        <div className="flex items-center gap-2">
          {past ? (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider"
              style={{ backgroundColor: 'rgba(100,100,100,0.1)', color: 'var(--color-on-surface-variant)' }}>
              {t('finalizado')}
            </span>
          ) : registrationOpen ? (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider"
              style={{ backgroundColor: 'rgba(5,150,105,0.1)', color: '#047857' }}>
              {t('inscripcionesAbiertas')}
            </span>
          ) : (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider"
              style={{ backgroundColor: 'rgba(220,38,38,0.1)', color: 'var(--color-error)' }}>
              {t('inscripcionesCerradas')}
            </span>
          )}
        </div>

        <h3 className="font-bold text-base leading-snug" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
          {ch.name}
        </h3>

        <div className="flex flex-col gap-1.5 text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
          <div className="flex items-center gap-2">
            <CalendarIcon className="w-3.5 h-3.5 flex-shrink-0" />
            <span className="capitalize">{dateStr}</span>
          </div>
          <div className="flex items-center gap-2">
            <LocationIcon className="w-3.5 h-3.5 flex-shrink-0" />
            <span>{ch.location}</span>
          </div>
          <div className="flex items-center gap-2">
            <TagIcon className="w-3.5 h-3.5 flex-shrink-0" />
            <span>{t('categoriaCount', { count: ch.categoryIds.length })}</span>
          </div>
        </div>

        {ch.description && (
          <p className="text-sm line-clamp-2" style={{ color: 'var(--color-on-surface-variant)' }}>
            {ch.description}
          </p>
        )}

        <div className="mt-auto pt-3 border-t flex items-center justify-between"
          style={{ borderColor: 'var(--color-outline-variant)' }}>
          <ChampionshipPricingSummary />
          <Link
            href={!past && registrationOpen ? `/campeonatos/${ch.id}?scroll=inscripcion` : `/campeonatos/${ch.id}`}
            className="px-4 py-2 text-xs font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
            {!past && registrationOpen ? t('inscribirse') : t('verDetalles')}
          </Link>
        </div>
      </div>
    </article>
  )
}

// ── Icons ─────────────────────────────────────────────────────────────────────

function ChevronIcon({ open }: { open: boolean }) {
  return (
    <svg
      className="w-4 h-4 transition-transform"
      style={{ transform: open ? 'rotate(180deg)' : 'rotate(0deg)', color: 'var(--color-on-surface-variant)' }}
      fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
    </svg>
  )
}
function ArrowLeftIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
    </svg>
  )
}
function ArrowRightIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
    </svg>
  )
}
function TrophyIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M16 4h2a2 2 0 012 2v1c0 4.418-3.134 8.107-7.342 8.942M8 4H6a2 2 0 00-2 2v1c0 4.418 3.134 8.107 7.342 8.942M12 20v-4m0 0a4 4 0 004-4H8a4 4 0 004 4z" />
    </svg>
  )
}
function CalendarIcon({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <svg className={className} style={style} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
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
function TagIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
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
