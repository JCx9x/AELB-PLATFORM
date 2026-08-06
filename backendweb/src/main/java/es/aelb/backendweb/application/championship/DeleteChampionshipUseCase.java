package es.aelb.backendweb.application.championship;

import es.aelb.backendweb.domain.championship.Championship;
import es.aelb.backendweb.domain.championship.ChampionshipRepository;
import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;

public class DeleteChampionshipUseCase {

    private final ChampionshipRepository championshipRepository;

    public DeleteChampionshipUseCase(ChampionshipRepository championshipRepository) {
        this.championshipRepository = championshipRepository;
    }

    public void execute(String championshipId) {
        championshipRepository.findById(ChampionshipId.of(championshipId))
                .orElseThrow(() -> new Championship.NotFoundException(championshipId));
        championshipRepository.delete(ChampionshipId.of(championshipId));
    }
}
