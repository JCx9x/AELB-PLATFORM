package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRole;
import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.UserJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.UserJpaEntity.RoleJpa;

/**
 * Traduce entre el objeto de dominio User y la entidad JPA UserJpaEntity.
 * Punto de corte entre dominio e infraestructura.
 */
public class UserJpaMapper {

    private UserJpaMapper() {}

    public static UserJpaEntity toJpa(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().value());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setDni(user.getDni().value());
        entity.setPhone(user.getPhone());
        entity.setCity(user.getCity());
        entity.setProvince(user.getProvince());
        entity.setNationality(user.getNationality());
        entity.setBirthDate(user.getBirthDate());
        entity.setRole(RoleJpa.valueOf(user.getRole().name()));
        entity.setBlocked(user.isBlocked());
        entity.setTeamId(user.getTeamId());
        entity.setTokenVersion(user.getTokenVersion());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }

    public static User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                Email.of(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getFirstName(),
                entity.getLastName(),
                Dni.of(entity.getDni()),
                entity.getPhone(),
                entity.getCity(),
                entity.getProvince(),
                entity.getNationality(),
                entity.getBirthDate(),
                UserRole.valueOf(entity.getRole().name()),
                entity.isBlocked(),
                entity.getTeamId(),
                entity.getTokenVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
