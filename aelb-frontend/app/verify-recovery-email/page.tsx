'use client'

import { Suspense, useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import Navbar from '../components/Navbar'
import { verifyRecoveryEmail } from '../lib/api'

function VerifyRecoveryContent() {
  const t = useTranslations('VerifyRecoveryEmail')
  const token = useSearchParams().get('token') ?? ''
  const [state, setState] = useState<'loading' | 'ok' | 'error'>(token ? 'loading' : 'error')

  useEffect(() => {
    if (!token) return
    verifyRecoveryEmail(token).then(() => setState('ok')).catch(() => setState('error'))
  }, [token])

  return (
    <div className="max-w-md mx-auto px-4 py-16 text-center" aria-live="polite">
      <p className="text-[11px] font-bold uppercase tracking-widest mb-3" style={{ color: 'var(--color-primary-cta)' }}>
        {t('titulo')}
      </p>

      {state === 'loading' && (
        <>
          <div className="w-8 h-8 mx-auto mb-4 border-4 border-[#d32f2f] border-t-transparent rounded-full animate-spin" />
          <p className="text-sm" style={{ color: 'var(--color-on-surface-variant)' }}>{t('verificando')}</p>
        </>
      )}

      {state === 'ok' && (
        <>
          <h1 className="font-extrabold text-2xl mb-3" style={{ color: 'var(--color-on-surface)' }}>{t('okTitulo')}</h1>
          <p className="text-sm mb-8" style={{ color: 'var(--color-on-surface-variant)' }}>{t('okTexto')}</p>
          <Link href="/perfil" className="inline-block px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem]"
            style={{ backgroundColor: 'var(--color-primary-cta)' }}>
            {t('irPerfil')}
          </Link>
        </>
      )}

      {state === 'error' && (
        <>
          <h1 className="font-extrabold text-2xl mb-3" style={{ color: 'var(--color-on-surface)' }}>{t('errorTitulo')}</h1>
          <p className="text-sm mb-8" style={{ color: 'var(--color-on-surface-variant)' }}>{t('errorTexto')}</p>
          <div className="flex flex-wrap justify-center gap-3">
            <Link href="/perfil" className="inline-block px-6 py-3 text-sm font-bold uppercase tracking-widest text-white rounded-[0.25rem]"
              style={{ backgroundColor: 'var(--color-primary-cta)' }}>
              {t('irPerfil')}
            </Link>
            <Link href="/" className="inline-block px-6 py-3 text-sm font-bold uppercase tracking-widest rounded-[0.25rem] border"
              style={{ borderColor: 'var(--color-outline)', color: 'var(--color-on-surface)' }}>
              {t('irInicio')}
            </Link>
          </div>
        </>
      )}
    </div>
  )
}

export default function VerifyRecoveryEmailPage() {
  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />
      <Suspense fallback={null}>
        <VerifyRecoveryContent />
      </Suspense>
    </div>
  )
}
