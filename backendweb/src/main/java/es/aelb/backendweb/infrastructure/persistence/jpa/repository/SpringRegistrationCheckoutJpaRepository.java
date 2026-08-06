package es.aelb.backendweb.infrastructure.persistence.jpa.repository;

import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RegistrationCheckoutJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface SpringRegistrationCheckoutJpaRepository
        extends JpaRepository<RegistrationCheckoutJpaEntity, String> {

    List<RegistrationCheckoutJpaEntity> findByUserId(String userId);
    List<RegistrationCheckoutJpaEntity> findByChampionshipId(String championshipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from RegistrationCheckoutJpaEntity c where c.id = :id")
    java.util.Optional<RegistrationCheckoutJpaEntity> findByIdForUpdate(@Param("id") String id);

    java.util.Optional<RegistrationCheckoutJpaEntity> findByBasketKey(String basketKey);
}
