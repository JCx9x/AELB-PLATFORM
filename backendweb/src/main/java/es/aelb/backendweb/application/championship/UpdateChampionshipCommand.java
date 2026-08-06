package es.aelb.backendweb.application.championship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record UpdateChampionshipCommand(
        String     championshipId,
        String     name,
        String     location,
        LocalDate  eventDate,
        LocalDate  registrationDeadline,
        BigDecimal price,
        String     imageKey,   // null=keep existing | ""=remove | "key/path"=new S3 key
        String     description,
        boolean    requiresCurrentQuota,
        boolean    visible,
        Set<String> categoryIds
) {}
