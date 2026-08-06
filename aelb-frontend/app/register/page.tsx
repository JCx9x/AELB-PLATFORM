'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { registerUser, loginUser, type ApiError } from '../lib/api'
import { saveSession } from '../lib/auth'

export default function RegisterPage() {
  const t      = useTranslations('Register')
  const router = useRouter()

  const nationalities = t.raw('nacionalidades') as Record<string, string>

  const [firstName, setFirstName]     = useState('')
  const [lastName, setLastName]       = useState('')
  const [nationality, setNationality] = useState('ES')
  const [dni, setDni]                 = useState('')
  const [email, setEmail]             = useState('')
  const [phone, setPhone]             = useState('')
  const [city, setCity]               = useState('')
  const [province, setProvince]       = useState('')
  const [password, setPassword]       = useState('')
  const [confirm, setConfirm]         = useState('')
  const [showPassword, setShowPassword]   = useState(false)
  const [showConfirm,  setShowConfirm]    = useState(false)
  const [acceptTerms, setAcceptTerms] = useState(false)

  const [error, setError]     = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)

    // Client-side checks
    if (password !== confirm) {
      setError(t('errorPasswordsNoCoinciden'))
      return
    }
    if (!acceptTerms) {
      setError(t('errorAceptarTerminos'))
      return
    }

    setLoading(true)
    try {
      await registerUser({
        email, password, firstName, lastName, dni, phone,
        city, province: province.trim() || null, nationality,
      })
      // Auto-login tras registro exitoso
      const auth = await loginUser(email, password)
      saveSession(auth)
      router.push('/')
    } catch (err) {
      const apiErr = err as ApiError
      setError(apiErr.detail ?? t('errorCrearCuenta'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex" style={{ backgroundColor: 'var(--color-surface)' }}>

      {/* ── Left brand panel (desktop) ── */}
      <div className="hidden lg:flex lg:w-[480px] xl:w-[560px] flex-shrink-0 flex-col relative overflow-hidden"
        style={{ backgroundColor: 'var(--color-dark)' }}>
        <div className="absolute inset-0 bg-gradient-to-br from-[#1a1c1c] via-[#2a1515] to-[#af101a]/40 pointer-events-none" />

        <div className="relative z-10 flex flex-col justify-center flex-1 px-10">
          <h1 className="text-white font-extrabold leading-[0.95] tracking-tight mb-6"
            style={{ fontSize: '3rem', letterSpacing: '-0.02em' }}>
            {t('brandLinea1')}<br />{t('brandLinea2')}
          </h1>
          <p className="text-white/50 text-base leading-relaxed max-w-xs mb-10">
            {t('brandSubtitulo')}
          </p>
          <div className="flex flex-col gap-4">
            {(t.raw('steps') as string[])
              .map((step, i) => (
                <div key={i} className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0 mt-0.5"
                    style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                    {i + 1}
                  </div>
                  <p className="text-white/60 text-sm leading-snug">{step}</p>
                </div>
              ))}
          </div>
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

        {/* Form */}
        <div className="flex-1 flex flex-col justify-center px-6 py-10 lg:px-16 xl:px-20 overflow-y-auto"
          style={{ backgroundColor: 'var(--color-surface-card)' }}>
          <div className="w-full max-w-md mx-auto">

            <div className="mb-8">
              <h2 className="font-bold mb-1"
                style={{ fontSize: '1.75rem', color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
                {t('crearCuenta')}
              </h2>
              <p className="text-base" style={{ color: 'var(--color-on-surface-variant)' }}>
                {t('completaInfo')}
              </p>
            </div>

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

            <form className="space-y-5" onSubmit={handleSubmit}>

              {/* Name row */}
              <div className="grid grid-cols-2 gap-3">
                <Field label={t('campoNombre')} htmlFor="firstName">
                  <TextInput id="firstName" type="text" autoComplete="given-name" required
                    placeholder={t('placeholderNombre')} value={firstName} onChange={(e) => setFirstName(e.target.value)} />
                </Field>
                <Field label={t('campoApellidos')} htmlFor="lastName">
                  <TextInput id="lastName" type="text" autoComplete="family-name" required
                    placeholder={t('placeholderApellidos')} value={lastName} onChange={(e) => setLastName(e.target.value)} />
                </Field>
              </div>

              {/* Nacionalidad */}
              <Field label={t('campoNacionalidad')} htmlFor="nationality">
                <select id="nationality" required value={nationality}
                  onChange={(e) => setNationality(e.target.value)}
                  className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all bg-white
                    focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                  style={{
                    backgroundColor: 'var(--color-surface)',
                    color: 'var(--color-on-surface)',
                    border: '1px solid var(--color-outline)',
                  }}>
                  {Object.entries(nationalities).map(([code, label]) => (
                    <option key={code} value={code}>{label}</option>
                  ))}
                </select>
              </Field>

              {/* DNI / NIE — depende de la nacionalidad */}
              <Field label={nationality === 'ES' ? t('campoDniEspanol') : t('campoDniExtranjero')} htmlFor="dni"
                hint={nationality === 'ES' ? t('dniHint') : t('dniHintExtranjero')}>
                <TextInput id="dni" type="text" autoComplete="off" required
                  placeholder={nationality === 'ES' ? '12345678A' : t('placeholderNie')}
                  value={dni} onChange={(e) => setDni(e.target.value.toUpperCase())} />
              </Field>

              {/* Email */}
              <Field label={t('campoEmail')} htmlFor="reg-email">
                <TextInput id="reg-email" type="email" autoComplete="email" required
                  placeholder={t('placeholderEmail')} value={email} onChange={(e) => setEmail(e.target.value)} />
              </Field>

              {/* Phone */}
              <Field label={t('campoTelefono')} htmlFor="phone">
                <TextInput id="phone" type="tel" autoComplete="tel" required
                  placeholder="+34 600 000 000" value={phone} onChange={(e) => setPhone(e.target.value)} />
              </Field>

              {/* Ciudad + Provincia */}
              <div className="grid grid-cols-2 gap-3">
                <Field label={t('campoCiudad')} htmlFor="city">
                  <TextInput id="city" type="text" autoComplete="address-level2" required
                    placeholder={t('placeholderCiudad')} value={city} onChange={(e) => setCity(e.target.value)} />
                </Field>
                <Field label={t('campoProvincia')} htmlFor="province" hint={t('provinciaOpcional')}>
                  <TextInput id="province" type="text" autoComplete="address-level1"
                    placeholder={t('placeholderProvincia')} value={province} onChange={(e) => setProvince(e.target.value)} />
                </Field>
              </div>

              {/* Password */}
              <Field label={t('campoContrasena')} htmlFor="reg-password">
                <PasswordInput id="reg-password" show={showPassword} required
                  onToggle={() => setShowPassword((v) => !v)}
                  placeholder={t('placeholderContrasena')} autoComplete="new-password"
                  value={password} onChange={(e) => setPassword(e.target.value)} />
              </Field>

              {/* Confirm */}
              <Field label={t('campoConfirmarContrasena')} htmlFor="reg-confirm">
                <PasswordInput id="reg-confirm" show={showConfirm} required
                  onToggle={() => setShowConfirm((v) => !v)}
                  placeholder={t('placeholderRepiteContrasena')} autoComplete="new-password"
                  value={confirm} onChange={(e) => setConfirm(e.target.value)} />
              </Field>

              {/* Terms */}
              <div className="flex items-start gap-3">
                <input id="terms" type="checkbox" required
                  checked={acceptTerms} onChange={(e) => setAcceptTerms(e.target.checked)}
                  className="w-4 h-4 mt-0.5 rounded-[0.125rem] cursor-pointer accent-[#d32f2f]" />
                <label htmlFor="terms" className="text-sm cursor-pointer leading-snug"
                  style={{ color: 'var(--color-on-surface-variant)' }}>
                  {t('aceptoLos')}{' '}
                  <Link href="/terms" className="font-semibold hover:underline" style={{ color: 'var(--color-primary)' }}>
                    {t('terminosDeUso')}
                  </Link>{' '}
                  {t('yLa')}{' '}
                  <Link href="/privacy" className="font-semibold hover:underline" style={{ color: 'var(--color-primary)' }}>
                    {t('politicaPrivacidad')}
                  </Link>
                </label>
              </div>

              {/* Submit */}
              <button type="submit" disabled={loading}
                className="w-full py-4 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}
                onMouseEnter={(e) => !loading && (e.currentTarget.style.backgroundColor = 'var(--color-primary)')}
                onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-primary-cta)')}>
                {loading && <SpinnerIcon className="w-4 h-4 animate-spin" />}
                {loading ? t('creandoCuenta') : t('crearCuentaFederativa')}
              </button>
            </form>

            <p className="mt-8 text-center text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
              {t('yaTienesCuenta')}{' '}
              <Link href="/login" className="font-bold transition-opacity hover:opacity-70"
                style={{ color: 'var(--color-primary)' }}>
                {t('iniciaSesion')}
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

// ── Shared sub-components ─────────────────────────────────────────────────────
function Field({ label, htmlFor, hint, children }: {
  label: string; htmlFor: string; hint?: string; children: React.ReactNode
}) {
  return (
    <div>
      <label htmlFor={htmlFor}
        className="block text-[11px] font-bold uppercase tracking-widest mb-2"
        style={{ color: 'var(--color-on-surface)' }}>
        {label}
      </label>
      {children}
      {hint && <p className="text-[11px] mt-1.5" style={{ color: 'var(--color-outline)' }}>{hint}</p>}
    </div>
  )
}

function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none transition-all
        focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
      style={{
        backgroundColor: 'var(--color-surface)',
        color: 'var(--color-on-surface)',
        border: '1px solid var(--color-outline)',
      }}
    />
  )
}

function PasswordInput({ id, show, onToggle, placeholder, autoComplete, value, onChange, required }: {
  id: string; show: boolean; onToggle: () => void
  placeholder: string; autoComplete?: string
  value: string; onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  required?: boolean
}) {
  const t = useTranslations('Register')
  return (
    <div className="relative">
      <input id={id} type={show ? 'text' : 'password'}
        placeholder={placeholder} autoComplete={autoComplete}
        value={value} onChange={onChange} required={required}
        className="w-full pl-4 pr-12 py-3 text-base rounded-[0.25rem] outline-none transition-all
          focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
        style={{
          backgroundColor: 'var(--color-surface)',
          color: 'var(--color-on-surface)',
          border: '1px solid var(--color-outline)',
        }}
      />
      <button type="button" aria-label={show ? t('ocultar') : t('mostrar')} onClick={onToggle}
        className="absolute inset-y-0 right-4 flex items-center transition-opacity hover:opacity-70"
        style={{ color: 'var(--color-outline)' }}>
        {show
          ? <EyeOffIcon className="w-4 h-4" />
          : <EyeIcon    className="w-4 h-4" />}
      </button>
    </div>
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
