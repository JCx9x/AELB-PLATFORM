import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import RegisterPage from './page'
import { registerUser, resendVerificationByEmail } from '../lib/api'

vi.mock('../lib/api', () => ({
  registerUser: vi.fn(),
  resendVerificationByEmail: vi.fn(),
}))

const mockRegister = vi.mocked(registerUser)
const mockResend = vi.mocked(resendVerificationByEmail)

async function fillForm(password: string, confirm = password) {
  const user = userEvent.setup()
  render(<RegisterPage />)
  await user.type(screen.getByLabelText(/^nombre$/i), 'Ana')
  await user.type(screen.getByLabelText(/apellidos/i), 'García')
  await user.type(screen.getByLabelText(/^dni$/i), '12345678Z')
  await user.type(screen.getByLabelText(/^email$/i), 'ana@example.com')
  await user.type(screen.getByLabelText(/teléfono/i), '600000000')
  await user.type(screen.getByLabelText(/ciudad/i), 'Madrid')
  await user.type(screen.getByLabelText(/fecha de nacimiento/i), '1995-05-20')
  await user.type(screen.getByLabelText(/^contraseña$/i), password)
  await user.type(screen.getByLabelText(/confirmar contraseña/i), confirm)
  await user.click(screen.getByRole('checkbox'))
  await user.click(screen.getByRole('button', { name: /crear cuenta federativa/i }))
  return user
}

describe('RegisterPage', () => {
  beforeEach(() => {
    mockRegister.mockReset()
    mockResend.mockReset()
    window.scrollTo = vi.fn()
  })

  it('rejects a weak password like 12345678 without calling the API', async () => {
    await fillForm('12345678')

    expect(mockRegister).not.toHaveBeenCalled()
    expect(screen.getByText(/no cumple los requisitos de seguridad/i)).toBeInTheDocument()
  })

  it('shows the pending-verification screen instead of the form after creating the account', async () => {
    mockRegister.mockResolvedValueOnce(undefined as never)
    await fillForm('Lucha2026')

    expect(mockRegister).toHaveBeenCalledOnce()
    expect(screen.getByRole('heading', { name: /revisa tu correo electrónico/i })).toBeInTheDocument()
    expect(screen.getByText('ana@example.com')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /crear cuenta federativa/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /ir a iniciar sesión/i })).toHaveAttribute('href', '/login')
  })

  it('lets the user resend the verification email from the pending screen', async () => {
    mockRegister.mockResolvedValueOnce(undefined as never)
    mockResend.mockResolvedValueOnce({ message: 'ok' })
    const user = await fillForm('Lucha2026')

    await user.click(screen.getByRole('button', { name: /reenviar correo/i }))

    expect(mockResend).toHaveBeenCalledWith('ana@example.com')
    expect(await screen.findByText(/nuevo correo de verificación/i)).toBeInTheDocument()
  })
})
