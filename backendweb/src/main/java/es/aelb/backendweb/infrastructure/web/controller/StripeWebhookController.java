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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import es.aelb.backendweb.infrastructure.observability.ObservabilityMetrics;

/** Endpoint exclusivamente para eventos firmados por Stripe. */
@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private final StripeWebhookVerifier verifier;
    private final ConfirmStripeQuotaPaymentUseCase confirmPayment;
    private final ConfirmStripeRegistrationPaymentUseCase confirmRegistrationPayment;
    private final ConfirmStripeChampionshipBasketPaymentUseCase confirmBasketPayment;
    private final ObjectMapper objectMapper;
    private final ObservabilityMetrics metrics;
    public StripeWebhookController(StripeWebhookVerifier verifier, ConfirmStripeQuotaPaymentUseCase confirmPayment, ConfirmStripeRegistrationPaymentUseCase confirmRegistrationPayment, ConfirmStripeChampionshipBasketPaymentUseCase confirmBasketPayment, ObjectMapper objectMapper, ObservabilityMetrics metrics) { this.verifier = verifier; this.confirmPayment = confirmPayment; this.confirmRegistrationPayment = confirmRegistrationPayment; this.confirmBasketPayment = confirmBasketPayment; this.objectMapper = objectMapper; this.metrics = metrics; }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        long started = System.nanoTime();
        if (!verifier.isValid(payload, signature)) {
            metrics.stripeWebhook("signature_invalid");
            log.warn("event=STRIPE_SIGNATURE_INVALID durationMs={}", elapsed(started));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventId = event.path("id").asText("");
            String eventType = event.path("type").asText();
            log.info("event=STRIPE_WEBHOOK_RECEIVED stripeEventId={} eventType={}", eventId, eventType);
            if (!"checkout.session.completed".equals(eventType) && !"checkout.session.async_payment_succeeded".equals(eventType)) {
                metrics.stripeWebhook("unsupported");
                log.info("event=STRIPE_WEBHOOK_IGNORED stripeEventId={} eventType={} durationMs={}", eventId, eventType, elapsed(started));
                return ResponseEntity.ok().build();
            }
            JsonNode session = event.path("data").path("object");
            String sessionId = session.path("id").asText("");
            if (!"paid".equals(session.path("payment_status").asText())) {
                metrics.stripeWebhook("not_paid");
                log.info("event=STRIPE_WEBHOOK_IGNORED stripeEventId={} stripeSessionId={} reason=not_paid durationMs={}", eventId, sessionId, elapsed(started));
                return ResponseEntity.ok().build();
            }
            JsonNode metadata = session.path("metadata");
            String registrationCheckoutId = metadata.path("registration_checkout_id").asText();
            if (!registrationCheckoutId.isBlank()) {
                String categoryIds = metadata.path("category_ids").asText();
                if (!categoryIds.isBlank()) { confirmBasketPayment.execute(registrationCheckoutId, sessionId); }
                else confirmRegistrationPayment.execute(registrationCheckoutId, sessionId);
                metrics.stripeWebhook("processed");
                metrics.payment("confirmed");
                log.info("event=STRIPE_WEBHOOK_PROCESSED stripeEventId={} stripeSessionId={} eventType={} durationMs={}", eventId, sessionId, eventType, elapsed(started));
                return ResponseEntity.ok().build();
            }
            String userId = metadata.path("user_id").asText();
            int year = metadata.path("year").asInt(0);
            long amountTotalInCents = session.path("amount_total").asLong(-1);
            if (userId.isBlank() || year == 0 || sessionId.isBlank() || amountTotalInCents < 0) {
                metrics.stripeWebhook("invalid_metadata");
                log.warn("event=STRIPE_WEBHOOK_INVALID_METADATA stripeEventId={} stripeSessionId={} durationMs={}", eventId, sessionId, elapsed(started));
                return ResponseEntity.badRequest().build();
            }
            confirmPayment.execute(userId, year, sessionId, amountTotalInCents);
            metrics.stripeWebhook("processed");
            metrics.payment("confirmed");
            log.info("event=STRIPE_WEBHOOK_PROCESSED stripeEventId={} stripeSessionId={} eventType={} durationMs={}", eventId, sessionId, eventType, elapsed(started));
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            metrics.stripeWebhook("failed");
            log.error("event=STRIPE_WEBHOOK_FAILED durationMs={} exceptionType={}", elapsed(started), ex.getClass().getName(), ex);
            return ResponseEntity.badRequest().build();
        }
    }

    private static long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000; }
}
