'use client'

import { useTranslations } from 'next-intl'
import { checkPassword, PASSWORD_MIN_LENGTH, type PasswordRule } from '../lib/passwordPolicy'

const ORDER: PasswordRule[] = ['minimo', 'minuscula', 'mayuscula', 'numero']

/** Lista de requisitos de la contraseña que se marcan en verde según se cumplen. */
export default function PasswordRequirements({ password, id }: { password: string; id?: string }) {
  const t = useTranslations('PasswordPolicy')
  const result = checkPassword(password)

  return (
    <ul id={id} className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1" aria-label={t('titulo')}>
      {ORDER.map((rule) => {
        const ok = result[rule]
        return (
          <li key={rule} className="flex items-center gap-1.5 text-[11px]"
            style={{ color: ok ? '#15803d' : 'var(--color-outline)' }}>
            {ok ? (
              <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3} aria-hidden>
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            ) : (
              <span className="w-3.5 h-3.5 flex-shrink-0 flex items-center justify-center" aria-hidden>
                <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: 'currentColor' }} />
              </span>
            )}
            <span>{t(rule, { min: PASSWORD_MIN_LENGTH })}</span>
            <span className="sr-only">{ok ? t('cumplido') : t('pendiente')}</span>
          </li>
        )
      })}
    </ul>
  )
}
