package es.aelb.backendweb.domain.news;

import es.aelb.backendweb.domain.news.valueobject.NewsId;
import es.aelb.backendweb.domain.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface NewsRepository {

    void               save(News news);
    Optional<News>     findById(NewsId id);
    Optional<News>     findPublishedById(NewsId id);
    List<News>         findAll();
    List<News>         findPublished();
    PageResult<News> findPage(int page, int size, boolean publishedOnly);
    /** Same as {@link #findPage} but filtered by a case-insensitive text match on title or content. */
    PageResult<News> search(String query, int page, int size, boolean publishedOnly);
    void               delete(NewsId id);
}
