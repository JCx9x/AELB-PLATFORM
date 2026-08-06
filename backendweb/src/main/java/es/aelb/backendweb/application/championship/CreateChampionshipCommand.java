package es.aelb.backendweb.application.championship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record CreateChampionshipCommand(
        String     name,
        String     location,
        LocalDate  eventDate,
        LocalDate  registrationDeadline,
        BigDecimal price,
        String     imageKey,   // nullable — S3 object key or legacy URL
        String     description,
        boolean    requiresCurrentQuota,
        String     createdByUserId,
        Set<String> categoryIds
) {}
