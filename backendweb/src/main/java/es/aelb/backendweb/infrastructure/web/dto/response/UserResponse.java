package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        String        id,
        String        email,
        String        firstName,
        String        lastName,
        String        dni,
        String        phone,
        String        city,
        String        province,
        String        nationality,
        LocalDate     birthDate,
        String        role,
        boolean       blocked,
        String        teamId,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getEmail().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getDni().value(),
                user.getPhone(),
                user.getCity(),
                user.getProvince(),
                user.getNationality(),
                user.getBirthDate(),
                user.getRole().name(),
                user.isBlocked(),
                user.getTeamId(),
                user.getCreatedAt()
        );
    }
}
