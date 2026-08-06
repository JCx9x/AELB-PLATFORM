package es.aelb.backendweb.infrastructure.persistence.jpa.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.domain.pricing.checkout.CheckoutLineItem;
import es.aelb.backendweb.domain.pricing.checkout.RegistrationCheckout;
import es.aelb.backendweb.infrastructure.persistence.jpa.entity.RegistrationCheckoutJpaEntity;

import java.math.BigDecimal;
import java.util.*;

public final class RegistrationCheckoutJpaMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RegistrationCheckoutJpaMapper() {}

    public static RegistrationCheckoutJpaEntity toJpa(RegistrationCheckout domain) {
        RegistrationCheckoutJpaEntity entity = new RegistrationCheckoutJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setChampionshipId(domain.getChampionshipId());
        entity.setLineItemsJson(serializeLineItems(domain.getLineItems()));
        entity.setTotal(domain.getTotal());
        entity.setCurrency(domain.getCurrency());
        entity.setMetadataJson(serializeMetadata(domain.getMetadata()));
        entity.setBasketKey(domain.getBasketKey());
        entity.setStatus(domain.getStatus());
        entity.setStripeSessionId(domain.getStripeSessionId());
        entity.setPaymentReference(domain.getPaymentReference());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static RegistrationCheckout toDomain(RegistrationCheckoutJpaEntity entity) {
        return RegistrationCheckout.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getChampionshipId(),
                deserializeLineItems(entity.getLineItemsJson()),
                entity.getTotal(),
                entity.getCurrency(),
                deserializeMetadata(entity.getMetadataJson()),
                entity.getBasketKey(),
                entity.getStatus(),
                entity.getStripeSessionId(),
                entity.getPaymentReference(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String serializeLineItems(List<CheckoutLineItem> items) {
        try {
            List<Map<String, String>> list = items.stream()
                    .map(li -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("concept", li.concept());
                        m.put("amount",  li.amount().toPlainString());
                        m.put("ruleId",  li.ruleId() != null ? li.ruleId() : "");
                        return m;
                    }).toList();
            return MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando lineItems", e);
        }
    }

    private static List<CheckoutLineItem> deserializeLineItems(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptyList();
        try {
            List<Map<String, String>> list = MAPPER.readValue(json, new TypeReference<>() {});
            return list.stream()
                    .map(m -> CheckoutLineItem.of(m.get("concept"), new BigDecimal(m.get("amount")), m.get("ruleId")))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error deserializando lineItems: " + json, e);
        }
    }

    private static String serializeMetadata(Map<String, String> meta) {
        if (meta == null || meta.isEmpty()) return "{}";
        try {
            return MAPPER.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando metadata", e);
        }
    }

    private static Map<String, String> deserializeMetadata(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return Collections.emptyMap();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}
