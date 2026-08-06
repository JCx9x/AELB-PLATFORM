package es.aelb.backendweb.domain.pricing.checkout;

public enum CheckoutStatus {
    /** Esperando confirmación de pago del proveedor. */
    PENDING_PAYMENT,
    /** Pago confirmado vía webhook. Las inscripciones quedan registradas. */
    PAID,
    /** Expirado sin pago (pasado el TTL). */
    EXPIRED,
    /** Cancelado manualmente. */
    CANCELLED
}
