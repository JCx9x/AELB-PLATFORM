package es.aelb.backendweb.domain.championship;

import es.aelb.backendweb.domain.championship.valueobject.ChampionshipId;
import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Campeonato: agregado raíz.
 * Gestiona sus categorías y controla su visibilidad.
 * La fecha de inscripción no puede superar la fecha del evento.
 */
public class Championship extends AggregateRoot<ChampionshipId> {

    private       String       name;
    private       String       location;
    private       LocalDate    eventDate;
    private       LocalDate    registrationDeadline;
    private       BigDecimal   price;
    private       boolean      visible;
    private       String       imageUrl;
    private       String       description;
    private       boolean      requiresCurrentQuota;
    private final UserId       createdBy;
    private final Set<String>  categoryIds;
    private final LocalDateTime createdAt;
    private       LocalDateTime updatedAt;

    private Championship(
            ChampionshipId id,
            String name, String location,
            LocalDate eventDate, LocalDate registrationDeadline,
            BigDecimal price, UserId createdBy,
            LocalDateTime createdAt
    ) {
        super(id);
        validateDates(registrationDeadline, eventDate);
        this.name                 = name;
        this.location             = location;
        this.eventDate            = eventDate;
        this.registrationDeadline = registrationDeadline;
        this.price                = price;
        this.createdBy            = createdBy;
        this.visible              = false;
        this.categoryIds          = new HashSet<>();
        this.createdAt            = createdAt;
        this.updatedAt            = createdAt;
    }

    public static Championship create(
            String name, String location,
            LocalDate eventDate, LocalDate registrationDeadline,
            BigDecimal price, UserId createdBy
    ) {
        validateName(name);
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        return new Championship(
                ChampionshipId.generate(), name, location,
                eventDate, registrationDeadline, price, createdBy,
                LocalDateTime.now()
        );
    }

    /** Reconstituye desde persistencia. */
    public static Championship reconstitute(
            ChampionshipId id, String name, String location,
            LocalDate eventDate, LocalDate registrationDeadline,
            BigDecimal price, boolean visible, String imageUrl,
            String description, boolean requiresCurrentQuota, UserId createdBy, Set<String> categoryIds,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        Championship c = new Championship(id, name, location, eventDate,
                registrationDeadline, price, createdBy, createdAt);
        c.visible      = visible;
        c.imageUrl     = imageUrl;
        c.description  = description;
        c.requiresCurrentQuota = requiresCurrentQuota;
        c.updatedAt    = updatedAt;
        if (categoryIds != null) c.categoryIds.addAll(categoryIds);
        return c;
    }

    public void publish() {
        if (categoryIds.isEmpty()) {
            throw new ChampionshipWithoutCategoriesException(getId().value());
        }
        this.visible   = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.visible   = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void addCategory(String categoryId) {
        categoryIds.add(categoryId);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeCategory(String categoryId) {
        categoryIds.remove(categoryId);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRegistrationOpen() {
        return visible && !LocalDate.now().isAfter(registrationDeadline);
    }

    public boolean hasCategory(String categoryId) {
        return categoryIds.contains(categoryId);
    }

    public void updateDetails(
            String name, String location,
            LocalDate eventDate, LocalDate registrationDeadline,
            BigDecimal price, String imageUrl, String description, boolean requiresCurrentQuota
    ) {
        validateName(name);
        validateDates(registrationDeadline, eventDate);
        this.name                 = name;
        this.location             = location;
        this.eventDate            = eventDate;
        this.registrationDeadline = registrationDeadline;
        this.price                = price;
        this.imageUrl             = imageUrl;
        this.description          = description;
        this.requiresCurrentQuota = requiresCurrentQuota;
        this.updatedAt            = LocalDateTime.now();
    }

    private static void validateDates(LocalDate deadline, LocalDate event) {
        if (deadline.isAfter(event)) {
            throw new IllegalArgumentException(
                    "La fecha límite de inscripción no puede ser posterior al evento"
            );
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del campeonato no puede estar vacío");
        }
    }

    // --- Getters ---
    public String         getName()                 { return name; }
    public String         getLocation()             { return location; }
    public LocalDate      getEventDate()            { return eventDate; }
    public LocalDate      getRegistrationDeadline() { return registrationDeadline; }
    public BigDecimal     getPrice()                { return price; }
    public boolean        isVisible()               { return visible; }
    public String         getImageUrl()             { return imageUrl; }
    public String         getDescription()          { return description; }
    public boolean        requiresCurrentQuota()    { return requiresCurrentQuota; }
    public UserId         getCreatedBy()            { return createdBy; }
    public Set<String>    getCategoryIds()          { return Collections.unmodifiableSet(categoryIds); }
    public LocalDateTime  getCreatedAt()            { return createdAt; }
    public LocalDateTime  getUpdatedAt()            { return updatedAt; }

    // --- Excepciones de dominio ---

    public static final class ChampionshipWithoutCategoriesException extends DomainException {
        public ChampionshipWithoutCategoriesException(String id) {
            super("El campeonato " + id + " debe tener al menos una categoría para publicarse");
        }
    }

    public static final class RegistrationClosedException extends DomainException {
        public RegistrationClosedException(String id) {
            super("Las inscripciones para el campeonato " + id + " están cerradas");
        }
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Campeonato no encontrado: " + id);
        }
    }
}
