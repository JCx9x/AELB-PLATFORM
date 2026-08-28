'use client'

import { useRef, useState } from 'react'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import { forgotPassword, type ApiError } from '../lib/api'

// UX only — the backend (PASSWORD_RESET_COOLDOWN_SECONDS, default 60s) is the
// real source of truth for how often a reset email can be requested. This just
// avoids showing an active "resend" button while the last request is almost
// certainly still within the backend's own cooldown window.
const RESEND_COOLDOWN_SECONDS = 60

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function maskEmail(rawEmail: string): string {
  const atIndex = rawEmail.indexOf('@')
  if (atIndex <= 0) return rawEmail
  return `${rawEmail[0]}***${rawEmail.slice(atIndex)}`
}

export default function ForgotPasswordPage() {
  const t = useTranslations('ForgotPassword')

  const [email, setEmail] = useState('')
  const [fieldError, setFieldError] = useState<string | null>(null)

  const [phase, setPhase] = useState<'form' | 'sent'>('form')
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [correlationId, setCorrelationId] = useState<string | null>(null)

  const [maskedEmail, setMaskedEmail] = useState('')
  const [lastEmail, setLastEmail] = useState('')
  const [cooldown, setCooldown] = useState(0)

  // Belt-and-suspenders against double-click: this is checked synchronously,
  // unlike React state, so two rapid clicks before the first re-render can't
  // both slip through. The disabled button attribute is the primary guard;
  // this is a second, state-update-timing-independent one.
  const submittingRef = useRef(false)

  function startCooldown() {
    setCooldown(RESEND_COOLDOWN_SECONDS)
    const timer = window.setInterval(() => {
      setCooldown((current) => {
        if (current <= 1) {
          window.clearInterval(timer)
          return 0
        }
        return current - 1
      })
    }, 1000)
  }

  async function sendResetRequest(targetEmail: string) {
    if (submittingRef.current) return
    submittingRef.current = true
    setLoading(true)
    setErrorMessage(null)
    setCorrelationId(null)

    try {
      await forgotPassword(targetEmail)
      // Same generic outcome regardless of whether the account exists —
      // the backend already responds identically either way; the UI must
      // never introduce a difference it didn't provide.
      setMaskedEmail(maskEmail(targetEmail))
      setLastEmail(targetEmail)
      setPhase('sent')
      startCooldown()
    } catch (err) {
      const apiErr = err as ApiError
      if (apiErr.status === 429) {
        setErrorMessage(apiErr.detail || t('errorRateLimit'))
        startCooldown()
      } else {
        setErrorMessage(t('errorGenerico'))
        setCorrelationId(apiErr.correlationId ?? null)
      }
    } finally {
      submittingRef.current = false
      setLoading(false)
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFieldError(null)

    const trimmed = email.trim()
    if (!trimmed) {
      setFieldError(t('errorEmailObligatorio'))
      return
    }
    if (!EMAIL_REGEX.test(trimmed)) {
      setFieldError(t('errorEmailInvalido'))
      return
    }

    void sendResetRequest(trimmed)
  }

  function handleResend() {
    if (cooldown > 0 || loading || !lastEmail) return
    void sendResetRequest(lastEmail)
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

        <div className="flex-1 flex flex-col justify-center px-6 py-10 lg:px-16 xl:px-20"
          style={{ backgroundColor: 'var(--color-surface-card)' }}>
          <div className="w-full max-w-md mx-auto">

            {phase === 'form' ? (
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
                    <label htmlFor="forgot-email"
                      className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                      style={{ color: 'var(--color-on-surface)' }}>
                      {t('campoEmail')}
                    </label>
                    <input
                      id="forgot-email"
                      name="email"
                      type="email"
                      autoComplete="email"
                      required
                      value={email}
                      onChange={(e) => { setEmail(e.target.value); if (fieldError) setFieldError(null) }}
                      placeholder={t('emailPlaceholder')}
                      aria-invalid={fieldError ? true : undefined}
                      aria-describedby={fieldError ? 'forgot-email-error' : undefined}
                      className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
                        focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                      style={{
                        backgroundColor: 'var(--color-surface)',
                        color: 'var(--color-on-surface)',
                        border: fieldError ? '1px solid var(--color-error)' : '1px solid var(--color-outline)',
                      }}
                    />
                    {fieldError && (
                      <p id="forgot-email-error" role="alert" className="text-[11px] mt-1.5" style={{ color: 'var(--color-error)' }}>
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
                    {loading ? t('enviando') : t('botonEnviar')}
                  </button>
                </form>

                <p className="mt-8 text-center text-sm">
                  <Link href="/login" className="font-bold transition-opacity hover:opacity-70"
                    style={{ color: 'var(--color-primary)' }}>
                    {t('volverLogin')}
                  </Link>
                </p>
              </>
            ) : (
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
                    {t('successTexto', { email: maskedEmail })}
                  </p>
                  <p className="text-[11px] mt-3" style={{ color: 'var(--color-outline)' }}>
                    {t('successHint')}
                  </p>
                </div>

                <div className="mt-6 text-center">
                  {cooldown > 0 ? (
                    <p className="text-xs" style={{ color: 'var(--color-outline)' }}>
                      {t('reenviarEn', { seconds: cooldown })}
                    </p>
                  ) : (
                    <button type="button" onClick={handleResend} disabled={loading}
                      className="text-xs font-bold uppercase tracking-widest transition-opacity hover:opacity-70 disabled:opacity-50 disabled:cursor-not-allowed"
                      style={{ color: 'var(--color-primary)' }}>
                      {loading ? t('enviando') : t('reenviarEnlace')}
                    </button>
                  )}
                  {errorMessage && (
                    <div className="flex flex-col gap-1 px-4 py-3 mt-4 rounded-[0.25rem] text-sm text-left"
                      style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)', border: '1px solid #fca5a5' }}>
                      <span>{errorMessage}</span>
                      {correlationId && (
                        <p className="text-[11px] opacity-70">{t('referencia', { id: correlationId })}</p>
                      )}
                    </div>
                  )}
                </div>

                <p className="mt-8 text-center text-sm">
                  <Link href="/login" className="font-bold transition-opacity hover:opacity-70"
                    style={{ color: 'var(--color-primary)' }}>
                    {t('volverLogin')}
                  </Link>
                </p>
              </div>
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

function SpinnerIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
    </svg>
  )
}
