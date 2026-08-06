package es.aelb.backendweb.application.team;

public record CreateTeamCommand(
        String name,
        String responsibleName,
        String responsibleLastName,
        String phone,
        String province,
        String locality,
        String logoBase64,
        String logoType
) {}
