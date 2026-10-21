import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import LoginPage from './page'
import { loginUser } from '../lib/api'

// This file only asserts the forgot-password entry point required by the
// forgot-password feature; it does not re-test login's own pre-existing
// behavior (credentials submit, remember-device, etc.), which is out of scope.

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useSearchParams: () => ({ get: () => null }),
}))

vi.mock('../lib/api', () => ({
  loginUser: vi.fn(),
}))

describe('LoginPage navigation', () => {
  it('renders a visible "¿Olvidaste tu contraseña?" link pointing to /forgot-password', () => {
    render(<LoginPage />)

    const link = screen.getByRole('link', { name: /¿olvidaste tu contraseña\?/i })
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute('href', '/forgot-password')
  })

  it('does not call loginUser just by rendering the page', () => {
    render(<LoginPage />)
    expect(vi.mocked(loginUser)).not.toHaveBeenCalled()
  })
})
