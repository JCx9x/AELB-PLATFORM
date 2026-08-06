package es.aelb.backendweb.application.news;

import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.news.NewsRepository;
import es.aelb.backendweb.domain.news.valueobject.NewsId;

public class DeleteNewsUseCase {

    private final NewsRepository newsRepository;

    public DeleteNewsUseCase(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public void execute(String newsId) {
        newsRepository.findById(NewsId.of(newsId))
                .orElseThrow(() -> new News.NotFoundException(newsId));
        newsRepository.delete(NewsId.of(newsId));
    }
}
