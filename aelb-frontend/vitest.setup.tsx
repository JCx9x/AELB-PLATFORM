import '@testing-library/jest-dom/vitest'
import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import type { ReactNode } from 'react'
import es from './messages/es.json'

// vitest isn't configured with `test.globals`, so Testing Library's automatic
// afterEach cleanup (which hooks into a global afterEach) never registers —
// without this, DOM from one test leaks into the next and getBy* queries
// start matching duplicate elements across tests in the same file.
afterEach(() => cleanup())

// Real Spanish copy from the app's own message catalog, so assertions check
// actual UX text instead of translation keys.
type Messages = Record<string, Record<string, unknown>>

function interpolate(template: string, params?: Record<string, unknown>): string {
  if (!params) return template
  return Object.entries(params).reduce(
    (acc, [key, value]) => acc.replaceAll(`{${key}}`, String(value)),
    template
  )
}

vi.mock('next-intl', () => ({
  useTranslations: (namespace: string) => {
    const dict = (es as Messages)[namespace] ?? {}
    return (key: string, params?: Record<string, unknown>) => {
      const value = dict[key]
      return typeof value === 'string' ? interpolate(value, params) : key
    }
  },
  useLocale: () => 'es',
}))

// next/link needs an app-router context to prefetch; tests only care that it
// renders a real anchor with the right href.
vi.mock('next/link', () => ({
  default: ({ href, children, ...rest }: { href: string; children: ReactNode } & Record<string, unknown>) => (
    <a href={typeof href === 'string' ? href : String(href)} {...rest}>{children}</a>
  ),
}))
