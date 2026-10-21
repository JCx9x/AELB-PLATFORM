'use client'

import { Suspense, useRef, useState } from 'react'
import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { resetPassword, type ApiError } from '../lib/api'

function ResetPasswordForm() {
  const t = useTranslations('ResetPassword')
  const token = useSearchParams().get('token') ?? ''

  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [fieldError, setFieldError] = useState<string | null>(null)

  const [done, setDone] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [correlationId, setCorrelationId] = useState<string | null>(null)

  // Same double-submit guard used on the forgot-password screen.
  const submittingRef = useRef(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFieldError(null)

    if (password.length < 8) {
      setFieldError(t('errorPasswordMinimo'))
      return
    }
    if (password !== confirm) {
      setFieldError(t('errorPasswordsNoCoinciden'))
      return
    }

    if (submittingRef.current) return
    submittingRef.current = true
    setLoading(true)
    setErrorMessage(null)
    setCorrelationId(null)

    try {
      await resetPassword(token, password)
      setDone(true)
    } catch (err) {
      const apiErr = err as ApiError
      if (apiErr.status === 429) {
        setErrorMessage(apiErr.detail || t('errorRateLimit'))
      } else if (apiErr.status === 422 || apiErr.status === 400) {
        // Invalid/expired/already-used token — backend message is already
        // safe to show verbatim (see ResetPasswordUseCase.InvalidResetTokenException).
        setErrorMessage(apiErr.detail || t('errorTokenAusente'))
      } else {
        setErrorMessage(t('errorGenerico'))
        setCorrelationId(apiErr.correlationId ?? null)
      }
    } finally {
      submittingRef.current = false
      setLoading(false)
    }
  }

  const missingToken = token.trim() === ''

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

        <div className="flex-1 flex flex-col justify-center px-6 py-10 lg:px-16 xl:px-20"
          style={{ backgroundColor: 'var(--color-surface-card)' }}>
          <div className="w-full max-w-md mx-auto">

            {done ? (
              <div aria-live="polite">
                <div className="flex flex-col items-center text-center mb-2">
                  <div className="w-14 h-14 rounded-full flex items-center justify-center mb-5"
                    style={{ backgroundColor: '#dcfce7' }}>
                    <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="#15803d" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <h2 className="font-bold mb-2"
                    style={{ fontSize: '1.75rem', color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                    {t('successTitulo')}
                  </h2>
                  <p className="text-sm leading-relaxed" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('successTexto')}
                  </p>
                </div>
                <p className="mt-8 text-center text-sm">
                  <Link href="/login" className="font-bold transition-opacity hover:opacity-70"
                    style={{ color: 'var(--color-primary)' }}>
                    {t('irALogin')}
                  </Link>
                </p>
              </div>
            ) : missingToken ? (
              <div aria-live="polite">
                <div className="flex items-start gap-3 px-4 py-3 mb-6 rounded-[0.25rem] text-sm"
                  style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)', border: '1px solid #fca5a5' }}>
                  <svg className="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round"
                      d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  {t('errorTokenAusente')}
                </div>
                <p className="text-center text-sm">
                  <Link href="/forgot-password" className="font-bold transition-opacity hover:opacity-70"
                    style={{ color: 'var(--color-primary)' }}>
                    {t('solicitarNuevoEnlace')}
                  </Link>
                </p>
              </div>
            ) : (
              <>
                <div className="mb-8">
                  <h2 className="font-bold mb-2"
                    style={{ fontSize: '1.75rem', color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                    {t('titulo')}
                  </h2>
                  <p className="text-sm leading-relaxed" style={{ color: 'var(--color-on-surface-variant)' }}>
                    {t('instrucciones')}
                  </p>
                </div>

                <div aria-live="polite">
                  {errorMessage && (
                    <div className="flex flex-col gap-1 px-4 py-3 mb-6 rounded-[0.25rem] text-sm"
                      style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)', border: '1px solid #fca5a5' }}>
                      <div className="flex items-start gap-3">
                        <svg className="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round"
                            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <span>{errorMessage}</span>
                      </div>
                      {correlationId && (
                        <p className="text-[11px] opacity-70 pl-7">{t('referencia', { id: correlationId })}</p>
                      )}
                    </div>
                  )}
                </div>

                <form className="space-y-5" onSubmit={handleSubmit} noValidate>
                  <div>
                    <label htmlFor="reset-password"
                      className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                      style={{ color: 'var(--color-on-surface)' }}>
                      {t('campoNuevaContrasena')}
                    </label>
                    <div className="relative">
                      <input
                        id="reset-password" type={showPassword ? 'text' : 'password'}
                        autoComplete="new-password" required minLength={8}
                        value={password} onChange={(e) => { setPassword(e.target.value); if (fieldError) setFieldError(null) }}
                        placeholder={t('placeholderContrasena')}
                        aria-describedby={fieldError ? 'reset-password-error' : 'reset-password-hint'}
                        className="w-full pl-4 pr-12 py-3 text-base rounded-[0.25rem] outline-none transition-all
                          focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                        style={{
                          backgroundColor: 'var(--color-surface)',
                          color: 'var(--color-on-surface)',
                          border: fieldError ? '1px solid var(--color-error)' : '1px solid var(--color-outline)',
                        }}
                      />
                      <button type="button"
                        aria-label={showPassword ? t('ocultarContrasena') : t('mostrarContrasena')}
                        onClick={() => setShowPassword((v) => !v)}
                        className="absolute inset-y-0 right-4 flex items-center transition-opacity hover:opacity-70"
                        style={{ color: 'var(--color-outline)' }}>
                        {showPassword ? <EyeOffIcon className="w-4 h-4" /> : <EyeIcon className="w-4 h-4" />}
                      </button>
                    </div>
                    <p id="reset-password-hint" className="text-[11px] mt-1.5" style={{ color: 'var(--color-outline)' }}>
                      {t('minimo8Caracteres')}
                    </p>
                  </div>

                  <div>
                    <label htmlFor="reset-confirm"
                      className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                      style={{ color: 'var(--color-on-surface)' }}>
                      {t('campoConfirmarContrasena')}
                    </label>
                    <div className="relative">
                      <input
                        id="reset-confirm" type={showConfirm ? 'text' : 'password'}
                        autoComplete="new-password" required
                        value={confirm} onChange={(e) => { setConfirm(e.target.value); if (fieldError) setFieldError(null) }}
                        placeholder={t('placeholderContrasena')}
                        aria-invalid={fieldError ? true : undefined}
                        aria-describedby={fieldError ? 'reset-password-error' : undefined}
                        className="w-full pl-4 pr-12 py-3 text-base rounded-[0.25rem] outline-none transition-all
                          focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                        style={{
                          backgroundColor: 'var(--color-surface)',
                          color: 'var(--color-on-surface)',
                          border: fieldError ? '1px solid var(--color-error)' : '1px solid var(--color-outline)',
                        }}
                      />
                      <button type="button"
                        aria-label={showConfirm ? t('ocultarContrasena') : t('mostrarContrasena')}
                        onClick={() => setShowConfirm((v) => !v)}
                        className="absolute inset-y-0 right-4 flex items-center transition-opacity hover:opacity-70"
                        style={{ color: 'var(--color-outline)' }}>
                        {showConfirm ? <EyeOffIcon className="w-4 h-4" /> : <EyeIcon className="w-4 h-4" />}
                      </button>
                    </div>
                    {fieldError && (
                      <p id="reset-password-error" role="alert" className="text-[11px] mt-1.5" style={{ color: 'var(--color-error)' }}>
                        {fieldError}
                      </p>
                    )}
                  </div>

                  <button type="submit" disabled={loading}
                    className="w-full py-4 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    style={{ backgroundColor: 'var(--color-primary-cta)' }}
                    onMouseEnter={(e) => !loading && (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
                    {loading && <SpinnerIcon className="w-4 h-4 animate-spin" />}
                    {loading ? t('guardando') : t('botonCambiar')}
                  </button>
                </form>

                <p className="mt-8 text-center text-sm">
                  <Link href="/login" className="font-bold transition-opacity hover:opacity-70"
                    style={{ color: 'var(--color-primary)' }}>
                    {t('irALogin')}
                  </Link>
                </p>
              </>
            )}
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

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="min-h-screen" style={{ backgroundColor: 'var(--color-surface)' }} />}>
      <ResetPasswordForm />
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
