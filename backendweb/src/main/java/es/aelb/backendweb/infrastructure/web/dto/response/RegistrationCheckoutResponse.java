package es.aelb.backendweb.infrastructure.web.dto.response;

import es.aelb.backendweb.domain.pricing.checkout.CheckoutLineItem;
import es.aelb.backendweb.domain.pricing.checkout.CheckoutStatus;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RegistrationCheckoutResponse(
        String                   checkoutId,
        String                   userId,
        String                   championshipId,
        BigDecimal               total,
        String                   currency,
        CheckoutStatus           status,
        List<LineItemDto>        lineItems,
        Map<String, String>      metadata,
        LocalDateTime            expiresAt,
        LocalDateTime            createdAt
) {
    public record LineItemDto(String concept, BigDecimal amount) {}

    public static RegistrationCheckoutResponse from(RegistrationCheckout checkout) {
        List<LineItemDto> items = checkout.getLineItems().stream()
                .map(li -> new LineItemDto(li.concept(), li.amount()))
                .toList();
        return new RegistrationCheckoutResponse(
                checkout.getId(),
                checkout.getUserId(),
                checkout.getChampionshipId(),
                checkout.getTotal(),
                checkout.getCurrency(),
                checkout.getStatus(),
                items,
                checkout.getMetadata(),
                checkout.getExpiresAt(),
                checkout.getCreatedAt()
        );
    }
}
