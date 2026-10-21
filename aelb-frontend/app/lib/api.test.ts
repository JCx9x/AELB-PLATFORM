import { afterEach, describe, expect, it, vi } from 'vitest'

import { normalizeBaseUrl } from './api'

describe('normalizeBaseUrl', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it.each([
    ['http://localhost:8080', 'http://localhost:8080'],
    ['http://localhost:8080/', 'http://localhost:8080'],
    ['http://localhost:8080/api', 'http://localhost:8080'],
    ['http://localhost:8080/api/', 'http://localhost:8080'],
    ['/api', ''],
    ['/api/', ''],
    ['https://example.com/myapi/', 'https://example.com/myapi'],
  ])('normalizes %s to %s', (input, expected) => {
    expect(normalizeBaseUrl(input)).toBe(expected)
  })
})

async function importApiWithBaseUrl(baseUrl: string) {
  vi.resetModules()
  vi.stubEnv('NEXT_PUBLIC_API_URL', baseUrl)
  return import('./api')
}

function successfulResponse(init: ResponseInit = {}, body = ''): Response {
  return new Response(body, { status: 200, ...init })
}

describe('API URL construction', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllEnvs()
    document.cookie = 'XSRF-TOKEN=; Max-Age=0'
  })

  it.each([
    ['http://localhost:8080', 'http://localhost:8080/api/users'],
    ['http://localhost:8080/', 'http://localhost:8080/api/users'],
    ['http://localhost:8080/api', 'http://localhost:8080/api/users'],
    ['http://localhost:8080/api/', 'http://localhost:8080/api/users'],
    ['/api', '/api/users'],
    ['/api/', '/api/users'],
  ])('generates one /api for registerUser with %s', async (baseUrl, expectedUrl) => {
    document.cookie = 'XSRF-TOKEN=test-token'
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      successfulResponse({ status: 201, headers: { Location: '/api/users/user-1' } })
    )
    const { registerUser } = await importApiWithBaseUrl(baseUrl)

    await registerUser({
      email: 'user@example.test', password: 'password', firstName: 'Test',
      lastName: 'User', dni: '12345678Z', phone: '600000000', city: 'Madrid',
      province: null, nationality: 'ES', birthDate: '1990-01-01',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      expectedUrl,
      expect.objectContaining({ method: 'POST' }),
    )
    expect(fetchMock.mock.calls[0][0].toString()).not.toContain('/api/api/')
  })

  it('uses /api/auth/csrf when the CSRF cookie is absent', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(successfulResponse())
    const { registerUser } = await importApiWithBaseUrl('/api')

    await expect(registerUser({} as never)).rejects.toMatchObject({
      detail: 'No se pudo inicializar la protección CSRF',
    })

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/csrf', expect.objectContaining({
      credentials: 'include',
    }))
  })

  it('uses /api/auth/login for login', async () => {
    document.cookie = 'XSRF-TOKEN=test-token'
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      successfulResponse({}, JSON.stringify({
        userId: 'user-1', email: 'user@example.test', role: 'ATHLETE',
        firstName: 'Test', lastName: 'User',
      }))
    )
    const { loginUser } = await importApiWithBaseUrl('/api/')

    await loginUser('user@example.test', 'password')

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock.mock.calls[0][0].toString()).not.toContain('/api/api/')
  })
})
