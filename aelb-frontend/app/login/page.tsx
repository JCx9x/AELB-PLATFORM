'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { loginUser, type ApiError } from '../lib/api'
import { saveSession } from '../lib/auth'

function LoginForm() {
  const t             = useTranslations('Login')
  const router       = useRouter()
  const searchParams = useSearchParams()
  const returnTo     = searchParams.get('returnTo') ?? '/'

  const [email, setEmail]             = useState('')
  const [password, setPassword]       = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberDevice, setRememberDevice] = useState(false)
  const [error, setError]             = useState<string | null>(null)
  const [loading, setLoading]         = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const auth = await loginUser(email, password)
      saveSession(auth)
      router.push(returnTo)
    } catch (err) {
      const apiErr = err as ApiError
      if (apiErr.status === 401 || apiErr.status === 403) {
        setError(apiErr.detail)
      } else {
        setError(t('errorConexion'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex" style={{ backgroundColor: 'var(--color-surface)' }}>

      {/* ── Left brand panel (desktop only) ── */}
      <div className="hidden lg:flex lg:w-[480px] xl:w-[560px] flex-shrink-0 flex-col relative overflow-hidden"
        style={{ backgroundColor: 'var(--color-dark)' }}>
        <div className="absolute inset-0 bg-gradient-to-br from-[#1a1c1c] via-[#2a1515] to-[#af101a]/40 pointer-events-none" />

        <div className="relative z-10 flex flex-col justify-center flex-1 px-10 pb-10">
       
          <h1 className="text-white font-extrabold leading-[0.95] tracking-tight mb-6"
            style={{ fontSize: '3.5rem', letterSpacing: '-0.02em' }}>
            {t('brandLinea1')}<br />{t('brandLinea2')}<br />{t('brandLinea3')}
          </h1>
          <p className="text-white/50 text-base leading-relaxed max-w-xs">
            {t('brandSubtitulo')}
          </p>

        </div>

        <div className="relative z-10 px-10 py-6 border-t border-white/10">
          <p className="text-white/30 text-xs">{t('copyright')}</p>
        </div>
      </div>

      {/* ── Right form panel ── */}
      <div className="flex flex-col flex-1">

        {/* Mobile header */}
        <div className="lg:hidden px-6 pt-8 pb-6" style={{ backgroundColor: 'var(--color-dark)' }}>
          <h2 className="text-white font-extrabold text-2xl leading-tight tracking-tight">
            {t('mobileHeaderTitulo')}
          </h2>
        </div>

        {/* Form area */}
        <div className="flex-1 flex flex-col justify-center px-6 py-10 lg:px-16 xl:px-20"
          style={{ backgroundColor: 'var(--color-surface-card)' }}>
          <div className="w-full max-w-md mx-auto">

        
            {/* Error banner */}
            {error && (
              <div className="flex items-start gap-3 px-4 py-3 mb-6 rounded-[0.25rem] text-sm"
                style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)', border: '1px solid #fca5a5' }}>
                <svg className="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round"
                    d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {error}
              </div>
            )}

             <div className="relative flex items-center py-1">
                <div className="flex-1" style={{ borderColor: 'var(--color-outline-variant)' }} />
                <span className="text-[11px] font-semibold uppercase tracking-widest"
                  style={{ color: 'var(--color-outline)' }}>{t('loginParaInscribirte')}</span>
                <div className="flex-1 border-t" style={{ borderColor: 'var(--color-outline-variant)' }} />
              </div>

            <form className="space-y-5" onSubmit={handleSubmit}>

              {/* Email */}
              <div>
                <label htmlFor="email"
                  className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                  style={{ color: 'var(--color-on-surface)' }}>
                  {t('email')}
                </label>
                <div className="relative">
                  <span className="absolute inset-y-0 left-4 flex items-center pointer-events-none"
                    style={{ color: 'var(--color-outline)' }}>
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round"
                        d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                    </svg>
                  </span>
                  <input
                    id="email" type="email" autoComplete="email" required
                    value={email} onChange={(e) => setEmail(e.target.value)}
                    placeholder={t('emailPlaceholder')}
                    className="w-full pl-11 pr-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                      focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                    style={{
                      backgroundColor: 'var(--color-surface)',
                      color: 'var(--color-on-surface)',
                      border: '1px solid var(--color-outline)',
                    }}
                  />
                </div>
              </div>

              {/* Password */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label htmlFor="password"
                    className="block text-[11px] font-bold uppercase tracking-widest"
                    style={{ color: 'var(--color-on-surface)' }}>
                    {t('contrasena')}
                  </label>
                  <Link href="/forgot-password"
                    className="text-xs font-semibold transition-opacity hover:opacity-70"
                    style={{ color: 'var(--color-primary)' }}>
                    {t('olvidasteContrasena')}
                  </Link>
                </div>
                <div className="relative">
                  <span className="absolute inset-y-0 left-4 flex items-center pointer-events-none"
                    style={{ color: 'var(--color-outline)' }}>
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round"
                        d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                  </span>
                  <input
                    id="password" type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password" required
                    value={password} onChange={(e) => setPassword(e.target.value)}
                    placeholder={t('contrasenaPlaceholder')}
                    className="w-full pl-11 pr-12 py-3 text-base rounded-[0.25rem] outline-none transition-all
                      focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                    style={{
                      backgroundColor: 'var(--color-surface)',
                      color: 'var(--color-on-surface)',
                      border: '1px solid var(--color-outline)',
                    }}
                  />
                  <button type="button"
                    aria-label={showPassword ? t('ocultarContrasena') : t('mostrarContrasena')}
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute inset-y-0 right-4 flex items-center transition-opacity hover:opacity-70"
                    style={{ color: 'var(--color-outline)' }}>
                    {showPassword
                      ? <EyeOffIcon className="w-4 h-4" />
                      : <EyeIcon    className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Remember */}
              <div className="flex items-center gap-3">
                <input id="remember" type="checkbox" checked={rememberDevice}
                  onChange={(e) => setRememberDevice(e.target.checked)}
                  className="w-4 h-4 rounded-[0.125rem] cursor-pointer accent-[#d32f2f]" />
                <label htmlFor="remember" className="text-sm cursor-pointer"
                  style={{ color: 'var(--color-on-surface-variant)' }}>
                  {t('recordarDispositivo')}
                </label>
              </div>

              {/* Submit */}
              <button type="submit" disabled={loading}
                className="w-full py-4 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}
                onMouseEnter={(e) => !loading && (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
                onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
                {loading && <SpinnerIcon className="w-4 h-4 animate-spin" />}
                {loading ? t('autenticando') : t('loginToAelb')}
              </button>
            </form>

            <p className="mt-8 text-center text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('nuevoEnAelb')}{' '}
              <Link href="/register" className="font-bold transition-opacity hover:opacity-70"
                style={{ color: 'var(--color-primary)' }}>
                {t('creaTuCuenta')}
              </Link>
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t" style={{
          backgroundColor: 'var(--color-surface-card)',
          borderColor: 'var(--color-outline-variant)',
        }}>
          <p className="text-center text-[11px]" style={{ color: 'var(--color-outline)' }}>
            {t('copyright')}
            {' · '}<Link href="/privacy" className="hover:underline">{t('privacidad')}</Link>
            {' · '}<Link href="/terms" className="hover:underline">{t('terminos')}</Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-screen" style={{ backgroundColor: 'var(--color-surface)' }} />}>
      <LoginForm />
    </Suspense>
  )
}

// ── Icons ─────────────────────────────────────────────────────────────────────
function EyeIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
    </svg>
  )
}

function EyeOffIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
    </svg>
  )
}

function SpinnerIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
    </svg>
  )
}

function IdIcon() {
  return (
    <svg className="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2" />
    </svg>
  )
}
