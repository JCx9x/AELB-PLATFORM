package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.news.News;
import es.aelb.backendweb.domain.news.NewsRepository;
import es.aelb.backendweb.domain.news.valueobject.NewsId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.NewsJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringNewsJpaRepository;
import es.aelb.backendweb.domain.shared.pagination.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaNewsRepositoryAdapter implements NewsRepository {

    private final SpringNewsJpaRepository jpaRepository;

    public JpaNewsRepositoryAdapter(SpringNewsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(News news) {
        jpaRepository.save(NewsJpaMapper.toJpa(news));
    }

    @Override
    public Optional<News> findById(NewsId id) {
        return jpaRepository.findById(id.value()).map(NewsJpaMapper::toDomain);
    }

    @Override
    public Optional<News> findPublishedById(NewsId id) {
        return jpaRepository.findByIdAndPublishedTrue(id.value()).map(NewsJpaMapper::toDomain);
    }

    @Override
    public List<News> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NewsJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<News> findPublished() {
        return jpaRepository.findByPublishedTrueOrderByPublishedAtDesc().stream()
                .map(NewsJpaMapper::toDomain)
                .toList();
    }

    @Override public PageResult<News> findPage(int page, int size, boolean publishedOnly) {
        var result = publishedOnly ? jpaRepository.findByPublishedTrueOrderByPublishedAtDesc(PageRequest.of(page, size)) : jpaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(NewsJpaMapper::toDomain).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public void delete(NewsId id) {
        jpaRepository.deleteById(id.value());
    }
}
