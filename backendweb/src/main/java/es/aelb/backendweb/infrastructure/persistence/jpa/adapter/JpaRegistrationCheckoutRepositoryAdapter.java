package es.aelb.backendweb.infrastructure.persistence.jpa.adapter;

import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.infrastructure.persistence.jpa.mapper.RegistrationCheckoutJpaMapper;
import es.aelb.backendweb.infrastructure.persistence.jpa.repository.SpringRegistrationCheckoutJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

@Component
public class JpaRegistrationCheckoutRepositoryAdapter implements RegistrationCheckoutRepository {

    private final SpringRegistrationCheckoutJpaRepository jpaRepository;

    public JpaRegistrationCheckoutRepositoryAdapter(SpringRegistrationCheckoutJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(RegistrationCheckout checkout) {
        try {
            jpaRepository.saveAndFlush(RegistrationCheckoutJpaMapper.toJpa(checkout));
        } catch (DataIntegrityViolationException exception) {
            throw new RegistrationCheckout.DuplicateActiveBasketException();
        }
    }

    @Override
    public Optional<RegistrationCheckout> findById(String id) {
        return jpaRepository.findById(id).map(RegistrationCheckoutJpaMapper::toDomain);
    }

    @Override
    public Optional<RegistrationCheckout> findByIdForUpdate(String id) {
        return jpaRepository.findByIdForUpdate(id).map(RegistrationCheckoutJpaMapper::toDomain);
    }

    @Override
    public Optional<RegistrationCheckout> findByActiveBasketKey(String basketKey) {
        return jpaRepository.findByBasketKey(basketKey)
                .filter(entity -> entity.getStatus() == es.aelb.backendweb.domain.pricing.checkout.CheckoutStatus.PENDING_PAYMENT)
                .map(RegistrationCheckoutJpaMapper::toDomain);
    }

    @Override
    public List<RegistrationCheckout> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(RegistrationCheckoutJpaMapper::toDomain).toList();
    }

    @Override
    public List<RegistrationCheckout> findByChampionshipId(String championshipId) {
        return jpaRepository.findByChampionshipId(championshipId).stream()
                .map(RegistrationCheckoutJpaMapper::toDomain).toList();
    }
}
