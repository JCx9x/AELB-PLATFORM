package es.aelb.backendweb.application.registration;

import es.aelb.backendweb.domain.pricing.checkout.CheckoutLineItem;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckoutRepository;
import es.aelb.backendweb.domain.registration.Registration;
import es.aelb.backendweb.domain.registration.RegistrationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConfirmStripeChampionshipBasketPaymentUseCaseTest {

    @Test
    void confirmsTheReservationWithoutRevalidatingTheChampionship() {
        String userId = "user-1";
        String championshipId = "championship-1";
        Registration registration = Registration.create(
                es.aelb.backendweb.domain.user.valueobject.UserId.of(userId),
                es.aelb.backendweb.domain.championship.valueobject.ChampionshipId.of(championshipId),
                "category-1", BigDecimal.TEN,
                es.aelb.backendweb.domain.user.valueobject.UserId.of(userId));

        RegistrationCheckout checkout = RegistrationCheckout.create(
                userId, championshipId,
                List.of(CheckoutLineItem.of("Inscripción", BigDecimal.TEN, "category-1")),
                BigDecimal.TEN, "EUR", Map.of("categoryIds", "category-1"), "basket-1");
        checkout.putMetadata("registrationIds", registration.getId());
        checkout.bindStripeSession("cs_test_reserved");

        RegistrationCheckoutRepository checkouts = mock(RegistrationCheckoutRepository.class);
        RegistrationRepository registrations = mock(RegistrationRepository.class);
        when(checkouts.findByIdForUpdate(checkout.getId())).thenReturn(Optional.of(checkout));
        when(registrations.findById(registration.getId())).thenReturn(Optional.of(registration));

        new ConfirmStripeChampionshipBasketPaymentUseCase(checkouts, registrations)
                .execute(checkout.getId(), "cs_test_reserved");

        assertTrue(registration.isPaid());
        verify(registrations).save(registration);
        verify(checkouts).save(checkout);
    }
}
