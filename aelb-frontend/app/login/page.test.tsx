import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import LoginPage from './page'
import { loginUser, resendVerificationByEmail } from '../lib/api'
import type { ApiError } from '../lib/api'

// This file only asserts the forgot-password entry point required by the
// forgot-password feature; it does not re-test login's own pre-existing
// behavior (credentials submit, remember-device, etc.), which is out of scope.

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useSearchParams: () => ({ get: () => null }),
}))

vi.mock('../lib/api', () => ({
  loginUser: vi.fn(),
  resendVerificationByEmail: vi.fn(),
}))

const mockLoginUser = vi.mocked(loginUser)
const mockResendVerificationByEmail = vi.mocked(resendVerificationByEmail)

function apiError(status: number, detail: string, code?: string): ApiError {
  return { status, detail, code }
}

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

// Cuenta sin email verificado: LoginUseCase bloquea el acceso (ver backend) y el frontend
// debe ofrecer una salida (reenviar verificación) en vez de un error sin acción posible.
describe('LoginPage — cuenta sin email verificado', () => {
  beforeEach(() => {
    mockLoginUser.mockReset()
    mockResendVerificationByEmail.mockReset()
  })

  async function submitLogin(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText(/^email$/i), 'sin-verificar@example.com')
    await user.type(screen.getByLabelText(/^contraseña$/i), 'una-contraseña-cualquiera')
    await user.click(screen.getByRole('button', { name: /iniciar sesión en aelb/i }))
  }

  it('muestra el botón de reenvío solo cuando el error es EMAIL_NOT_VERIFIED', async () => {
    mockLoginUser.mockRejectedValueOnce(apiError(403, 'Verifica tu email antes de iniciar sesión.', 'EMAIL_NOT_VERIFIED'))
    const user = userEvent.setup()
    render(<LoginPage />)

    await submitLogin(user)

    expect(await screen.findByRole('button', { name: /reenviar email de verificación/i })).toBeInTheDocument()
  })

  it('no muestra el botón de reenvío para otros errores (p. ej. credenciales incorrectas)', async () => {
    mockLoginUser.mockRejectedValueOnce(apiError(401, 'Credenciales incorrectas'))
    const user = userEvent.setup()
    render(<LoginPage />)

    await submitLogin(user)

    await screen.findByText(/credenciales incorrectas/i)
    expect(screen.queryByRole('button', { name: /reenviar email de verificación/i })).not.toBeInTheDocument()
  })

  it('al pulsar reenviar, llama a resendVerificationByEmail con el email introducido y confirma el envío', async () => {
    mockLoginUser.mockRejectedValueOnce(apiError(403, 'Verifica tu email antes de iniciar sesión.', 'EMAIL_NOT_VERIFIED'))
    mockResendVerificationByEmail.mockResolvedValueOnce({ message: 'ok' })
    const user = userEvent.setup()
    render(<LoginPage />)

    await submitLogin(user)
    await user.click(await screen.findByRole('button', { name: /reenviar email de verificación/i }))

    await waitFor(() => expect(mockResendVerificationByEmail).toHaveBeenCalledWith('sin-verificar@example.com'))
    expect(await screen.findByText(/recibirás un email en breve/i)).toBeInTheDocument()
  })
})
