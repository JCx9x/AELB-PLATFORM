// Configuración compartida de idiomas — sin prefijo de idioma en la URL.
// El locale activo se guarda en una cookie y se lee tanto en el servidor
// (app/i18n/request.ts) como en el cliente (LanguageSwitcher).

export const locales = ['es', 'en'] as const
export type Locale = (typeof locales)[number]

export const defaultLocale: Locale = 'es'

export const localeCookieName = 'aelb_locale'

export const localeLabels: Record<Locale, string> = {
  es: 'Español',
  en: 'English',
}

export function isLocale(value: string | undefined | null): value is Locale {
  return !!value && (locales as readonly string[]).includes(value)
}
