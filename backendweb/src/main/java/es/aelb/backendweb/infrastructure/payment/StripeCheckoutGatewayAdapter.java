package es.aelb.backendweb.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.StripeCheckoutGateway;
import es.aelb.backendweb.domain.quota.StripeCheckoutSession;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.infrastructure.config.StripeProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Adaptador HTTP de Stripe Checkout. La clave secreta nunca sale del backend. */
public class StripeCheckoutGatewayAdapter implements StripeCheckoutGateway {
    private static final URI CHECKOUT_SESSIONS_URL = URI.create("https://api.stripe.com/v1/checkout/sessions");
    private final StripeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final MeterRegistry meterRegistry;
    private final Counter retriesCounter;
    private final Counter circuitRejectedCounter;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile long circuitOpenUntil;

    public StripeCheckoutGatewayAdapter(StripeProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        this.retriesCounter = meterRegistry.counter("stripe.checkout.retries");
        this.circuitRejectedCounter = meterRegistry.counter("stripe.checkout.circuit.rejected");
        Gauge.builder("stripe.checkout.circuit.open", this, adapter -> adapter.isCircuitOpen() ? 1 : 0)
                .description("1 si el circuit breaker de Stripe está abierto, 0 si está cerrado")
                .register(meterRegistry);
    }

    private boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntil;
    }

    @Override
    public StripeCheckoutSession createFor(Quota quota) {
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) throw new StripeNotConfiguredException();
        long cents = quota.getAmount().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mode", "payment");
        form.put("success_url", properties.getSuccessUrl());
        form.put("cancel_url", properties.getCancelUrl());
        form.put("client_reference_id", quota.getId());
        form.put("metadata[quota_id]", quota.getId());
        form.put("metadata[user_id]", quota.getUserId().value());
        form.put("metadata[year]", String.valueOf(quota.getYear()));
        form.put("line_items[0][price_data][currency]", properties.getCurrency());
        form.put("line_items[0][price_data][unit_amount]", String.valueOf(cents));
        form.put("line_items[0][price_data][product_data][name]", "Cuota anual " + quota.getYear() + " - " + quota.getAgeCategory().getLabel());
        form.put("line_items[0][quantity]", "1");
        return sendCreateSession(form, "annual-quota-" + quota.getId());
    }

    @Override
    public StripeCheckoutSession createFor(RegistrationCheckout checkout, String description) {
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) throw new StripeNotConfiguredException();
        long cents = checkout.getTotal().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mode", "payment"); form.put("success_url", properties.getSuccessUrl()); form.put("cancel_url", properties.getCancelUrl());
        form.put("client_reference_id", checkout.getId()); form.put("metadata[registration_checkout_id]", checkout.getId());
        form.put("metadata[registration_id]", checkout.getMetadata().getOrDefault("registrationId", ""));
        form.put("metadata[category_ids]", checkout.getMetadata().getOrDefault("categoryIds", ""));
        form.put("metadata[user_id]", checkout.getUserId()); form.put("metadata[championship_id]", checkout.getChampionshipId());
        form.put("expires_at", String.valueOf(checkout.getExpiresAt().toInstant(ZoneOffset.UTC).getEpochSecond()));
        form.put("line_items[0][price_data][currency]", properties.getCurrency()); form.put("line_items[0][price_data][unit_amount]", String.valueOf(cents));
        form.put("line_items[0][price_data][product_data][name]", description); form.put("line_items[0][quantity]", "1");
        return sendCreateSession(form, "registration-checkout-" + checkout.getId());
    }
    private StripeCheckoutSession sendCreateSession(Map<String, String> form, String idempotencyKey) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";
        try {
            ensureCircuitClosed();
            int attempts = Math.max(0, properties.getMaxRetries()) + 1;
            for (int attempt = 0; attempt < attempts; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(CHECKOUT_SESSIONS_URL)
                            .timeout(java.time.Duration.ofMillis(properties.getRequestTimeoutMs()))
                            .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8)))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("Idempotency-Key", idempotencyKey)
                            .POST(HttpRequest.BodyPublishers.ofString(formEncode(form))).build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() / 100 == 2) {
                        JsonNode body = objectMapper.readTree(response.body());
                        if (!body.path("id").asText().isBlank() && !body.path("url").asText().isBlank()) {
                            consecutiveFailures.set(0);
                            outcome = "success";
                            return new StripeCheckoutSession(body.path("id").asText(), body.path("url").asText());
                        }
                    }
                    if (!isRetryable(response.statusCode()) || attempt == attempts - 1) throw new StripeRequestException();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    recordFailure();
                    throw new StripeRequestException();
                } catch (IOException e) {
                    if (attempt == attempts - 1) { recordFailure(); throw new StripeRequestException(); }
                } catch (StripeRequestException e) {
                    recordFailure();
                    throw e;
                } catch (Exception e) {
                    recordFailure();
                    throw new StripeRequestException();
                }
                retriesCounter.increment();
                backoff(attempt);
            }
            recordFailure();
            throw new StripeRequestException();
        } finally {
            sample.stop(meterRegistry.timer("stripe.checkout.requests", "outcome", outcome));
        }
    }
    private boolean isRetryable(int status) { return status == 429 || status >= 500; }
    private void backoff(int attempt) {
        try { Thread.sleep(Math.min(2000, properties.getRetryBaseDelayMs() * (1L << attempt))); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new StripeRequestException(); }
    }
    private void ensureCircuitClosed() {
        if (isCircuitOpen()) {
            circuitRejectedCounter.increment();
            throw new StripeRequestException();
        }
    }
    private void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= Math.max(1, properties.getCircuitBreakerFailureThreshold())) {
            circuitOpenUntil = System.currentTimeMillis() + Math.max(1000, properties.getCircuitBreakerOpenMs());
        }
    }
    private String formEncode(Map<String, String> form) { return form.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&")); }
    public static final class StripeNotConfiguredException extends DomainException { public StripeNotConfiguredException() { super("Stripe no está configurado. Añade STRIPE_SECRET_KEY en la configuración del backend."); } }
    public static final class StripeRequestException extends DomainException { public StripeRequestException() { super("No se ha podido iniciar el pago con Stripe. Inténtalo de nuevo."); } }
}
