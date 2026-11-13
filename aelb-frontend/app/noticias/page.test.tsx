import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import NoticiasPage from './page'
import { getNews, type NewsItem, type Paged } from '../lib/api'

vi.mock('../lib/api', () => ({
  getNews: vi.fn(),
  newsImageUrl: (id: string) => `/api/news/${id}/image`,
}))

vi.mock('../components/Navbar', () => ({ default: () => null }))

const mockGetNews = vi.mocked(getNews)

function newsItem(n: number): NewsItem {
  return {
    id: `n${n}`, title: `Noticia ${n}`, slug: `noticia-${n}`, content: `Contenido ${n}`,
    hasImage: false, imageType: null, imageUrl: null, published: true,
    publishedAt: '2026-10-01T10:00:00Z', authorId: 'a1',
    createdAt: '2026-10-01T10:00:00Z', updatedAt: '2026-10-01T10:00:00Z',
  }
}

function paged(items: NewsItem[], page: number, totalPages: number): Paged<NewsItem> {
  return { items, page, size: 12, totalElements: totalPages * 12, totalPages }
}

describe('NoticiasPage', () => {
  beforeEach(() => {
    mockGetNews.mockReset()
    window.scrollTo = vi.fn()
  })

  it('hides pagination when everything fits in one page', async () => {
    mockGetNews.mockResolvedValue(paged([newsItem(1)], 0, 1))
    render(<NoticiasPage />)

    expect(await screen.findByText('Noticia 1')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: /paginación/i })).not.toBeInTheDocument()
  })

  it('loads the next page when the user clicks "Siguiente"', async () => {
    mockGetNews.mockImplementation(async (page = 0) => paged([newsItem(page + 1)], page, 3))
    const user = userEvent.setup()
    render(<NoticiasPage />)

    expect(await screen.findByText('Noticia 1')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /siguiente/i }))

    expect(await screen.findByText('Noticia 2')).toBeInTheDocument()
    expect(mockGetNews).toHaveBeenLastCalledWith(1, 12, '')
    expect(screen.getByRole('button', { name: 'Página 2' })).toHaveAttribute('aria-current', 'page')
  })

  it('searches news and resets to the first page', async () => {
    mockGetNews.mockImplementation(async (page = 0, _size, query) =>
      query ? paged([], 0, 0) : paged([newsItem(page + 1)], page, 3))
    const user = userEvent.setup()
    render(<NoticiasPage />)

    await screen.findByText('Noticia 1')
    await user.click(screen.getByRole('button', { name: /siguiente/i }))
    await screen.findByText('Noticia 2')

    await user.type(screen.getByRole('searchbox', { name: /buscar noticias/i }), 'mundial')

    await waitFor(() => expect(mockGetNews).toHaveBeenLastCalledWith(0, 12, 'mundial'))
    expect(await screen.findByText(/no hay noticias que coincidan con «mundial»/i)).toBeInTheDocument()
  })
})
