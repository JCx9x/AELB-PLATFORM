package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.document.Document;
import es.aelb.backendweb.domain.document.DocumentRepository;
import es.aelb.backendweb.domain.document.valueobject.DocumentId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.DocumentJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringDocumentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaDocumentRepositoryAdapter implements DocumentRepository {

    private final SpringDocumentJpaRepository jpaRepository;

    public JpaDocumentRepositoryAdapter(SpringDocumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Document document) {
        jpaRepository.save(DocumentJpaMapper.toJpa(document));
    }

    @Override
    public Optional<Document> findById(DocumentId id) {
        return jpaRepository.findById(id.value()).map(DocumentJpaMapper::toDomain);
    }

    @Override
    public List<Document> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DocumentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Document> findPublished() {
        return jpaRepository.findByPublishedTrueOrderByPublishedAtDesc().stream()
                .map(DocumentJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(DocumentId id) {
        jpaRepository.deleteById(id.value());
    }
}
