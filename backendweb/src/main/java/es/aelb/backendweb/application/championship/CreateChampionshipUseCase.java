package es.aelb.backendweb.application.championship;

import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import es.aelb.backendweb.infrastructure.storage.StoredFileKeys;

public class CreateChampionshipUseCase {

    private final ChampionshipRepository championshipRepository;

    public CreateChampionshipUseCase(ChampionshipRepository championshipRepository) {
        this.championshipRepository = championshipRepository;
    }

    public Championship execute(CreateChampionshipCommand cmd) {
        if (cmd.imageKey() != null && !cmd.imageKey().isBlank()
                && !StoredFileKeys.isAllowedImageKey(cmd.imageKey(), "championships")) {
            throw new IllegalArgumentException("La imagen del campeonato no es válida");
        }
        Championship c = Championship.create(
                cmd.name(), cmd.location(),
                cmd.eventDate(), cmd.registrationDeadline(),
                cmd.price(), UserId.of(cmd.createdByUserId())
        );

        c.updateDetails(cmd.name(), cmd.location(),
                cmd.eventDate(), cmd.registrationDeadline(),
                cmd.price(), cmd.imageKey(), cmd.description(), cmd.requiresCurrentQuota());

        if (cmd.categoryIds() != null) {
            cmd.categoryIds().forEach(c::addCategory);
        }

        championshipRepository.save(c);
        return c;
    }
}
