package es.aelb.backendweb.domain.registration;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inscripción de un usuario en un campeonato.
 *
 * Invariantes:
 * - Un usuario solo puede estar inscrito una vez por campeonato
 *   (reforzado también por UNIQUE constraint en BD).
 * - Solo GESTOR/ADMIN puede marcar como pagado.
 * - Una inscripción confirmada no puede cancelarse.
 */
public class Registration extends AggregateRoot<String> {

    private final UserId          userId;
    private final ChampionshipId  championshipId;
    private       String          categoryId;
    private final BigDecimal      amount;
    private       PaymentStatus   paymentStatus;
    private       String          notes;
    private final UserId          registeredBy;
    private final LocalDateTime   createdAt;
    private       LocalDateTime   updatedAt;

    private Registration(
            String id,
            UserId userId,
            ChampionshipId championshipId,
            String categoryId, BigDecimal amount,
            UserId registeredBy,
            LocalDateTime createdAt
    ) {
        super(id);
        this.userId          = userId;
        this.championshipId  = championshipId;
        this.categoryId      = categoryId;
        this.amount          = amount;
        this.paymentStatus   = PaymentStatus.PENDING;
        this.registeredBy    = registeredBy;
        this.createdAt       = createdAt;
        this.updatedAt       = createdAt;
    }

    public static Registration create(
            UserId userId,
            ChampionshipId championshipId,
            String categoryId, BigDecimal amount,
            UserId registeredBy
    ) {
        return new Registration(
                UUID.randomUUID().toString(),
                userId, championshipId, categoryId, amount,
                registeredBy, LocalDateTime.now()
        );
    }

    public static Registration reconstitute(
            String id, UserId userId, ChampionshipId championshipId,
            String categoryId, BigDecimal amount, PaymentStatus paymentStatus,
            String notes, UserId registeredBy,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        Registration r = new Registration(id, userId, championshipId,
                categoryId, amount, registeredBy, createdAt);
        r.paymentStatus = paymentStatus;
        r.notes         = notes;
        r.updatedAt     = updatedAt;
        return r;
    }

    public void confirmPayment() {
        if (paymentStatus == PaymentStatus.CANCELLED) {
            throw new CannotModifyCancelledRegistrationException(getId());
        }
        this.paymentStatus = PaymentStatus.PAID;
        this.updatedAt     = LocalDateTime.now();
    }

    public void resetToPending() {
        if (paymentStatus == PaymentStatus.CANCELLED) {
            throw new CannotModifyCancelledRegistrationException(getId());
        }
        this.paymentStatus = PaymentStatus.PENDING;
        this.updatedAt     = LocalDateTime.now();
    }

    public void cancel() {
        if (paymentStatus == PaymentStatus.CANCELLED) {
            throw new CannotModifyCancelledRegistrationException(getId());
        }
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.updatedAt     = LocalDateTime.now();
    }

    public void updateNotes(String notes) {
        if (paymentStatus == PaymentStatus.CANCELLED) {
            throw new CannotModifyCancelledRegistrationException(getId());
        }
        this.notes     = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeCategory(String newCategoryId) {
        if (paymentStatus == PaymentStatus.CANCELLED) {
            throw new CannotModifyCancelledRegistrationException(getId());
        }
        this.categoryId = newCategoryId;
        this.updatedAt  = LocalDateTime.now();
    }

    public boolean isCancelled() { return paymentStatus == PaymentStatus.CANCELLED; }
    public boolean isPaid()      { return paymentStatus == PaymentStatus.PAID; }

    // --- Getters ---
    public UserId         getUserId()         { return userId; }
    public ChampionshipId getChampionshipId() { return championshipId; }
    public String         getCategoryId()     { return categoryId; }
    public BigDecimal     getAmount()         { return amount; }
    public PaymentStatus  getPaymentStatus()  { return paymentStatus; }
    public String         getNotes()          { return notes; }
    public UserId         getRegisteredBy()   { return registeredBy; }
    public LocalDateTime  getCreatedAt()      { return createdAt; }
    public LocalDateTime  getUpdatedAt()      { return updatedAt; }

    // --- Excepciones ---

    public static final class CannotModifyCancelledRegistrationException extends DomainException {
        public CannotModifyCancelledRegistrationException(String id) {
            super("La inscripción " + id + " está cancelada y no puede modificarse");
        }
    }

    public static final class DuplicateRegistrationException extends DomainException {
        public DuplicateRegistrationException(String userId, String championshipId) {
            super("El usuario " + userId + " ya está inscrito en el campeonato " + championshipId);
        }
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) { super("Inscripción no encontrada: " + id); }
    }
}
