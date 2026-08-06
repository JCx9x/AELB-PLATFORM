package es.aelb.backendweb.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.application.quota.ConfirmStripeQuotaPaymentUseCase;
import es.aelb.backendweb.application.registration.ConfirmStripeRegistrationPaymentUseCase;
import es.aelb.backendweb.application.registration.ConfirmStripeChampionshipBasketPaymentUseCase;
import es.aelb.backendweb.infrastructure.payment.StripeWebhookVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Endpoint exclusivamente para eventos firmados por Stripe. */
@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {
    private final StripeWebhookVerifier verifier;
    private final ConfirmStripeQuotaPaymentUseCase confirmPayment;
    private final ConfirmStripeRegistrationPaymentUseCase confirmRegistrationPayment;
    private final ConfirmStripeChampionshipBasketPaymentUseCase confirmBasketPayment;
    private final ObjectMapper objectMapper;
    public StripeWebhookController(StripeWebhookVerifier verifier, ConfirmStripeQuotaPaymentUseCase confirmPayment, ConfirmStripeRegistrationPaymentUseCase confirmRegistrationPayment, ConfirmStripeChampionshipBasketPaymentUseCase confirmBasketPayment, ObjectMapper objectMapper) { this.verifier = verifier; this.confirmPayment = confirmPayment; this.confirmRegistrationPayment = confirmRegistrationPayment; this.confirmBasketPayment = confirmBasketPayment; this.objectMapper = objectMapper; }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (!verifier.isValid(payload, signature)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("type").asText();
            if (!"checkout.session.completed".equals(eventType) && !"checkout.session.async_payment_succeeded".equals(eventType)) return ResponseEntity.ok().build();
            JsonNode session = event.path("data").path("object");
            if (!"paid".equals(session.path("payment_status").asText())) return ResponseEntity.ok().build();
            JsonNode metadata = session.path("metadata");
            String registrationCheckoutId = metadata.path("registration_checkout_id").asText();
            if (!registrationCheckoutId.isBlank()) {
                String categoryIds = metadata.path("category_ids").asText();
                if (!categoryIds.isBlank()) { confirmBasketPayment.execute(registrationCheckoutId, session.path("id").asText()); return ResponseEntity.ok().build(); }
                confirmRegistrationPayment.execute(registrationCheckoutId, session.path("id").asText());
                return ResponseEntity.ok().build();
            }
            String userId = metadata.path("user_id").asText();
            int year = metadata.path("year").asInt(0);
            if (userId.isBlank() || year == 0) return ResponseEntity.badRequest().build();
            confirmPayment.execute(userId, year, session.path("id").asText());
            return ResponseEntity.ok().build();
        } catch (Exception ex) { return ResponseEntity.badRequest().build(); }
    }
}
