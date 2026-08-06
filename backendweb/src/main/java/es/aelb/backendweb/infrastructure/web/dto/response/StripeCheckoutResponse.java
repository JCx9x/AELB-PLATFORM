package es.aelb.backendweb.infrastructure.web.dto.response;
import es.aelb.backendweb.domain.quota.StripeCheckoutSession;
public record StripeCheckoutResponse(String sessionId, String checkoutUrl) {
    public static StripeCheckoutResponse from(StripeCheckoutSession session) { return new StripeCheckoutResponse(session.id(), session.url()); }
}
