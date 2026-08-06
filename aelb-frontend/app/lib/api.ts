const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

// ── Types ─────────────────────────────────────────────────────────────────────

export interface AuthResponse {
  userId:    string
  email:     string
  role:      string
  firstName: string
  lastName:  string
}

export interface ApiError {
  status: number
  title?: string
  detail: string
}

export interface RegisterData {
  email:       string
  password:    string
  firstName:   string
  lastName:    string
  dni:         string
  phone:       string
  city:        string
  province:    string | null
  nationality: string
}

export interface UserProfile {
  id:          string
  email:       string
  firstName:   string
  lastName:    string
  dni:         string
  phone:       string | null
  city:        string | null
  province:    string | null
  nationality: string | null
  birthDate:   string | null
  role:        string
  blocked:     boolean
  teamId:      string | null
  createdAt:   string
}

export interface UpdateProfileData {
  firstName: string
  lastName:  string
  phone?:    string | null
  birthDate?: string | null
}

export interface Category {
  id:          string
  name:        string
  gender:      'MALE' | 'FEMALE' | 'OPEN'
  armSide:     'RIGHT' | 'LEFT' | 'BOTH'
  weightLimit: number | null
  ageGroup:    string | null
  ageCategory: CompetitionAgeCategoryId | null
  shift:       'MORNING' | 'AFTERNOON'
}

export type CompetitionAgeCategoryId =
  | 'SUB_JUNIOR' | 'JUNIOR' | 'YOUTH' | 'AMATEUR' | 'SENIOR' | 'MASTER'
  | 'GRAND_MASTER' | 'SENIOR_GRAND_MASTER' | 'SUPER_SENIOR_GRAND_MASTER'

export interface CompetitionAgeCategory {
  id: CompetitionAgeCategoryId
  displayName: string
  denomination: string | null
  minimumAge: number | null
  maximumAge: number | null
  shift: 'MORNING' | 'AFTERNOON'
}

export interface CategoryData {
  gender:      string
  armSide:     string
  weightLimit: number | null
  ageGroup:    string | null
  ageCategory: CompetitionAgeCategoryId | null
  shift:       'MORNING' | 'AFTERNOON'
}

export interface Championship {
  id:                   string
  name:                 string
  location:             string
  eventDate:            string
  registrationDeadline: string
  price:                number
  visible:              boolean
  imageUrl:             string | null  // presigned GET URL for display
  imageKey:             string | null  // raw S3 key — use in update payloads to keep existing image
  description:          string | null
  requiresCurrentQuota: boolean
  categoryIds:          string[]
  createdBy:            string
  createdAt:            string
  updatedAt:            string
}

export interface ChampionshipAgeCategoryPrice {
  ageCategory: CompetitionAgeCategoryId
  pricePerArm: number
  combinationPrice: number
}

export async function getChampionshipCategoryPrices(championshipId: string): Promise<ChampionshipAgeCategoryPrice[]> {
  const response = await fetchWithAuth(`/api/championships/${championshipId}/category-prices`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function saveChampionshipCategoryPrices(championshipId: string, prices: ChampionshipAgeCategoryPrice[]): Promise<ChampionshipAgeCategoryPrice[]> {
  const response = await fetchWithAuth(`/api/championships/${championshipId}/category-prices`, {
    method: 'PUT', body: JSON.stringify({ prices }),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export interface ChampionshipData {
  name:                 string
  location:             string
  eventDate:            string
  registrationDeadline: string
  price:                number
  imageKey:             string | null  // S3 object key from presigned upload
  description:          string | null
  requiresCurrentQuota: boolean
  visible:              boolean
  categoryIds:          string[]
}

// ── Internal helpers ──────────────────────────────────────────────────────────

async function parseError(response: Response): Promise<ApiError> {
  try {
    const body = await response.json()
    return {
      status: response.status,
      title:  body.title,
      detail: body.detail ?? body.message ?? 'Error desconocido',
    }
  } catch {
    return { status: response.status, detail: 'Error de red' }
  }
}

// ── Public API functions ──────────────────────────────────────────────────────

// ── Storage / presigned uploads ───────────────────────────────────────────────

export async function requestUploadUrl(
  folder: string,
  contentType: string
): Promise<{ uploadUrl: string; key: string }> {
  const response = await fetchWithAuth(
    `/api/storage/presign/upload?folder=${encodeURIComponent(folder)}&contentType=${encodeURIComponent(contentType)}`,
    { method: 'POST' }
  )
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function uploadToS3(uploadUrl: string, file: File): Promise<void> {
  const response = await fetch(uploadUrl, {
    method:  'PUT',
    headers: { 'Content-Type': file.type },
    body:    file,
  })
  if (!response.ok) {
    throw { status: response.status, detail: 'Error al subir la imagen al almacenamiento' }
  }
}

// ── Auth ──────────────────────────────────────────────────────────────────────

export async function loginUser(email: string, password: string): Promise<AuthResponse> {
  const csrfHeaders = await getCsrfHeaders()
  const response = await fetch(`${BASE_URL}/api/auth/login`, {
    method:  'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...csrfHeaders },
    body:    JSON.stringify({ email, password }),
  })

  if (!response.ok) {
    throw await parseError(response)
  }

  return response.json()
}

export async function registerUser(data: RegisterData): Promise<{ id: string }> {
  const csrfHeaders = await getCsrfHeaders()
  const response = await fetch(`${BASE_URL}/api/users`, {
    method:  'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...csrfHeaders },
    body:    JSON.stringify(data),
  })

  if (!response.ok) {
    throw await parseError(response)
  }

  // 201 Created — no body, ID viene en el header Location: /api/users/{id}
  const location = response.headers.get('Location') ?? ''
  const id = location.split('/').pop() ?? ''
  return { id }
}

export async function getUserProfile(userId: string): Promise<UserProfile> {
  const response = await fetchWithAuth(`/api/users/${userId}`)

  if (!response.ok) {
    throw await parseError(response)
  }

  return response.json()
}

export async function updateUserProfile(
  userId: string,
  data: UpdateProfileData
): Promise<UserProfile> {
  const response = await fetchWithAuth(`/api/users/${userId}`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })

  if (!response.ok) {
    throw await parseError(response)
  }

  return response.json()
}

export async function getCategories(): Promise<Category[]> {
  const response = await fetch(`${BASE_URL}/api/categories`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createCategory(data: CategoryData): Promise<Category> {
  const response = await fetchWithAuth('/api/categories', {
    method: 'POST',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function updateCategory(id: string, data: CategoryData): Promise<Category> {
  const response = await fetchWithAuth(`/api/categories/${id}`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function deleteCategory(id: string): Promise<void> {
  const response = await fetchWithAuth(`/api/categories/${id}`, { method: 'DELETE' })
  if (!response.ok) throw await parseError(response)
}

export interface NewsItem {
  id:          string
  title:       string
  slug:        string
  content:     string
  hasImage:    boolean
  imageType:   string | null
  imageUrl:    string | null  // presigned S3 GET URL; null for legacy images (use newsImageUrl())
  published:   boolean
  publishedAt: string | null
  authorId:    string
  createdAt:   string
  updatedAt:   string
}

export interface NewsData {
  title:       string
  content:     string
  imageKey:    string | null  // S3 object key from presigned upload
  removeImage: boolean
  published:   boolean
}

export async function getNews(): Promise<NewsItem[]> {
  const response = await fetch(`${BASE_URL}/api/news`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getAllNews(): Promise<NewsItem[]> {
  const response = await fetchWithAuth('/api/news/all')
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getNewsItem(id: string): Promise<NewsItem> {
  const response = await fetch(`${BASE_URL}/api/news/${id}`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export function newsImageUrl(id: string): string {
  return `${BASE_URL}/api/news/${id}/image`
}

export async function createNews(data: Omit<NewsData, 'removeImage'>): Promise<NewsItem> {
  const response = await fetchWithAuth('/api/news', {
    method: 'POST',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function updateNews(id: string, data: NewsData): Promise<NewsItem> {
  const response = await fetchWithAuth(`/api/news/${id}`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function deleteNews(id: string): Promise<void> {
  const response = await fetchWithAuth(`/api/news/${id}`, { method: 'DELETE' })
  if (!response.ok) throw await parseError(response)
}

export async function getChampionships(): Promise<Championship[]> {
  const response = await fetch(`${BASE_URL}/api/championships`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getAllChampionships(): Promise<Championship[]> {
  const response = await fetchWithAuth('/api/championships/all')
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getChampionship(id: string): Promise<Championship> {
  const response = await fetch(`${BASE_URL}/api/championships/${id}`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createChampionship(data: ChampionshipData): Promise<Championship> {
  const response = await fetchWithAuth('/api/championships', {
    method: 'POST',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function updateChampionship(id: string, data: ChampionshipData): Promise<Championship> {
  const response = await fetchWithAuth(`/api/championships/${id}`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function deleteChampionship(id: string): Promise<void> {
  const response = await fetchWithAuth(`/api/championships/${id}`, { method: 'DELETE' })
  if (!response.ok) throw await parseError(response)
}

export interface ResultItem {
  id:              string
  title:           string
  description:     string | null
  pdfOriginalSize: number
  pdfFileName:     string | null
  published:       boolean
  publishedAt:     string | null
  authorId:        string
  createdAt:       string
  updatedAt:       string
}

export interface ResultData {
  title:       string
  description: string | null
  pdfBase64:   string | null
  pdfFileName: string | null
  published:   boolean
}

export function resultPdfUrl(id: string): string {
  return `${BASE_URL}/api/results/${id}/pdf`
}

export async function getResults(): Promise<ResultItem[]> {
  const response = await fetch(`${BASE_URL}/api/results`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getAllResults(): Promise<ResultItem[]> {
  const response = await fetchWithAuth('/api/results/all')
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createResult(data: ResultData): Promise<ResultItem> {
  const response = await fetchWithAuth('/api/results', {
    method: 'POST',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function updateResult(id: string, data: ResultData): Promise<ResultItem> {
  const response = await fetchWithAuth(`/api/results/${id}`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function deleteResult(id: string): Promise<void> {
  const response = await fetchWithAuth(`/api/results/${id}`, { method: 'DELETE' })
  if (!response.ok) throw await parseError(response)
}

export interface RegistrationItem {
  id:                   string
  championshipId:       string
  championshipName:     string
  championshipLocation: string
  eventDate:            string
  categoryId:           string
  categoryName:         string
  categoryShift:        'MORNING' | 'AFTERNOON'
  categoryArmSide:      'RIGHT' | 'LEFT' | 'BOTH'
  amount:               number
  paymentStatus:        'PENDING' | 'PAID' | 'CANCELLED'
  createdAt:            string
}

export async function getUserRegistrations(userId: string): Promise<RegistrationItem[]> {
  const response = await fetchWithAuth(`/api/users/${userId}/registrations`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function registerForChampionship(
  championshipId: string,
  categoryId: string
): Promise<RegistrationItem> {
  const response = await fetchWithAuth(`/api/championships/${championshipId}/registrations`, {
    method: 'POST',
    body:   JSON.stringify({ categoryId }),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createStripeRegistrationCheckout(registrationId: string): Promise<{ sessionId: string; checkoutUrl: string }> {
  const response = await fetchWithAuth(`/api/registrations/${registrationId}/checkout`, { method: 'POST' })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export interface ChampionshipRegistrationPaymentPreview {
  total: number
  currency: string
  categoryIds: string[]
  items: Array<{ categoryId: string; name: string; armSide: Category['armSide']; weightLimit: number | null }>
}

export async function previewChampionshipRegistrationPayment(championshipId: string, categoryIds: string[]): Promise<ChampionshipRegistrationPaymentPreview> {
  const response = await fetchWithAuth(`/api/championships/${championshipId}/registration-payment/preview`, { method: 'POST', body: JSON.stringify({ categoryIds }) })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createChampionshipRegistrationCheckout(championshipId: string, categoryIds: string[]): Promise<{ sessionId: string; checkoutUrl: string }> {
  const response = await fetchWithAuth(`/api/championships/${championshipId}/registration-payment/checkout`, { method: 'POST', body: JSON.stringify({ categoryIds }) })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export interface AdminRegistrationItem {
  id:              string
  userId:          string
  userFirstName:   string
  userLastName:    string
  userEmail:       string
  userDni:         string
  categoryId:      string
  categoryName:    string
  categoryShift:   'MORNING' | 'AFTERNOON'
  categoryArmSide: 'RIGHT' | 'LEFT' | 'BOTH'
  amount:          number
  paymentStatus:   'PENDING' | 'PAID' | 'CANCELLED'
  notes:           string | null
  createdAt:       string
}

export async function getChampionshipRegistrations(championshipId: string): Promise<AdminRegistrationItem[]> {
  const response = await fetchWithAuth(`/api/championships/${championshipId}/registrations`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function downloadChampionshipRegistrationsPdf(championshipId: string): Promise<void> {
  const response = await fetchWithAuth(`/api/championships/${championshipId}/registrations/pdf`)
  if (!response.ok) throw await parseError(response)
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = 'inscritos-campeonato.pdf'
  link.click()
  URL.revokeObjectURL(url)
}

export async function updatePaymentStatus(
  registrationId: string,
  status: 'PENDING' | 'PAID'
): Promise<void> {
  const response = await fetchWithAuth(`/api/registrations/${registrationId}/payment`, {
    method: 'PUT',
    body:   JSON.stringify({ status }),
  })
  if (!response.ok) throw await parseError(response)
}

export async function changeRegistrationCategory(
  registrationId: string,
  categoryId: string
): Promise<void> {
  const response = await fetchWithAuth(`/api/registrations/${registrationId}/category`, {
    method: 'PUT',
    body:   JSON.stringify({ categoryId }),
  })
  if (!response.ok) throw await parseError(response)
}

export interface UserListResponse {
  users:      UserProfile[]
  total:      number
  page:       number
  totalPages: number
}

export async function adminGetUsers(params?: {
  search?: string
  role?: string
  currentQuota?: 'PAID' | 'PENDING'
  page?: number
  size?: number
}): Promise<UserListResponse> {
  const q = new URLSearchParams()
  if (params?.search) q.set('search', params.search)
  if (params?.role)   q.set('role',   params.role)
  if (params?.currentQuota) q.set('currentQuota', params.currentQuota)
  if (params?.page   != null) q.set('page', String(params.page))
  if (params?.size   != null) q.set('size', String(params.size))

  const response = await fetchWithAuth(`/api/users?${q}`)
  if (!response.ok) throw await parseError(response)

  const users: UserProfile[] = await response.json()
  return {
    users,
    total:      Number(response.headers.get('X-Total-Count') ?? users.length),
    page:       Number(response.headers.get('X-Page')        ?? 0),
    totalPages: Number(response.headers.get('X-Total-Pages') ?? 1),
  }
}

export interface AdminUpdateUserData {
  firstName: string
  lastName:  string
  phone?:    string | null
  birthDate?: string | null
  role?:     string | null
  blocked?:  boolean | null
}

export async function adminUpdateUser(userId: string, data: AdminUpdateUserData): Promise<UserProfile> {
  const response = await fetchWithAuth(`/api/users/${userId}/admin`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function adminDeleteUser(userId: string): Promise<void> {
  const response = await fetchWithAuth(`/api/users/${userId}`, { method: 'DELETE' })
  if (!response.ok) throw await parseError(response)
}

export async function changePassword(
  userId: string,
  data: { currentPassword: string; newPassword: string }
): Promise<void> {
  const response = await fetchWithAuth(`/api/users/${userId}/password`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })

  if (!response.ok) {
    throw await parseError(response)
  }
}

// ── Equipos ───────────────────────────────────────────────────────────────────

export interface Team {
  id:                  string
  name:                string
  responsibleName:     string
  responsibleLastName: string
  phone:               string | null
  province:            string
  locality:            string
  hasLogo:             boolean
  logoType:            string | null
}

export interface TeamData {
  name:                string
  responsibleName:     string
  responsibleLastName: string
  phone:               string | null
  province:            string
  locality:            string
  logoBase64:          string | null
  logoType:            string | null
}

export function teamLogoUrl(id: string): string {
  return `${BASE_URL}/api/teams/${id}/logo`
}

export async function getTeams(): Promise<Team[]> {
  const response = await fetch(`${BASE_URL}/api/teams`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createTeam(data: TeamData): Promise<Team> {
  const response = await fetchWithAuth('/api/teams', {
    method: 'POST',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function updateTeam(id: string, data: TeamData): Promise<Team> {
  const response = await fetchWithAuth(`/api/teams/${id}`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function deleteTeam(id: string): Promise<void> {
  const response = await fetchWithAuth(`/api/teams/${id}`, { method: 'DELETE' })
  if (!response.ok) throw await parseError(response)
}

// ── Documentación ─────────────────────────────────────────────────────────────

export interface DocumentItem {
  id:               string
  title:            string
  description:      string | null
  fileOriginalSize: number
  fileName:         string | null
  published:        boolean
  publishedAt:      string | null
  authorId:         string
  createdAt:        string
  updatedAt:        string
}

export interface DocumentData {
  title:       string
  description: string | null
  fileBase64:  string | null
  fileName:    string | null
  published:   boolean
}

export function documentFileUrl(id: string): string {
  return `${BASE_URL}/api/documents/${id}/file`
}

export async function getDocuments(): Promise<DocumentItem[]> {
  const response = await fetch(`${BASE_URL}/api/documents`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getAllDocuments(): Promise<DocumentItem[]> {
  const response = await fetchWithAuth('/api/documents/all')
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createDocument(data: Omit<DocumentData, never>): Promise<DocumentItem> {
  const response = await fetchWithAuth('/api/documents', {
    method: 'POST',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function updateDocument(id: string, data: DocumentData): Promise<DocumentItem> {
  const response = await fetchWithAuth(`/api/documents/${id}`, {
    method: 'PUT',
    body:   JSON.stringify(data),
  })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function deleteDocument(id: string): Promise<void> {
  const response = await fetchWithAuth(`/api/documents/${id}`, { method: 'DELETE' })
  if (!response.ok) throw await parseError(response)
}

// ── Cuotas anuales ───────────────────────────────────────────────────────────

export interface AnnualQuotaPrice {
  ageCategory: string
  label: string
  minimumAge: number | null
  maximumAge: number | null
  amount: number
}

export interface UserQuota {
  id: string
  year: number
  ageCategory: string
  ageCategoryLabel: string
  amount: number
  paymentStatus: 'PENDING' | 'PAID'
  paymentDate: string | null
  paidAt: string | null
  paymentSource: 'STRIPE' | 'MANUAL' | 'LEGACY' | null
  paidByUserId: string | null
}

export async function getAnnualQuotaPrices(year: number): Promise<AnnualQuotaPrice[]> {
  const response = await fetchWithAuth(`/api/quotas/prices/${year}`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function saveAnnualQuotaPrices(year: number, prices: Pick<AnnualQuotaPrice, 'ageCategory' | 'amount'>[]): Promise<AnnualQuotaPrice[]> {
  const response = await fetchWithAuth(`/api/quotas/prices/${year}`, { method: 'PUT', body: JSON.stringify({ prices }) })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getMyQuotas(): Promise<UserQuota[]> {
  const response = await fetchWithAuth('/api/quotas/my')
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function generateMyQuota(year: number): Promise<UserQuota> {
  const response = await fetchWithAuth(`/api/quotas/my/${year}`, { method: 'POST' })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function createStripeQuotaCheckout(year: number): Promise<{ sessionId: string; checkoutUrl: string }> {
  const response = await fetchWithAuth(`/api/quotas/my/${year}/checkout`, { method: 'POST' })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function markQuotaPaid(userId: string, year: number): Promise<UserQuota> {
  const response = await fetchWithAuth(`/api/quotas/users/${userId}/${year}/payment`, { method: 'PUT' })
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function getUserQuotas(userId: string): Promise<UserQuota[]> {
  const response = await fetchWithAuth(`/api/quotas/users/${userId}`)
  if (!response.ok) throw await parseError(response)
  return response.json()
}

export async function logoutUser(): Promise<void> {
  const response = await fetchWithAuth('/api/auth/logout', { method: 'POST' })
  if (!response.ok) throw await parseError(response)
}

let csrfToken: string | null = null

async function getCsrfHeaders(): Promise<Record<string, string>> {
  if (typeof window === 'undefined') return {}

  if (!csrfToken) {
    const response = await fetch(`${BASE_URL}/api/auth/csrf`, {
      credentials: 'include',
      cache: 'no-store',
    })
    if (!response.ok) throw await parseError(response)
    const body = await response.json() as { token?: string }
    if (!body.token) {
      throw { status: response.status, detail: 'No se pudo inicializar la protección CSRF' } satisfies ApiError
    }
    csrfToken = body.token
  }

  return { 'X-XSRF-TOKEN': csrfToken }
}

// The access token cookie is short-lived by design (see audit fix #4) — a
// dedup'd promise means concurrent 401s trigger a single refresh call, and
// all of them await its result instead of racing separate rotations.
let refreshInFlight: Promise<boolean> | null = null

async function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const csrfHeaders = await getCsrfHeaders()
        const response = await fetch(`${BASE_URL}/api/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json', ...csrfHeaders },
        })
        return response.ok
      } catch {
        return false
      } finally {
        refreshInFlight = null
      }
    })()
  }
  return refreshInFlight
}

export async function fetchWithAuth(url: string, options: RequestInit = {}, isRetry = false): Promise<Response> {
  const method = (options.method ?? 'GET').toUpperCase()
  const csrfHeaders = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)
    ? await getCsrfHeaders()
    : {}

  const response = await fetch(`${BASE_URL}${url}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders,
      ...options.headers,
    },
  })

  // The access token just expired (normal, ~15 min lifetime) — try a silent
  // rotation via the refresh cookie before treating this as a logged-out user.
  if (response.status === 401 && !isRetry && (await refreshSession())) {
    return fetchWithAuth(url, options, true)
  }

  // Existing browser sessions from the Bearer-token version become invalid
  // after this migration; do not leave a misleading authenticated UI state.
  if (response.status === 401 && typeof window !== 'undefined') {
    localStorage.removeItem('aelb_session')
    localStorage.removeItem('aelb_token')
  }

  return response
}
