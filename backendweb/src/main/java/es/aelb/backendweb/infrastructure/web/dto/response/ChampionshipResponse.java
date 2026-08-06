package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.championship.Championship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record ChampionshipResponse(
        String        id,
        String        name,
        String        location,
        LocalDate     eventDate,
        LocalDate     registrationDeadline,
        BigDecimal    price,
        boolean       visible,
        String        imageUrl,    // presigned GET URL for display (or legacy URL); null if no image
        String        imageKey,    // raw S3 object key stored in DB — use this in update payloads
        String        description,
        boolean       requiresCurrentQuota,
        Set<String>   categoryIds,
        String        createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChampionshipResponse from(Championship c, String resolvedImageUrl) {
        return new ChampionshipResponse(
                c.getId().value(),
                c.getName(),
                c.getLocation(),
                c.getEventDate(),
                c.getRegistrationDeadline(),
                c.getPrice(),
                c.isVisible(),
                resolvedImageUrl,
                c.getImageUrl(),
                c.getDescription(),
                c.requiresCurrentQuota(),
                c.getCategoryIds(),
                c.getCreatedBy().value(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
