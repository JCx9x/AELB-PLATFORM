package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.registration.PaymentStatus;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RegistrationJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RegistrationJpaEntity.PaymentStatusJpa;

public class RegistrationJpaMapper {

    private RegistrationJpaMapper() {}

    public static RegistrationJpaEntity toJpa(Registration registration) {
        RegistrationJpaEntity entity = new RegistrationJpaEntity();
        entity.setId(registration.getId());
        entity.setUserId(registration.getUserId().value());
        entity.setChampionshipId(registration.getChampionshipId().value());
        entity.setCategoryId(registration.getCategoryId());
        entity.setAmount(registration.getAmount());
        entity.setPaymentStatus(PaymentStatusJpa.valueOf(registration.getPaymentStatus().name()));
        entity.setNotes(registration.getNotes());
        entity.setRegisteredBy(registration.getRegisteredBy().value());
        entity.setCreatedAt(registration.getCreatedAt());
        entity.setUpdatedAt(registration.getUpdatedAt());
        return entity;
    }

    public static Registration toDomain(RegistrationJpaEntity entity) {
        return Registration.reconstitute(
                entity.getId(),
                UserId.of(entity.getUserId()),
                ChampionshipId.of(entity.getChampionshipId()),
                entity.getCategoryId(),
                entity.getAmount(),
                PaymentStatus.valueOf(entity.getPaymentStatus().name()),
                entity.getNotes(),
                UserId.of(entity.getRegisteredBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
