package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.news.CreateNewsCommand;
import es.aelb.backendweb.application.news.CreateNewsUseCase;
import es.aelb.backendweb.application.news.DeleteNewsUseCase;
import es.aelb.backendweb.application.news.UpdateNewsCommand;
import es.aelb.backendweb.application.news.UpdateNewsUseCase;
import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.news.NewsRepository;
import es.aelb.backendweb.domain.news.valueobject.NewsId;
import es.aelb.backendweb.infrastructure.storage.PresignedUrlService;
import es.aelb.backendweb.infrastructure.web.dto.request.CreateNewsRequest;
import es.aelb.backendweb.infrastructure.web.dto.request.UpdateNewsRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.NewsResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final CreateNewsUseCase    createNewsUseCase;
    private final UpdateNewsUseCase    updateNewsUseCase;
    private final DeleteNewsUseCase    deleteNewsUseCase;
    private final NewsRepository       newsRepository;
    private final PresignedUrlService  presignedUrlService;

    public NewsController(
            CreateNewsUseCase   createNewsUseCase,
            UpdateNewsUseCase   updateNewsUseCase,
            DeleteNewsUseCase   deleteNewsUseCase,
            NewsRepository      newsRepository,
            PresignedUrlService presignedUrlService
    ) {
        this.createNewsUseCase   = createNewsUseCase;
        this.updateNewsUseCase   = updateNewsUseCase;
        this.deleteNewsUseCase   = deleteNewsUseCase;
        this.newsRepository      = newsRepository;
        this.presignedUrlService = presignedUrlService;
    }

    @GetMapping
    public ResponseEntity<List<NewsResponse>> findPublished() {
        return ResponseEntity.ok(newsRepository.findPublished().stream()
                .map(this::toResponse).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<NewsResponse>> findAll() {
        return ResponseEntity.ok(newsRepository.findAll().stream()
                .map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> findById(@PathVariable String id) {
        return newsRepository.findById(NewsId.of(id))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        News news = newsRepository.findById(NewsId.of(id)).orElse(null);
        if (news == null || !news.hasLegacyImage()) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageBytes = Base64.getDecoder().decode(news.getImageData());
        String contentType = news.getImageType() != null
                ? news.getImageType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Cache-Control", "public, max-age=86400")
                .body(imageBytes);
    }

    @PostMapping
    public ResponseEntity<NewsResponse> create(
            @Valid @RequestBody CreateNewsRequest req,
            Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        News created = createNewsUseCase.execute(new CreateNewsCommand(
                req.title(), req.content(),
                req.imageKey(),
                req.published(), userId
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId().value()).toUri();
        return ResponseEntity.created(location).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateNewsRequest req
    ) {
        News updated = updateNewsUseCase.execute(new UpdateNewsCommand(
                id, req.title(), req.content(),
                req.imageKey(),
                req.removeImage(), req.published()
        ));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteNewsUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    private NewsResponse toResponse(News news) {
        String imageUrl = null;
        if (news.getImageKey() != null && !news.getImageKey().isBlank()) {
            imageUrl = presignedUrlService.generateReadUrl(news.getImageKey());
        }
        return NewsResponse.from(news, imageUrl);
    }
}
