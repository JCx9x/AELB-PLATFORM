package es.aelb.backendweb.domain.quota;

import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cuota anual de un usuario.
 * Invariante clave: solo puede existir una cuota por (usuario, año).
 * Esta regla se aplica en el caso de uso antes de llamar al repositorio.
 */
public class Quota extends AggregateRoot<String> {

    public enum QuotaPaymentStatus { PENDING, PAID }
    /** Origen auditable de una cuota pagada. LEGACY identifica pagos previos a esta trazabilidad. */
    public enum PaymentSource { STRIPE, MANUAL, LEGACY }

    private final UserId             userId;
    private final int                year;
    private final AgeCategory        ageCategory;
    private final BigDecimal         amount;
    private       QuotaPaymentStatus status;
    private       LocalDate          paymentDate;
    private       LocalDateTime      paidAt;
    private       PaymentSource      paymentSource;
    private       UserId             paidByUserId;
    private       String             paymentReference;
    private final LocalDateTime      createdAt;
    private       LocalDateTime      updatedAt;

    private Quota(String id, UserId userId, int year, AgeCategory ageCategory, BigDecimal amount, LocalDateTime createdAt) {
        super(id);
        this.userId    = userId;
        this.year      = year;
        this.ageCategory = ageCategory;
        this.amount    = amount;
        this.status    = QuotaPaymentStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Quota create(UserId userId, int year, AgeCategory ageCategory, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El importe de la cuota no puede ser negativo");
        }
        int currentYear = LocalDate.now().getYear();
        if (year < 2020 || year > currentYear + 1) {
            throw new IllegalArgumentException("Año de cuota inválido: " + year);
        }
        return new Quota(UUID.randomUUID().toString(), userId, year, ageCategory, amount, LocalDateTime.now());
    }

    public static Quota reconstitute(
            String id, UserId userId, int year, AgeCategory ageCategory, BigDecimal amount,
            QuotaPaymentStatus status, LocalDate paymentDate, LocalDateTime paidAt,
            PaymentSource paymentSource, UserId paidByUserId, String paymentReference,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        Quota q = new Quota(id, userId, year, ageCategory, amount, createdAt);
        q.status      = status;
        q.paymentDate = paymentDate;
        q.paidAt = paidAt;
        q.paymentSource = paymentSource;
        q.paidByUserId = paidByUserId;
        q.paymentReference = paymentReference;
        q.updatedAt   = updatedAt;
        return q;
    }

    public void markAsPaidManually(UserId markedByUserId) {
        markAsPaid(PaymentSource.MANUAL, markedByUserId, null);
    }

    public void markAsPaidByStripe(String checkoutSessionId) {
        markAsPaid(PaymentSource.STRIPE, null, checkoutSessionId);
    }

    private void markAsPaid(PaymentSource source, UserId markedByUserId, String reference) {
        if (status == QuotaPaymentStatus.PAID) {
            throw new QuotaAlreadyPaidException(userId.value(), year);
        }
        this.status      = QuotaPaymentStatus.PAID;
        this.paymentDate = LocalDate.now();
        this.paidAt      = LocalDateTime.now();
        this.paymentSource = source;
        this.paidByUserId = markedByUserId;
        this.paymentReference = reference;
        this.updatedAt   = LocalDateTime.now();
    }

    public boolean isPaid() { return status == QuotaPaymentStatus.PAID; }

    // --- Getters ---
    public UserId             getUserId()     { return userId; }
    public int                getYear()       { return year; }
    public AgeCategory        getAgeCategory(){ return ageCategory; }
    public BigDecimal         getAmount()     { return amount; }
    public QuotaPaymentStatus getStatus()     { return status; }
    public LocalDate          getPaymentDate(){ return paymentDate; }
    public LocalDateTime      getPaidAt()     { return paidAt; }
    public PaymentSource      getPaymentSource() { return paymentSource; }
    public UserId             getPaidByUserId() { return paidByUserId; }
    public String             getPaymentReference() { return paymentReference; }
    public LocalDateTime      getCreatedAt()  { return createdAt; }
    public LocalDateTime      getUpdatedAt()  { return updatedAt; }

    public static final class QuotaAlreadyPaidException extends DomainException {
        public QuotaAlreadyPaidException(String userId, int year) {
            super("La cuota del año " + year + " del usuario " + userId + " ya está pagada");
        }
    }

    public static final class DuplicateQuotaException extends DomainException {
        public DuplicateQuotaException(String userId, int year) {
            super("Ya existe una cuota para el usuario " + userId + " en el año " + year);
        }
    }
}
