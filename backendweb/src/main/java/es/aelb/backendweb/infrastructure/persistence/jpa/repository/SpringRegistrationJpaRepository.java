package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RegistrationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringRegistrationJpaRepository extends JpaRepository<RegistrationJpaEntity, String> {

    boolean existsByUserIdAndChampionshipId(String userId, String championshipId);
    List<RegistrationJpaEntity> findByUserIdAndChampionshipId(String userId, String championshipId);
    List<RegistrationJpaEntity> findByChampionshipId(String championshipId);
    List<RegistrationJpaEntity> findByUserId(String userId);
}
