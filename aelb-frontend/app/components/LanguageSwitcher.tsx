'use client'

import { useTransition } from 'react'
import { useRouter } from 'next/navigation'
import { useLocale, useTranslations } from 'next-intl'
import { locales, type Locale } from '../i18n/config'

// Selector de idioma ES/EN — no cambia la URL, solo la cookie de locale.
// Tras guardarla, refresca la ruta actual para que el servidor re-renderice
// con los mensajes del nuevo idioma.
export default function LanguageSwitcher({ variant = 'dark' }: { variant?: 'dark' | 'light' }) {
  const t = useTranslations('Navbar')
  const locale = useLocale() as Locale
  const router = useRouter()
  const [isPending, startTransition] = useTransition()

  async function changeLocale(next: Locale) {
    if (next === locale || isPending) return
    await fetch('/api/set-locale', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ locale: next }),
    })
    startTransition(() => router.refresh())
  }

  const activeColor   = variant === 'dark' ? '#fff' : 'var(--color-on-surface)'
  const inactiveColor = variant === 'dark' ? 'rgba(255,255,255,0.4)' : 'var(--color-outline)'

  return (
    <div className="flex items-center gap-0.5" aria-label={t('selectorIdioma')}>
      {locales.map((l, i) => (
        <span key={l} className="flex items-center">
          {i > 0 && <span className="text-[10px] mx-1" style={{ color: inactiveColor }}>/</span>}
          <button
            type="button"
            onClick={() => changeLocale(l)}
            disabled={isPending}
            aria-current={l === locale}
            className="text-[11px] font-bold uppercase tracking-widest transition-opacity hover:opacity-100 disabled:cursor-wait"
            style={{
              color: l === locale ? activeColor : inactiveColor,
              opacity: l === locale ? 1 : 0.8,
            }}>
            {l}
          </button>
        </span>
      ))}
    </div>
  )
}
