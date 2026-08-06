package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.result.Result;
import es.aelb.backendweb.domain.result.ResultRepository;
import es.aelb.backendweb.domain.result.valueobject.ResultId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.ResultJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringResultJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaResultRepositoryAdapter implements ResultRepository {

    private final SpringResultJpaRepository jpaRepository;

    public JpaResultRepositoryAdapter(SpringResultJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Result result) {
        jpaRepository.save(ResultJpaMapper.toJpa(result));
    }

    @Override
    public Optional<Result> findById(ResultId id) {
        return jpaRepository.findById(id.value()).map(ResultJpaMapper::toDomain);
    }

    @Override
    public List<Result> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ResultJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<Result> findPublished() {
        return jpaRepository.findByPublishedTrueOrderByPublishedAtDesc().stream()
                .map(ResultJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(ResultId id) {
        jpaRepository.deleteById(id.value());
    }
}
