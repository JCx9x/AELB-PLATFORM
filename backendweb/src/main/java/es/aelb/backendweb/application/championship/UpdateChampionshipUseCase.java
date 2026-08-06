package es.aelb.backendweb.application.championship;

import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;

import java.util.HashSet;
import java.util.Set;

public class UpdateChampionshipUseCase {

    private final ChampionshipRepository championshipRepository;

    public UpdateChampionshipUseCase(ChampionshipRepository championshipRepository) {
        this.championshipRepository = championshipRepository;
    }

    public Championship execute(UpdateChampionshipCommand cmd) {
        Championship c = championshipRepository.findById(ChampionshipId.of(cmd.championshipId()))
                .orElseThrow(() -> new Championship.NotFoundException(cmd.championshipId()));

        // imageKey semantics: null=keep existing | ""=remove | "key"=new value
        String imageUrl;
        if (cmd.imageKey() == null) {
            imageUrl = c.getImageUrl();
        } else if (cmd.imageKey().isEmpty()) {
            imageUrl = null;
        } else {
            imageUrl = cmd.imageKey();
        }

        c.updateDetails(cmd.name(), cmd.location(),
                cmd.eventDate(), cmd.registrationDeadline(),
                cmd.price(), imageUrl, cmd.description(), cmd.requiresCurrentQuota());

        Set<String> current = new HashSet<>(c.getCategoryIds());
        Set<String> desired = cmd.categoryIds() != null ? new HashSet<>(cmd.categoryIds()) : new HashSet<>();
        current.stream().filter(id -> !desired.contains(id)).forEach(c::removeCategory);
        desired.stream().filter(id -> !current.contains(id)).forEach(c::addCategory);

        if (cmd.visible()) {
            c.publish();
        } else {
            c.unpublish();
        }

        championshipRepository.save(c);
        return c;
    }
}
