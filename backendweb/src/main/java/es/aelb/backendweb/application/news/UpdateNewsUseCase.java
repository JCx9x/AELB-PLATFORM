package es.aelb.backendweb.application.news;

import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.news.NewsRepository;
import es.aelb.backendweb.domain.news.valueobject.NewsId;

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
