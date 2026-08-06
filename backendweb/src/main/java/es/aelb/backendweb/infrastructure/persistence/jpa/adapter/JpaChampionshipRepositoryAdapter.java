package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.ChampionshipJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringChampionshipJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaChampionshipRepositoryAdapter implements ChampionshipRepository {

    private final SpringChampionshipJpaRepository jpaRepository;

    public JpaChampionshipRepositoryAdapter(SpringChampionshipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Championship championship) {
        jpaRepository.save(ChampionshipJpaMapper.toJpa(championship));
    }

    @Override
    public Optional<Championship> findById(ChampionshipId id) {
        return jpaRepository.findById(id.value()).map(ChampionshipJpaMapper::toDomain);
    }

    @Override
    public List<Championship> findAll() {
        return jpaRepository.findAll().stream().map(ChampionshipJpaMapper::toDomain).toList();
    }

    @Override
    public List<Championship> findVisible() {
        return jpaRepository.findByVisibleTrue().stream().map(ChampionshipJpaMapper::toDomain).toList();
    }

    @Override
    public void delete(ChampionshipId id) {
        jpaRepository.deleteById(id.value());
    }
}
