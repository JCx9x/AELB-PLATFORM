package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    Page<UserJpaEntity> findAll(Pageable pageable);
    @Query("select u from UserJpaEntity u where (:search = '' or lower(u.email) like lower(concat('%', :search, '%')) or lower(u.firstName) like lower(concat('%', :search, '%')) or lower(u.lastName) like lower(concat('%', :search, '%')) or lower(u.dni) like lower(concat('%', :search, '%'))) and (:role is null or u.role = :role) order by u.createdAt desc")
    Page<UserJpaEntity> findPageBySearchAndRole(@Param("search") String search, @Param("role") UserJpaEntity.RoleJpa role, Pageable pageable);
}
