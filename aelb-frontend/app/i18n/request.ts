import { cookies, headers } from 'next/headers'
import { getRequestConfig } from 'next-intl/server'
import { defaultLocale, isLocale, localeCookieName } from './config'

export default getRequestConfig(async () => {
  const cookieStore = await cookies()
  const cookieLocale = cookieStore.get(localeCookieName)?.value

  let locale = isLocale(cookieLocale) ? cookieLocale : undefined

  // Sin cookie todavía (primera visita): negocia con el Accept-Language del navegador.
  if (!locale) {
    const acceptLanguage = (await headers()).get('accept-language') ?? ''
    if (acceptLanguage.toLowerCase().startsWith('en')) locale = 'en'
  }

  locale ??= defaultLocale

  return {
    locale,
    messages: (await import(`../../messages/${locale}.json`)).default,
  }
})
