package es.aelb.backendweb.application.registration;

public record RegisterForChampionshipCommand(
        String userId,
        String championshipId,
        String categoryId,
        String registeredById   // quien ejecuta la acción (puede ser el mismo usuario o un gestor)
) {}
