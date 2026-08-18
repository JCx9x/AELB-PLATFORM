package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA. Solo se usa dentro del adaptador de infraestructura. */
public interface SpringUserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByDni(String dni);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserJpaEntity u where u.id = :id")
    Optional<UserJpaEntity> findByIdForUpdate(@Param("id") String id);
    boolean                 existsByEmail(String email);
    boolean                 existsByDni(String dni);
    java.util.List<UserJpaEntity> findByTeamId(String teamId);
}
