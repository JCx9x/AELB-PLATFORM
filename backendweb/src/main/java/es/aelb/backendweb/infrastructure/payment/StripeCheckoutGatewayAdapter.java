package es.aelb.backendweb.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.domain.quota.Quota;
import es.aelb.backendweb.domain.quota.StripeCheckoutGateway;
import es.aelb.backendweb.domain.quota.StripeCheckoutSession;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.infrastructure.config.StripeProperties;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adaptador HTTP de Stripe Checkout. La clave secreta nunca sale del backend. */
public class StripeCheckoutGatewayAdapter implements StripeCheckoutGateway {
    private static final URI CHECKOUT_SESSIONS_URL = URI.create("https://api.stripe.com/v1/checkout/sessions");
    private final StripeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StripeCheckoutGatewayAdapter(StripeProperties properties, ObjectMapper objectMapper) { this.properties = properties; this.objectMapper = objectMapper; }

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
        try {
            HttpRequest request = HttpRequest.newBuilder(CHECKOUT_SESSIONS_URL)
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Idempotency-Key", "annual-quota-" + quota.getId())
                    .POST(HttpRequest.BodyPublishers.ofString(formEncode(form))).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() / 100 != 2 || body.path("url").asText().isBlank()) throw new StripeRequestException();
            return new StripeCheckoutSession(body.path("id").asText(), body.path("url").asText());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new StripeRequestException();
        } catch (Exception e) { throw new StripeRequestException(); }
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
        try {
            HttpRequest request = HttpRequest.newBuilder(CHECKOUT_SESSIONS_URL)
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Idempotency-Key", "registration-checkout-" + checkout.getId())
                    .POST(HttpRequest.BodyPublishers.ofString(formEncode(form))).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() / 100 != 2 || body.path("url").asText().isBlank()) throw new StripeRequestException();
            return new StripeCheckoutSession(body.path("id").asText(), body.path("url").asText());
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new StripeRequestException(); }
        catch (Exception e) { throw new StripeRequestException(); }
    }
    private String formEncode(Map<String, String> form) { return form.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&")); }
    public static final class StripeNotConfiguredException extends DomainException { public StripeNotConfiguredException() { super("Stripe no está configurado. Añade STRIPE_SECRET_KEY en la configuración del backend."); } }
    public static final class StripeRequestException extends DomainException { public StripeRequestException() { super("No se ha podido iniciar el pago con Stripe. Inténtalo de nuevo."); } }
}
