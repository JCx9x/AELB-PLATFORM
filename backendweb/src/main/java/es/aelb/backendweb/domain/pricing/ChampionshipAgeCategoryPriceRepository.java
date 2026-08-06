package es.aelb.backendweb.domain.pricing;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;

import java.util.List;

public interface ChampionshipAgeCategoryPriceRepository {
    List<ChampionshipAgeCategoryPrice> findByChampionshipId(ChampionshipId championshipId);
    void replaceForChampionship(ChampionshipId championshipId, List<ChampionshipAgeCategoryPrice> prices);
}
