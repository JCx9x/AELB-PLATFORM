package es.aelb.backendweb.application.championship;

import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;

public class CreateChampionshipUseCase {

    private final ChampionshipRepository championshipRepository;

    public CreateChampionshipUseCase(ChampionshipRepository championshipRepository) {
        this.championshipRepository = championshipRepository;
    }

    public Championship execute(CreateChampionshipCommand cmd) {
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
