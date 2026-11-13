import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import RecoveryEmailCard from './RecoveryEmailCard'
import { updateRecoveryEmail, type UserProfile } from '../lib/api'

vi.mock('../lib/api', () => ({ updateRecoveryEmail: vi.fn() }))

const mockUpdate = vi.mocked(updateRecoveryEmail)

function profile(overrides: Partial<UserProfile> = {}): UserProfile {
  return {
    id: 'u1', email: 'ana@example.com', firstName: 'Ana', lastName: 'García', dni: '12345678Z',
    phone: null, city: null, province: null, nationality: null, birthDate: '1995-05-20',
    role: 'USER', blocked: false, emailVerified: true, recoveryEmail: null, recoveryEmailVerified: false,
    teamId: null, createdAt: '2026-01-01T00:00:00', ...overrides,
  }
}

describe('RecoveryEmailCard', () => {
  beforeEach(() => mockUpdate.mockReset())

  it('saves a new recovery email and shows it as pending verification', async () => {
    const updated = profile({ recoveryEmail: 'backup@example.com' })
    mockUpdate.mockResolvedValueOnce(updated)
    const onUpdated = vi.fn()
    const user = userEvent.setup()
    const { rerender } = render(<RecoveryEmailCard profile={profile()} onUpdated={onUpdated} />)

    await user.type(screen.getByRole('textbox', { name: /correo de recuperación/i }), 'Backup@Example.com')
    await user.click(screen.getByRole('button', { name: /guardar/i }))

    expect(mockUpdate).toHaveBeenCalledWith('u1', 'backup@example.com')
    expect(onUpdated).toHaveBeenCalledWith(updated)
    rerender(<RecoveryEmailCard profile={updated} onUpdated={onUpdated} />)
    expect(screen.getByText(/pendiente de verificar/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /reenviar correo de verificación/i })).toBeInTheDocument()
  })

  it('rejects the primary email without calling the API', async () => {
    const user = userEvent.setup()
    render(<RecoveryEmailCard profile={profile()} onUpdated={vi.fn()} />)

    await user.type(screen.getByRole('textbox', { name: /correo de recuperación/i }), 'ANA@example.com')
    await user.click(screen.getByRole('button', { name: /guardar/i }))

    expect(mockUpdate).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent(/distinto de tu correo principal/i)
  })

  it('removes a verified recovery email', async () => {
    mockUpdate.mockResolvedValueOnce(profile())
    const user = userEvent.setup()
    render(<RecoveryEmailCard profile={profile({ recoveryEmail: 'backup@example.com', recoveryEmailVerified: true })} onUpdated={vi.fn()} />)

    expect(screen.getByText(/^verificado$/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /quitar/i }))

    expect(mockUpdate).toHaveBeenCalledWith('u1', null)
    expect(await screen.findByText(/correo de recuperación eliminado/i)).toBeInTheDocument()
  })
})
