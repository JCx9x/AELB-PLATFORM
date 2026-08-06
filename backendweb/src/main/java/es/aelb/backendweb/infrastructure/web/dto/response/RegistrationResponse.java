package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.registration.Registration;

public record RegistrationResponse(
        String id,
        String championshipId,
        String championshipName,
        String championshipLocation,
        String eventDate,
        String categoryId,
        String categoryName,
        String categoryShift,
        String categoryArmSide,
        java.math.BigDecimal amount,
        String paymentStatus,
        String createdAt
) {
    public static RegistrationResponse from(Registration reg, Championship ch, Category cat) {
        return new RegistrationResponse(
                reg.getId(),
                ch.getId().value(),
                ch.getName(),
                ch.getLocation(),
                ch.getEventDate().toString(),
                cat.getId().value(),
                cat.getName(),
                cat.getShift().name(),
                cat.getArmSide().name(),
                reg.getAmount(),
                reg.getPaymentStatus().name(),
                reg.getCreatedAt().toString()
        );
    }
}
