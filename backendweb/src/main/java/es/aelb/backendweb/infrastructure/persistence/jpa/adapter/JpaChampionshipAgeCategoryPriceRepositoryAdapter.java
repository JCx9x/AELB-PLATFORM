package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.pricing.ChampionshipAgeCategoryPrice;
import es.aelb.backendweb.domain.pricing.ChampionshipAgeCategoryPriceRepository;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.ChampionshipAgeCategoryPriceJpaEntity;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringChampionshipAgeCategoryPriceJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class JpaChampionshipAgeCategoryPriceRepositoryAdapter implements ChampionshipAgeCategoryPriceRepository {
    private final SpringChampionshipAgeCategoryPriceJpaRepository repository;
    public JpaChampionshipAgeCategoryPriceRepositoryAdapter(SpringChampionshipAgeCategoryPriceJpaRepository repository) { this.repository = repository; }
    public List<ChampionshipAgeCategoryPrice> findByChampionshipId(ChampionshipId id) {
        return repository.findByChampionshipId(id.value()).stream().map(entity -> new ChampionshipAgeCategoryPrice(entity.getAgeCategory(), entity.getPricePerArm(), entity.getCombinationPrice())).toList();
    }
    @Transactional
    public void replaceForChampionship(ChampionshipId id, List<ChampionshipAgeCategoryPrice> prices) {
        repository.deleteByChampionshipId(id.value());
        repository.saveAll(prices.stream().map(price -> {
            ChampionshipAgeCategoryPriceJpaEntity entity = new ChampionshipAgeCategoryPriceJpaEntity();
            entity.setChampionshipId(id.value()); entity.setAgeCategory(price.ageCategory());
            entity.setPricePerArm(price.pricePerArm()); entity.setCombinationPrice(price.combinationPrice());
            return entity;
        }).toList());
    }
}
