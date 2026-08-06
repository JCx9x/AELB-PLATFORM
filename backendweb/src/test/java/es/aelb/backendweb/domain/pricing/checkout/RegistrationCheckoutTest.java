package es.aelb.backendweb.domain.pricing.checkout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationCheckoutTest {

    @Test
    void createsCheckoutWithStripeCompatibleExpiryAndReleasesBasketAfterPayment() {
        RegistrationCheckout checkout = checkout("basket-key");

        assertEquals(Duration.ofMinutes(35), Duration.between(checkout.getCreatedAt(), checkout.getExpiresAt()));
        assertEquals("basket-key", checkout.getBasketKey());

        checkout.bindStripeSession("cs_test_expected");
        checkout.confirmPayment("cs_test_expected");

        assertTrue(checkout.isPaid());
        assertNull(checkout.getBasketKey());
        assertEquals("cs_test_expected", checkout.getPaymentReference());
    }

    @Test
    void onlyThePersistedStripeSessionCanConfirmTheCheckout() {
        RegistrationCheckout checkout = checkout("basket-key");
        checkout.bindStripeSession("cs_test_expected");

        assertDoesNotThrow(() -> checkout.validateStripeSession("cs_test_expected"));
        assertThrows(RegistrationCheckout.StripeSessionMismatchException.class,
                () -> checkout.validateStripeSession("cs_test_other"));
        assertThrows(RegistrationCheckout.StripeSessionMismatchException.class,
                () -> checkout.bindStripeSession("cs_test_other"));
    }

    @Test
    void verifiedStripePaymentCanBeRecordedAfterWebhookDeliveryDelay() {
        LocalDateTime now = LocalDateTime.now();
        RegistrationCheckout expired = RegistrationCheckout.reconstitute(
                "checkout", "user", "championship", List.of(), BigDecimal.TEN, "EUR", Map.of(), "basket",
                CheckoutStatus.PENDING_PAYMENT, "cs_test_expected", null,
                now.minusMinutes(1), now.minusMinutes(40), now.minusMinutes(1));

        expired.validateStripeSession("cs_test_expected");
        expired.confirmPayment("cs_test_expected");

        assertTrue(expired.isPaid());
    }

    private RegistrationCheckout checkout(String basketKey) {
        return RegistrationCheckout.create("user", "championship",
                List.of(CheckoutLineItem.of("Inscripción", BigDecimal.TEN, "rule")),
                BigDecimal.TEN, "EUR", Map.of("categoryIds", "category"), basketKey);
    }
}
