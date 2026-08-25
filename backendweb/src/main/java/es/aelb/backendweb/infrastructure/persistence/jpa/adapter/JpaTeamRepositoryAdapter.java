package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.team.Team;
import es.aelb.backendweb.domain.team.TeamRepository;
import es.aelb.backendweb.domain.team.valueobject.TeamId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.TeamJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringTeamJpaRepository;
import es.aelb.backendweb.domain.shared.pagination.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaTeamRepositoryAdapter implements TeamRepository {

    private final SpringTeamJpaRepository jpaRepository;

    public JpaTeamRepositoryAdapter(SpringTeamJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Team team) {
        jpaRepository.save(TeamJpaMapper.toJpa(team));
    }

    @Override
    public Optional<Team> findById(TeamId id) {
        return jpaRepository.findById(id.value())
                .map(TeamJpaMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, TeamId excludeId) {
        return jpaRepository.existsByNameAndIdNot(name, excludeId.value());
    }

    @Override
    public List<Team> findAll() {
        return jpaRepository.findAll().stream()
                .map(TeamJpaMapper::toDomain)
                .toList();
    }

    @Override public PageResult<Team> findPage(int page, int size) {
        var result = jpaRepository.findAll(PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(TeamJpaMapper::toDomain).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public void delete(TeamId id) {
        jpaRepository.deleteById(id.value());
    }
}
