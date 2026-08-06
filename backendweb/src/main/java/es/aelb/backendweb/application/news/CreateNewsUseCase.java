package es.aelb.backendweb.application.news;

import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.news.NewsRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.text.Normalizer;

public class CreateNewsUseCase {

    private final NewsRepository newsRepository;

    public CreateNewsUseCase(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public News execute(CreateNewsCommand cmd) {
        String slug = toSlug(cmd.title()) + "-" + System.currentTimeMillis();
        News news = News.create(cmd.title(), slug, cmd.content(), UserId.of(cmd.authorUserId()));

        if (cmd.imageKey() != null && !cmd.imageKey().isBlank()) {
            news.setImageKey(cmd.imageKey());
        }

        if (cmd.published()) {
            news.publish();
        }

        newsRepository.save(news);
        return news;
    }

    static String toSlug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "noticia" : slug;
    }
}
