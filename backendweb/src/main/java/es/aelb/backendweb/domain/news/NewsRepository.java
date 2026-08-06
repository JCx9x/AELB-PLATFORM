package es.aelb.backendweb.domain.news;

import es.aelb.backendweb.domain.news.valueobject.NewsId;

import java.util.List;
import java.util.Optional;

public interface NewsRepository {

    void               save(News news);
    Optional<News>     findById(NewsId id);
    List<News>         findAll();
    List<News>         findPublished();
    void               delete(NewsId id);
}
