'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { updateRecoveryEmail, type ApiError, type UserProfile } from '../lib/api'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * Correo secundario para recuperar la contraseña cuando el usuario no recuerda
 * el principal. Solo se usa una vez verificado mediante el enlace que se le envía.
 */
export default function RecoveryEmailCard({ profile, onUpdated }: {
  profile: UserProfile
  onUpdated: (updated: UserProfile) => void
}) {
  const t = useTranslations('Perfil')
  const current = profile.recoveryEmail
  const [editing, setEditing] = useState(!current)
  const [value,   setValue]   = useState(current ?? '')
  const [saving,  setSaving]  = useState(false)
  const [error,   setError]   = useState<string | null>(null)
  const [notice,  setNotice]  = useState<string | null>(null)

  async function save(next: string | null, successNotice: string | null = null) {
    setSaving(true)
    setError(null)
    setNotice(null)
    try {
      const updated = await updateRecoveryEmail(profile.id, next)
      onUpdated(updated)
      setValue(updated.recoveryEmail ?? '')
      setEditing(!updated.recoveryEmail)
      setNotice(successNotice)
    } catch (err) {
      const apiErr = err as ApiError
      setError(apiErr.status === 422 && apiErr.detail ? apiErr.detail : t('recuperacionError'))
    } finally {
      setSaving(false)
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = value.trim().toLowerCase()
    if (!EMAIL_REGEX.test(trimmed)) {
      setError(t('recuperacionErrorInvalido'))
      return
    }
    if (trimmed === profile.email.toLowerCase()) {
      setError(t('recuperacionErrorIgual'))
      return
    }
    void save(trimmed)
  }

  function handleCancel() {
    setValue(current ?? '')
    setEditing(false)
    setError(null)
  }

  const secondaryButton = 'px-4 py-2.5 text-xs font-bold uppercase tracking-widest rounded-[0.25rem] border transition-opacity hover:opacity-70 disabled:opacity-50 disabled:cursor-not-allowed'
  const secondaryStyle = { borderColor: 'var(--color-outline)', color: 'var(--color-on-surface)' }

  return (
    <section
      aria-labelledby="recovery-email-title"
      className="rounded-[0.25rem] border overflow-hidden"
      style={{ backgroundColor: 'var(--color-surface-card)', borderColor: 'var(--color-outline-variant)' }}>

      <div className="px-6 py-5 border-b" style={{ borderColor: 'var(--color-outline-variant)' }}>
        <h2 id="recovery-email-title" className="font-bold text-base" style={{ color: 'var(--color-on-surface)', letterSpacing: '-0.01em' }}>
          {t('recuperacionTitulo')}
        </h2>
        <p className="text-sm mt-0.5" style={{ color: 'var(--color-on-surface-variant)' }}>
          {t('recuperacionDescripcion')}
        </p>
      </div>

      <div className="px-6 py-6 space-y-4" aria-live="polite">
        {notice && (
          <div className="px-4 py-3 rounded-[0.25rem] text-sm font-medium" style={{ backgroundColor: '#dcfce7', color: '#15803d' }}>
            {notice}
          </div>
        )}
        {error && (
          <div role="alert" className="px-4 py-3 rounded-[0.25rem] text-sm" style={{ backgroundColor: '#fef2f2', color: 'var(--color-error)' }}>
            {error}
          </div>
        )}

        {editing ? (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div>
              <label htmlFor="recovery-email" className="block text-[11px] font-bold uppercase tracking-widest mb-2"
                style={{ color: 'var(--color-on-surface)' }}>
                {t('recuperacionCampo')}
              </label>
              <input
                id="recovery-email"
                type="email"
                autoComplete="email"
                maxLength={255}
                value={value}
                onChange={e => { setValue(e.target.value); if (error) setError(null) }}
                placeholder={t('recuperacionPlaceholder')}
                className="w-full px-4 py-3 text-base rounded-[0.25rem] outline-none focus:ring-2 focus:ring-[#d32f2f] focus:border-transparent"
                style={{ backgroundColor: 'var(--color-surface)', color: 'var(--color-on-surface)', border: '1px solid var(--color-outline)' }}
              />
            </div>
            <div className="flex flex-wrap gap-3">
              <button type="submit" disabled={saving || !value.trim()}
                className="px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem] disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ backgroundColor: 'var(--color-primary-cta)' }}>
                {saving ? t('recuperacionGuardando') : t('recuperacionGuardar')}
              </button>
              {current && (
                <button type="button" onClick={handleCancel} disabled={saving} className={secondaryButton} style={secondaryStyle}>
                  {t('recuperacionCancelar')}
                </button>
              )}
            </div>
          </form>
        ) : current && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-base font-semibold break-all" style={{ color: 'var(--color-on-surface)' }}>{current}</span>
              {profile.recoveryEmailVerified ? (
                <span className="px-2 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-widest" style={{ backgroundColor: '#dcfce7', color: '#15803d' }}>
                  {t('recuperacionVerificado')}
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-widest" style={{ backgroundColor: '#fef3c7', color: '#92400e' }}>
                  {t('recuperacionPendiente')}
                </span>
              )}
            </div>

            {!profile.recoveryEmailVerified && (
              <p className="text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>
                {t('recuperacionPendienteTexto', { email: current })}
              </p>
            )}

            <div className="flex flex-wrap gap-3">
              {!profile.recoveryEmailVerified && (
                <button type="button" disabled={saving} className={secondaryButton} style={secondaryStyle}
                  onClick={() => void save(current, t('recuperacionReenviado'))}>
                  {t('recuperacionReenviar')}
                </button>
              )}
              <button type="button" disabled={saving} className={secondaryButton} style={secondaryStyle}
                onClick={() => { setEditing(true); setNotice(null) }}>
                {t('recuperacionCambiar')}
              </button>
              <button type="button" disabled={saving} className={secondaryButton}
                style={{ borderColor: 'var(--color-error)', color: 'var(--color-error)' }}
                onClick={() => void save(null, t('recuperacionQuitado'))}>
                {t('recuperacionQuitar')}
              </button>
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
