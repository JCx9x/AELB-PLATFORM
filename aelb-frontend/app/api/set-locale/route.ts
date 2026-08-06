import { cookies } from 'next/headers'
import { NextRequest, NextResponse } from 'next/server'
import { isLocale, localeCookieName } from '../../i18n/config'

export async function POST(request: NextRequest) {
  const { locale } = await request.json()

  if (!isLocale(locale)) {
    return NextResponse.json({ error: 'Locale no soportado' }, { status: 400 })
  }

  const cookieStore = await cookies()
  cookieStore.set(localeCookieName, locale, {
    path: '/',
    maxAge: 60 * 60 * 24 * 365, // 1 año
    sameSite: 'lax',
  })

  return NextResponse.json({ success: true })
}
