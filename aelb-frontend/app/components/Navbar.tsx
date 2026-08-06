'use client'

import { useState, useEffect, useRef } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { getSession, clearSession, type Session } from '../lib/auth'
import { logoutUser } from '../lib/api'
import LanguageSwitcher from './LanguageSwitcher'

const NAV_LINKS = [
  { href: '/',               key: 'inicio' },
  { href: '/campeonatos',    key: 'campeonatos' },
  { href: '/noticias',       key: 'noticias' },
  { href: '/resultados',     key: 'resultados' },
  { href: '/documentacion',  key: 'documentacion' },
  { href: '/equipos',        key: 'equipos' },
] as const

const GESTION_LINKS = [
  { href: '/admin/campeonatos',   key: 'campeonatos' },
  { href: '/admin/categorias',    key: 'categorias' },
  { href: '/admin/inscripciones', key: 'inscripciones' },
  { href: '/admin/noticias',      key: 'noticias' },
  { href: '/admin/resultados',    key: 'resultados' },
  { href: '/admin/documentacion', key: 'documentacion' },
  { href: '/admin/cuotas',        key: 'cuotas' },
  { href: '/admin/usuarios',      key: 'usuarios' },
] as const

function initials(firstName: string, lastName: string) {
  return ((firstName[0] ?? '') + (lastName[0] ?? '')).toUpperCase()
}

function isAdmin(session: Session | null) {
  return session?.role === 'GESTOR' || session?.role === 'ADMIN'
}

export default function Navbar() {
  const t = useTranslations('Navbar')
  const [menuOpen,     setMenuOpen]     = useState(false)
  const [gestionOpen,  setGestionOpen]  = useState(false)
  const [mobileAdmin,  setMobileAdmin]  = useState(false)
  const [session,      setSession]      = useState<Session | null>(null)

  const pathname      = usePathname()
  const router        = useRouter()
  const dropdownRef   = useRef<HTMLDivElement>(null)

  useEffect(() => { setSession(getSession()) }, [])

  // Close dropdown on route change
  useEffect(() => { setGestionOpen(false); setMenuOpen(false) }, [pathname])

  // Close dropdown on outside click
  useEffect(() => {
    if (!gestionOpen) return
    function handleClick(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setGestionOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [gestionOpen])

  async function handleLogout() {
    try {
      await logoutUser()
    } finally {
      clearSession()
      router.push('/login')
    }
  }

  const onAdminRoute = pathname.startsWith('/admin')

  return (
    <>
      {/* ── Top navbar ─────────────────────────────────────────────────────── */}
      <header className="sticky top-0 z-50 border-b"
        style={{ backgroundColor: 'var(--color-dark)', borderColor: 'rgba(255,255,255,0.08)' }}>
        <div className="max-w-7xl mx-auto px-4 lg:px-8">
          <div className="flex items-center justify-between h-14 lg:h-16">

            {/* Left: hamburger (mobile) + logo */}
            <div className="flex items-center gap-3">
              <button className="lg:hidden p-1.5 text-white/70 hover:text-white"
                onClick={() => setMenuOpen((v) => !v)} aria-label={t('abrirMenu')}>
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>

            </div>

            {/* Center: desktop nav */}
            <nav className="hidden lg:flex items-center gap-1">
              {NAV_LINKS.map(({ href, key }) => (
                <Link key={href} href={href}
                  className={`px-4 py-2 text-sm font-semibold uppercase tracking-wider rounded-[0.25rem] transition-colors ${
                    pathname === href
                      ? 'text-white bg-white/10'
                      : 'text-white/60 hover:text-white hover:bg-white/5'
                  }`}>
                  {t(`links.${key}`)}
                </Link>
              ))}

              {/* ── Gestión dropdown (GESTOR / ADMIN only) ─── */}
              {isAdmin(session) && (
                <div ref={dropdownRef} className="relative ml-1">
                  <button
                    onClick={() => setGestionOpen((v) => !v)}
                    className={`flex items-center gap-1.5 px-4 py-2 text-sm font-semibold uppercase tracking-wider rounded-[0.25rem] transition-colors ${
                      onAdminRoute || gestionOpen
                        ? 'text-white bg-white/10'
                        : 'text-white/60 hover:text-white hover:bg-white/5'
                    }`}
                    aria-expanded={gestionOpen}
                    aria-haspopup="true">
                    <ShieldIcon className="w-3.5 h-3.5" />
                    {t('gestion.titulo')}
                    <ChevronIcon className={`w-3 h-3 transition-transform ${gestionOpen ? 'rotate-180' : ''}`} />
                  </button>

                  {gestionOpen && (
                    <div className="absolute left-0 top-full mt-1 w-64 rounded-[0.25rem] shadow-2xl border overflow-hidden z-50"
                      style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>
                      <div className="px-4 py-2 border-b"
                        style={{ borderColor: 'var(--color-outline-variant)', backgroundColor: 'var(--color-surface)' }}>
                        <p className="text-[10px] font-bold uppercase tracking-widest"
                          style={{ color: 'var(--color-on-surface-variant)' }}>
                          {t('gestion.panel')}
                        </p>
                      </div>
                      {GESTION_LINKS.map(({ href, key }) => {
                        const active = pathname.startsWith(href)
                        return (
                          <Link key={href} href={href}
                            className="flex items-start gap-3 px-4 py-3 transition-colors"
                            style={{
                              backgroundColor: active ? 'rgba(211,47,47,0.08)' : 'transparent',
                              color: 'var(--color-on-surface)',
                            }}
                            onMouseEnter={(e) => { if (!active) e.currentTarget.style.backgroundColor = 'var(--color-surface)' }}
                            onMouseLeave={(e) => { if (!active) e.currentTarget.style.backgroundColor = 'transparent' }}>
                            <div className="mt-0.5 w-1.5 h-1.5 rounded-full flex-shrink-0"
                              style={{ backgroundColor: active ? 'var(--color-primary-cta)' : 'var(--color-outline)' }} />
                            <div>
                              <p className="text-sm font-semibold leading-none"
                                style={{ color: active ? 'var(--color-primary-cta)' : 'var(--color-on-surface)' }}>
                                {t(`gestion.items.${key}.label`)}
                              </p>
                              <p className="text-xs mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
                                {t(`gestion.items.${key}.desc`)}
                              </p>
                            </div>
                          </Link>
                        )
                      })}
                    </div>
                  )}
                </div>
              )}
            </nav>

            {/* Right: language + bell + user */}
            <div className="flex items-center gap-4">
              <div className="hidden lg:block">
                <LanguageSwitcher />
              </div>

              {session ? (
                <>
                  <Link href="/perfil" className="flex items-center gap-2 group">
                    <div className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold"
                      style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                      {initials(session.firstName, session.lastName)}
                    </div>
                    <div className="hidden lg:block">
                      <p className="text-white text-sm font-semibold leading-none">
                        {session.firstName} {session.lastName[0]}.
                      </p>
                    </div>
                  </Link>

                  <button onClick={handleLogout}
                    className="hidden lg:flex items-center text-white/40 hover:text-white/70 transition-colors p-1.5"
                    aria-label={t('cerrarSesion')}>
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round"
                        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                  </button>
                </>
              ) : (
                <Link href="/login"
                  className="px-4 py-2 text-sm font-bold uppercase tracking-wider text-white rounded-[0.25rem]"
                  style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                  {t('acceder')}
                </Link>
              )}
            </div>
          </div>
        </div>

        {/* ── Mobile dropdown ─────────────────────────────────────────────── */}
        {menuOpen && (
          <div className="lg:hidden border-t" style={{ borderColor: 'rgba(255,255,255,0.08)' }}>
            <nav className="px-4 py-3 flex flex-col gap-1">
              {NAV_LINKS.map(({ href, key }) => (
                <Link key={href} href={href}
                  onClick={() => setMenuOpen(false)}
                  className={`px-3 py-2.5 text-sm font-semibold uppercase tracking-wider rounded-[0.25rem] transition-colors ${
                    pathname === href
                      ? 'text-white bg-white/10'
                      : 'text-white/60 hover:text-white hover:bg-white/5'
                  }`}>
                  {t(`links.${key}`)}
                </Link>
              ))}

              {/* Gestión section — collapsible on mobile */}
              {isAdmin(session) && (
                <div className="mt-1 pt-1 border-t border-white/10">
                  <button
                    onClick={() => setMobileAdmin((v) => !v)}
                    className="w-full flex items-center justify-between px-3 py-2 rounded-[0.25rem] transition-colors text-white/60 hover:text-white hover:bg-white/5">
                    <span className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider">
                      <ShieldIcon className="w-3.5 h-3.5" />
                      {t('gestion.titulo')}
                    </span>
                    <ChevronIcon className={`w-3.5 h-3.5 transition-transform ${mobileAdmin ? 'rotate-180' : ''}`} />
                  </button>

                  {mobileAdmin && (
                    <div className="mt-1 space-y-0.5 pl-2">
                      {GESTION_LINKS.map(({ href, key }) => (
                        <Link key={href} href={href}
                          onClick={() => setMenuOpen(false)}
                          className={`flex items-center gap-2 px-3 py-2.5 text-sm font-semibold rounded-[0.25rem] transition-colors ${
                            pathname.startsWith(href)
                              ? 'text-white bg-white/10'
                              : 'text-white/50 hover:text-white hover:bg-white/5'
                          }`}>
                          <span className="w-1 h-1 rounded-full bg-current opacity-60" />
                          {t(`gestion.items.${key}.label`)}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <div className="mt-2 pt-2 border-t border-white/10 flex items-center justify-between">
                {session ? (
                  <button onClick={handleLogout}
                    className="text-left px-3 py-2.5 text-sm font-semibold uppercase tracking-wider text-white/40 hover:text-white/70">
                    {t('cerrarSesion')}
                  </button>
                ) : (
                  <Link href="/login" onClick={() => setMenuOpen(false)}
                    className="block px-3 py-2.5 text-sm font-semibold uppercase tracking-wider text-white/60 hover:text-white">
                    {t('acceder')}
                  </Link>
                )}
                <LanguageSwitcher />
              </div>
            </nav>
          </div>
        )}
      </header>

    </>
  )
}

// ── Icons ─────────────────────────────────────────────────────────────────────
function ShieldIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  )
}

function ChevronIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
    </svg>
  )
}

function HomeIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
    </svg>
  )
}

function TrophyIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z" />
    </svg>
  )
}

function NewsIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
    </svg>
  )
}

function StatsIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
    </svg>
  )
}

function PersonIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
    </svg>
  )
}
