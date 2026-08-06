import type { AuthResponse } from './api'

const SESSION_KEY = 'aelb_session'
const LEGACY_TOKEN_KEY = 'aelb_token'

export interface Session {
  userId:    string
  email:     string
  role:      string
  firstName: string
  lastName:  string
}

// ── Write ─────────────────────────────────────────────────────────────────────

export function saveSession(auth: AuthResponse): void {
  const session: Session = {
    userId:    auth.userId,
    email:     auth.email,
    role:      auth.role,
    firstName: auth.firstName,
    lastName:  auth.lastName,
  }
  // Remove any token stored by versions prior to the HttpOnly-cookie migration.
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function updateSession(updates: Partial<Session>): void {
  const session = getSession()
  if (!session) return
  localStorage.setItem(SESSION_KEY, JSON.stringify({ ...session, ...updates }))
}

// ── Read ──────────────────────────────────────────────────────────────────────

export function getSession(): Session | null {
  if (typeof window === 'undefined') return null
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function isAuthenticated(): boolean {
  // This only controls presentation. The backend authorizes every request
  // with the HttpOnly cookie and never trusts this browser-side state.
  return Boolean(getSession())
}

// ── Delete ────────────────────────────────────────────────────────────────────

export function clearSession(): void {
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  localStorage.removeItem(SESSION_KEY)
}
