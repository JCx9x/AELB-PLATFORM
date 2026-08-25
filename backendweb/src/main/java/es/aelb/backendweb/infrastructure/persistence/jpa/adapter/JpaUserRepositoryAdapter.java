package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.UserJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringUserJpaRepository;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.UserJpaEntity;
import es.aelb.backendweb.domain.shared.pagination.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de infraestructura (driven adapter).
 * Implementa el puerto UserRepository del dominio
 * delegando en Spring Data JPA.
 */
@Component
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringUserJpaRepository jpaRepository;

    public JpaUserRepositoryAdapter(SpringUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(User user) {
        jpaRepository.save(UserJpaMapper.toJpa(user));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByDni(Dni dni) {
        return jpaRepository.findByDni(dni.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    @Override
    public boolean existsByDni(Dni dni) {
        return jpaRepository.existsByDni(dni.value());
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(UserJpaMapper::toDomain).toList();
    }

    @Override public PageResult<User> findPage(int page, int size) {
        var result = jpaRepository.findAll(PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(UserJpaMapper::toDomain).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Override public PageResult<User> findPage(int page, int size, String search, String role) {
        var roleValue = role == null || role.isBlank() ? null : UserJpaEntity.RoleJpa.valueOf(role.toUpperCase());
        var result = jpaRepository.findPageBySearchAndRole(search == null ? "" : search.trim(), roleValue, PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(UserJpaMapper::toDomain).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public List<User> findByTeamId(String teamId) {
        return jpaRepository.findByTeamId(teamId).stream().map(UserJpaMapper::toDomain).toList();
    }

    @Override
    public void delete(UserId id) {
        jpaRepository.deleteById(id.value());
    }
}
