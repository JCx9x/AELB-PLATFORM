package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data JPA. Solo se usa dentro del adaptador de infraestructura. */
public interface SpringUserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByDni(String dni);
    boolean                 existsByEmail(String email);
    boolean                 existsByDni(String dni);
    java.util.List<UserJpaEntity> findByTeamId(String teamId);
}
