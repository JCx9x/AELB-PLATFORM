package es.aelb.backendweb.domain.pricing.checkout;

import java.util.List;
import java.util.Optional;

public interface RegistrationCheckoutRepository {
    void save(RegistrationCheckout checkout);
    Optional<RegistrationCheckout> findById(String id);
    Optional<RegistrationCheckout> findByIdForUpdate(String id);
    Optional<RegistrationCheckout> findByActiveBasketKey(String basketKey);
    List<RegistrationCheckout> findByUserId(String userId);
    List<RegistrationCheckout> findByChampionshipId(String championshipId);
}
