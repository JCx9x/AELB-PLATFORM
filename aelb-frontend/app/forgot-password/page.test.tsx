import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ForgotPasswordPage from './page'
import { forgotPassword } from '../lib/api'
import type { ApiError } from '../lib/api'

vi.mock('../lib/api', () => ({
  forgotPassword: vi.fn(),
}))

const mockForgotPassword = vi.mocked(forgotPassword)

function apiError(status: number, detail: string, correlationId?: string): ApiError {
  return { status, detail, correlationId }
}

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    mockForgotPassword.mockReset()
  })

  // ── Render ──────────────────────────────────────────────────────────────
  it('renders the email input, submit button and back-to-login link', () => {
    render(<ForgotPasswordPage />)

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toHaveAttribute('type', 'email')
    expect(screen.getByLabelText(/email/i)).toHaveAttribute('autocomplete', 'email')
    expect(screen.getByRole('button', { name: /enviar enlace/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /volver a iniciar sesión/i })).toHaveAttribute('href', '/login')
  })

  // ── Validation ──────────────────────────────────────────────────────────
  it('does not call the API when the email is empty', async () => {
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.click(screen.getByRole('button', { name: /enviar enlace/i }))

    expect(mockForgotPassword).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent(/introduce tu email/i)
  })

  it('shows a validation message for an invalid email format without calling the API', async () => {
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.type(screen.getByLabelText(/email/i), 'not-an-email')
    await user.click(screen.getByRole('button', { name: /enviar enlace/i }))

    expect(mockForgotPassword).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent(/email válido/i)
  })

  // ── Success (existing user) ────────────────────────────────────────────
  it('shows the generic confirmation screen after a successful submission', async () => {
    mockForgotPassword.mockResolvedValueOnce({ message: 'ok' })
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.type(screen.getByLabelText(/email/i), 'existing.user@example.com')
    await user.click(screen.getByRole('button', { name: /enviar enlace/i }))

    expect(await screen.findByText(/revisa tu correo/i)).toBeInTheDocument()
    expect(mockForgotPassword).toHaveBeenCalledWith('existing.user@example.com')
    // Masked, not raw — the local part beyond the first character must not leak.
    expect(screen.getByText(/e\*\*\*@example\.com/)).toBeInTheDocument()
  })

  // ── Non-existent user: identical outcome (no enumeration) ─────────────
  it('shows the exact same confirmation screen for a non-existent account', async () => {
    // The backend responds identically for existing and non-existing accounts
    // (see RequestPasswordResetUseCase / EmailServiceAdapter#requestPasswordReset),
    // so from the frontend's point of view this is indistinguishable from the
    // "existing user" case above — same mock, same assertions.
    mockForgotPassword.mockResolvedValueOnce({ message: 'ok' })
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.type(screen.getByLabelText(/email/i), 'nobody@example.com')
    await user.click(screen.getByRole('button', { name: /enviar enlace/i }))

    expect(await screen.findByText(/revisa tu correo/i)).toBeInTheDocument()
    expect(screen.queryByText(/no existe/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/usuario no existe/i)).not.toBeInTheDocument()
  })

  // ── Loading ─────────────────────────────────────────────────────────────
  it('disables the submit button and shows a loading label while the request is pending', async () => {
    let resolveRequest!: (value: { message: string }) => void
    mockForgotPassword.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve }))
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.type(screen.getByLabelText(/email/i), 'user@example.com')
    await user.click(screen.getByRole('button', { name: /enviar enlace/i }))

    const pendingButton = screen.getByRole('button', { name: /enviando/i })
    expect(pendingButton).toBeDisabled()

    resolveRequest({ message: 'ok' })
    await waitFor(() => expect(screen.getByText(/revisa tu correo/i)).toBeInTheDocument())
  })

  // ── Double click ────────────────────────────────────────────────────────
  it('never fires more than one request no matter how many times the button is clicked while pending', async () => {
    let resolveRequest!: (value: { message: string }) => void
    mockForgotPassword.mockReturnValueOnce(new Promise((resolve) => { resolveRequest = resolve }))
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.type(screen.getByLabelText(/email/i), 'user@example.com')
    const button = screen.getByRole('button', { name: /enviar enlace/i })

    fireEvent.click(button)
    fireEvent.click(button)
    fireEvent.click(button)

    expect(mockForgotPassword).toHaveBeenCalledTimes(1)

    resolveRequest({ message: 'ok' })
    await waitFor(() => expect(screen.getByText(/revisa tu correo/i)).toBeInTheDocument())
  })

  // ── Network / 500 error ─────────────────────────────────────────────────
  it('shows a generic message without technical details on a network/server error', async () => {
    mockForgotPassword.mockRejectedValueOnce(apiError(500, 'Ha ocurrido un error inesperado.', '01J000CORRELATION'))
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.type(screen.getByLabelText(/email/i), 'user@example.com')
    await user.click(screen.getByRole('button', { name: /enviar enlace/i }))

    const alert = await screen.findByText(/no se ha podido procesar la solicitud/i)
    expect(alert).toBeInTheDocument()
    expect(screen.queryByText(/AxiosError|ECONNREFUSED|Internal Server Error|stack/i)).not.toBeInTheDocument()
    // The stays on the form — a real failure must never look like success.
    expect(screen.queryByText(/revisa tu correo/i)).not.toBeInTheDocument()
    expect(screen.getByText(/01J000CORRELATION/)).toBeInTheDocument()
  })

  // ── Rate limit ──────────────────────────────────────────────────────────
  it('shows a wait message on 429 without exposing technical details', async () => {
    mockForgotPassword.mockRejectedValueOnce(apiError(429, 'Demasiadas solicitudes. Inténtalo de nuevo en unos minutos.'))
    const user = userEvent.setup()
    render(<ForgotPasswordPage />)

    await user.type(screen.getByLabelText(/email/i), 'user@example.com')
    await user.click(screen.getByRole('button', { name: /enviar enlace/i }))

    expect(await screen.findByText(/demasiadas solicitudes/i)).toBeInTheDocument()
    expect(screen.queryByText(/revisa tu correo/i)).not.toBeInTheDocument()
  })
})
