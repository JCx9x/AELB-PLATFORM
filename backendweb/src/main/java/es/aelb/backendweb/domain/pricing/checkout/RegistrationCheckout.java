package es.aelb.backendweb.domain.pricing.checkout;

import es.aelb.backendweb.domain.shared.exception.DomainException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Checkout de inscripción: objeto que agrupa el precio calculado y los datos necesarios
 * para iniciar el proceso de pago y confirmar las inscripciones tras el webhook.
 *
 * Ciclo de vida:
 *   PENDING_PAYMENT → PAID (tras webhook exitoso)
 *   PENDING_PAYMENT → EXPIRED (tras TTL sin pago)
 *   PENDING_PAYMENT → CANCELLED (cancelado por el usuario)
 */
public final class RegistrationCheckout {

    private static final Duration CHECKOUT_TTL = Duration.ofMinutes(35);

    private final String                  id;
    private final String                  userId;
    private final String                  championshipId;
    private final List<CheckoutLineItem>  lineItems;
    private final BigDecimal              total;
    private final String                  currency;
    private final Map<String, String>     metadata;
    /** Present only while this basket reserves a payment attempt; null once released. */
    private       String                  basketKey;
    private       CheckoutStatus          status;
    private       String                  stripeSessionId;
    private       String                  paymentReference;
    private final LocalDateTime           expiresAt;
    private final LocalDateTime           createdAt;
    private       LocalDateTime           updatedAt;

    private RegistrationCheckout(
            String id, String userId, String championshipId,
            List<CheckoutLineItem> lineItems, BigDecimal total, String currency,
            Map<String, String> metadata, String basketKey, CheckoutStatus status,
            String stripeSessionId, String paymentReference, LocalDateTime expiresAt,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this.id               = id;
        this.userId           = userId;
        this.championshipId   = championshipId;
        this.lineItems        = List.copyOf(lineItems);
        this.total            = total;
        this.currency         = currency;
        this.metadata         = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
        this.basketKey        = basketKey;
        this.status           = status;
        this.stripeSessionId  = stripeSessionId;
        this.paymentReference = paymentReference;
        this.expiresAt        = expiresAt;
        this.createdAt        = createdAt;
        this.updatedAt        = updatedAt;
    }

    /**
     * Crea un checkout pendiente. Stripe Checkout exige una caducidad mínima de 30 minutos;
     * usamos 35 para que la sesión de Stripe y este registro expiren exactamente a la vez.
     */
    public static RegistrationCheckout create(
            String userId, String championshipId,
            List<CheckoutLineItem> lineItems, BigDecimal total, String currency,
            Map<String, String> metadata, String basketKey
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new RegistrationCheckout(
                UUID.randomUUID().toString(),
                userId, championshipId,
                lineItems, total, currency, metadata, basketKey,
                CheckoutStatus.PENDING_PAYMENT,
                null, null,
                now.plus(CHECKOUT_TTL), now, now
        );
    }

    public static RegistrationCheckout reconstitute(
            String id, String userId, String championshipId,
            List<CheckoutLineItem> lineItems, BigDecimal total, String currency,
            Map<String, String> metadata, String basketKey, CheckoutStatus status,
            String stripeSessionId, String paymentReference, LocalDateTime expiresAt,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        return new RegistrationCheckout(id, userId, championshipId,
                lineItems, total, currency, metadata, basketKey, status,
                stripeSessionId, paymentReference, expiresAt, createdAt, updatedAt);
    }

    /** Links this local payment intent to exactly one Stripe Checkout Session. */
    public void bindStripeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("La sesión de Stripe es obligatoria");
        }
        if (stripeSessionId != null && !stripeSessionId.equals(sessionId)) {
            throw new StripeSessionMismatchException(id);
        }
        this.stripeSessionId = sessionId;
        this.updatedAt = LocalDateTime.now();
    }

    public void validateStripeSession(String sessionId) {
        if (stripeSessionId == null || !stripeSessionId.equals(sessionId)) {
            throw new StripeSessionMismatchException(id);
        }
    }

    /**
     * Confirma el pago recibido vía webhook.
     * Guarda la referencia del proveedor de pagos.
     */
    public void confirmPayment(String paymentReference) {
        if (status != CheckoutStatus.PENDING_PAYMENT)
            throw new CheckoutNotPayableException(id, status);
        // La sesión Stripe tiene la misma expiración. Un webhook firmado puede llegar unos
        // segundos tarde, después de que Stripe haya cobrado correctamente, y debe poder
        // materializar el pago en vez de dejar un cobro huérfano.
        this.paymentReference = paymentReference;
        this.status           = CheckoutStatus.PAID;
        this.basketKey        = null;
        this.updatedAt        = LocalDateTime.now();
    }

    public void expire() {
        if (status == CheckoutStatus.PENDING_PAYMENT) {
            this.status    = CheckoutStatus.EXPIRED;
            this.basketKey = null;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void cancel() {
        if (status == CheckoutStatus.PAID)
            throw new CannotCancelPaidCheckoutException(id);
        this.status    = CheckoutStatus.CANCELLED;
        this.basketKey = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPaid()            { return status == CheckoutStatus.PAID; }
    public boolean isPendingPayment()  { return status == CheckoutStatus.PENDING_PAYMENT; }
    public boolean isExpired()         { return LocalDateTime.now().isAfter(expiresAt); }

    public String                 getId()               { return id; }
    public String                 getUserId()           { return userId; }
    public String                 getChampionshipId()   { return championshipId; }
    public List<CheckoutLineItem> getLineItems()        { return lineItems; }
    public BigDecimal             getTotal()            { return total; }
    public String                 getCurrency()         { return currency; }
    public Map<String, String>    getMetadata()         { return metadata; }
    public String                 getBasketKey()        { return basketKey; }
    public CheckoutStatus         getStatus()           { return status; }
    public String                 getStripeSessionId()  { return stripeSessionId; }
    public String                 getPaymentReference() { return paymentReference; }
    public LocalDateTime          getExpiresAt()        { return expiresAt; }
    public LocalDateTime          getCreatedAt()        { return createdAt; }
    public LocalDateTime          getUpdatedAt()        { return updatedAt; }

    // ── Excepciones ──────────────────────────────────────────────────────

    public static final class CheckoutNotPayableException extends DomainException {
        public CheckoutNotPayableException(String id, CheckoutStatus status) {
            super("El checkout " + id + " no puede confirmarse: estado actual es " + status);
        }
    }

    public static final class CannotCancelPaidCheckoutException extends DomainException {
        public CannotCancelPaidCheckoutException(String id) {
            super("El checkout " + id + " ya está pagado y no puede cancelarse.");
        }
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Checkout no encontrado: " + id);
        }
    }

    public static final class StripeSessionMismatchException extends DomainException {
        public StripeSessionMismatchException(String id) {
            super("La sesión Stripe no corresponde al checkout " + id);
        }
    }

    public static final class DuplicateActiveBasketException extends DomainException {
        public DuplicateActiveBasketException() {
            super("Ya existe un checkout pendiente para esta inscripción");
        }
    }
}
