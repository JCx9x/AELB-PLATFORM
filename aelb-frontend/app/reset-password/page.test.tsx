import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ResetPasswordPage from './page'
import { resetPassword } from '../lib/api'
import type { ApiError } from '../lib/api'

let mockToken: string | null = 'valid-token-123'

vi.mock('next/navigation', () => ({
  useSearchParams: () => ({ get: (key: string) => (key === 'token' ? mockToken : null) }),
}))

vi.mock('../lib/api', () => ({
  resetPassword: vi.fn(),
}))

const mockResetPassword = vi.mocked(resetPassword)

function apiError(status: number, detail: string, correlationId?: string): ApiError {
  return { status, detail, correlationId }
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    mockResetPassword.mockReset()
    mockToken = 'valid-token-123'
  })

  it('renders both password fields and the submit button when a token is present', () => {
    render(<ResetPasswordPage />)

    expect(screen.getByLabelText(/nueva contraseña/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/confirmar contraseña/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /guardar nueva contraseña/i })).toBeInTheDocument()
  })

  it('shows a dedicated error and no form when the token is missing from the URL', () => {
    mockToken = null
    render(<ResetPasswordPage />)

    expect(screen.getByText(/este enlace no es válido/i)).toBeInTheDocument()
    expect(screen.queryByLabelText(/nueva contraseña/i)).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /solicitar un enlace nuevo/i })).toHaveAttribute('href', '/forgot-password')
  })

  it('blocks submission when the password is shorter than 8 characters', async () => {
    const user = userEvent.setup()
    render(<ResetPasswordPage />)

    await user.type(screen.getByLabelText(/nueva contraseña/i), 'short')
    await user.type(screen.getByLabelText(/confirmar contraseña/i), 'short')
    await user.click(screen.getByRole('button', { name: /guardar nueva contraseña/i }))

    expect(mockResetPassword).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent(/al menos 8 caracteres/i)
  })

  it('blocks submission when passwords do not match', async () => {
    const user = userEvent.setup()
    render(<ResetPasswordPage />)

    await user.type(screen.getByLabelText(/nueva contraseña/i), 'password123')
    await user.type(screen.getByLabelText(/confirmar contraseña/i), 'password124')
    await user.click(screen.getByRole('button', { name: /guardar nueva contraseña/i }))

    expect(mockResetPassword).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent(/no coinciden/i)
  })

  it('shows a success screen and a link to login after a valid reset', async () => {
    mockResetPassword.mockResolvedValueOnce(undefined)
    const user = userEvent.setup()
    render(<ResetPasswordPage />)

    await user.type(screen.getByLabelText(/nueva contraseña/i), 'newpassword123')
    await user.type(screen.getByLabelText(/confirmar contraseña/i), 'newpassword123')
    await user.click(screen.getByRole('button', { name: /guardar nueva contraseña/i }))

    expect(await screen.findByText(/contraseña actualizada/i)).toBeInTheDocument()
    expect(mockResetPassword).toHaveBeenCalledWith('valid-token-123', 'newpassword123')
    expect(screen.getByRole('link', { name: /iniciar sesión/i })).toHaveAttribute('href', '/login')
  })

  it('shows the backend message for an invalid/expired token without redirecting to success', async () => {
    mockResetPassword.mockRejectedValueOnce(
      apiError(422, 'El enlace de recuperación no es válido o ha expirado')
    )
    const user = userEvent.setup()
    render(<ResetPasswordPage />)

    await user.type(screen.getByLabelText(/nueva contraseña/i), 'newpassword123')
    await user.type(screen.getByLabelText(/confirmar contraseña/i), 'newpassword123')
    await user.click(screen.getByRole('button', { name: /guardar nueva contraseña/i }))

    expect(await screen.findByText(/no es válido o ha expirado/i)).toBeInTheDocument()
    expect(screen.queryByText(/contraseña actualizada/i)).not.toBeInTheDocument()
  })

  it('disables the submit button while the request is pending', async () => {
    let resolveRequest!: () => void
    mockResetPassword.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve as () => void }))
    const user = userEvent.setup()
    render(<ResetPasswordPage />)

    await user.type(screen.getByLabelText(/nueva contraseña/i), 'newpassword123')
    await user.type(screen.getByLabelText(/confirmar contraseña/i), 'newpassword123')
    await user.click(screen.getByRole('button', { name: /guardar nueva contraseña/i }))

    expect(screen.getByRole('button', { name: /guardando/i })).toBeDisabled()

    resolveRequest()
    await waitFor(() => expect(screen.getByText(/contraseña actualizada/i)).toBeInTheDocument())
  })

  it('shows a generic message on network/server errors', async () => {
    mockResetPassword.mockRejectedValueOnce(apiError(500, 'Ha ocurrido un error inesperado.'))
    const user = userEvent.setup()
    render(<ResetPasswordPage />)

    await user.type(screen.getByLabelText(/nueva contraseña/i), 'newpassword123')
    await user.type(screen.getByLabelText(/confirmar contraseña/i), 'newpassword123')
    await user.click(screen.getByRole('button', { name: /guardar nueva contraseña/i }))

    expect(await screen.findByText(/no se ha podido procesar la solicitud/i)).toBeInTheDocument()
  })
})
