package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.user.User;

public record AdminRegistrationResponse(
        String id,
        String userId,
        String userFirstName,
        String userLastName,
        String userEmail,
        String userDni,
        String categoryId,
        String categoryName,
        String categoryShift,
        String categoryArmSide,
        java.math.BigDecimal amount,
        String paymentStatus,
        String notes,
        String createdAt
) {
    public static AdminRegistrationResponse from(Registration reg, User user, Category cat) {
        return new AdminRegistrationResponse(
                reg.getId(),
                user.getId().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail().value(),
                user.getDni().value(),
                cat.getId().value(),
                cat.getName(),
                cat.getShift().name(),
                cat.getArmSide().name(),
                reg.getAmount(),
                reg.getPaymentStatus().name(),
                reg.getNotes(),
                reg.getCreatedAt().toString()
        );
    }
}
