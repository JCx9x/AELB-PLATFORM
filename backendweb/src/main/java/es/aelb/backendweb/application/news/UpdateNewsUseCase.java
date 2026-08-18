package es.aelb.backendweb.application.news;

import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.news.NewsRepository;
import es.aelb.backendweb.domain.news.valueobject.NewsId;
import es.aelb.backendweb.infrastructure.storage.StoredFileKeys;

public class UpdateNewsUseCase {

    private final NewsRepository newsRepository;

    public UpdateNewsUseCase(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public News execute(UpdateNewsCommand cmd) {
        News news = newsRepository.findById(NewsId.of(cmd.newsId()))
                .orElseThrow(() -> new News.NotFoundException(cmd.newsId()));

        news.update(cmd.title(), cmd.content());

        if (cmd.removeImage()) {
            news.removeImage();
        } else if (cmd.imageKey() != null && !cmd.imageKey().isBlank()) {
            if (!StoredFileKeys.isAllowedImageKey(cmd.imageKey(), "news")) throw new IllegalArgumentException("La imagen de la noticia no es válida");
            news.setImageKey(cmd.imageKey());
        }

        if (cmd.published()) {
            news.publish();
        } else {
            news.unpublish();
        }

        newsRepository.save(news);
        return news;
    }
}
