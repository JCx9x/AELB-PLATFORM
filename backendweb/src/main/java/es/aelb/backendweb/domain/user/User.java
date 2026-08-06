package es.aelb.backendweb.domain.user;

import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.user.valueobject.Dni;
import es.aelb.backendweb.domain.user.valueobject.Email;
import es.aelb.backendweb.domain.user.valueobject.UserId;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agregado raíz de Usuario.
 * El email no es modificable una vez creado (invariante de negocio).
 * El bloqueo impide el acceso sin eliminar datos históricos.
 */
public class User extends AggregateRoot<UserId> {

    private final Email         email;       // inmutable
    private       String        passwordHash;
    private       String        firstName;
    private       String        lastName;
    private final Dni           dni;         // inmutable
    private       String        phone;
    private       String        city;
    private       String        province;
    private       String        nationality;
    private       LocalDate     birthDate;
    private       UserRole      role;
    private       boolean       blocked;
    private       String        teamId;
    private       int           tokenVersion;
    private final LocalDateTime createdAt;
    private       LocalDateTime updatedAt;

    private User(
            UserId id,
            Email email,
            String passwordHash,
            String firstName,
            String lastName,
            Dni dni,
            String phone,
            String city,
            String province,
            String nationality,
            LocalDate birthDate,
            UserRole role,
            int tokenVersion,
            LocalDateTime createdAt
    ) {
        super(id);
        this.email        = email;
        this.passwordHash = passwordHash;
        this.firstName    = firstName;
        this.lastName     = lastName;
        this.dni          = dni;
        this.phone        = phone;
        this.city          = city;
        this.province      = province;
        this.nationality   = nationality;
        this.birthDate    = birthDate;
        this.role         = role;
        this.blocked      = false;
        this.tokenVersion = tokenVersion;
        this.createdAt    = createdAt;
        this.updatedAt    = createdAt;
    }

    public static User create(
            Email email,
            String passwordHash,
            String firstName,
            String lastName,
            Dni dni,
            String phone,
            String city,
            String province,
            String nationality,
            LocalDate birthDate
    ) {
        validateName(firstName, "nombre");
        validateName(lastName, "apellido");
        return new User(
                UserId.generate(), email, passwordHash,
                firstName, lastName, dni, phone, city, province, nationality, birthDate,
                UserRole.USER, 0, LocalDateTime.now()
        );
    }

    /** Reconstituye desde persistencia sin regenerar ID ni timestamps. */
    public static User reconstitute(
            UserId id, Email email, String passwordHash,
            String firstName, String lastName, Dni dni,
            String phone, String city, String province, String nationality,
            LocalDate birthDate, UserRole role,
            boolean blocked, String teamId, int tokenVersion,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        User user = new User(id, email, passwordHash, firstName, lastName,
                dni, phone, city, province, nationality, birthDate, role, tokenVersion, createdAt);
        user.blocked   = blocked;
        user.teamId    = teamId;
        user.updatedAt = updatedAt;
        return user;
    }

    public void block() {
        if (this.blocked) throw new UserAlreadyBlockedException(getId().value());
        this.blocked   = true;
        this.tokenVersion++;
        this.updatedAt = LocalDateTime.now();
    }

    public void unblock() {
        this.blocked   = false;
        this.tokenVersion++;
        this.updatedAt = LocalDateTime.now();
    }

    public void promoteToGestor() {
        this.role      = UserRole.GESTOR;
        this.tokenVersion++;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String firstName, String lastName, String phone, LocalDate birthDate) {
        validateName(firstName, "nombre");
        validateName(lastName, "apellido");
        this.firstName = firstName;
        this.lastName  = lastName;
        this.phone     = phone;
        this.birthDate = birthDate;
        this.updatedAt = LocalDateTime.now();
    }

    /** Invalida sesiones activas: cambiar la contraseña debe cerrar cualquier sesión robada. */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.tokenVersion++;
        this.updatedAt    = LocalDateTime.now();
    }

    public void changeRole(UserRole newRole) {
        this.role      = newRole;
        this.tokenVersion++;
        this.updatedAt = LocalDateTime.now();
    }

    public void setBlocked(boolean blocked) {
        this.blocked   = blocked;
        this.tokenVersion++;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignTeam(String teamId) {
        this.teamId    = teamId;
        this.updatedAt = LocalDateTime.now();
    }

    public void leaveTeam() {
        this.teamId    = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return !blocked;
    }

    private static void validateName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El " + field + " no puede estar vacío");
        }
    }

    // --- Getters (sin setters: mutación controlada por métodos de dominio) ---

    public Email         getEmail()        { return email; }
    public String        getPasswordHash() { return passwordHash; }
    public String        getFirstName()    { return firstName; }
    public String        getLastName()     { return lastName; }
    public Dni           getDni()          { return dni; }
    public String        getPhone()        { return phone; }
    public String        getCity()         { return city; }
    public String        getProvince()     { return province; }
    public String        getNationality()  { return nationality; }
    public LocalDate     getBirthDate()    { return birthDate; }
    public UserRole      getRole()         { return role; }
    public boolean       isBlocked()       { return blocked; }
    public String        getTeamId()       { return teamId; }
    public int           getTokenVersion() { return tokenVersion; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // --- Excepciones de dominio anidadas ---

    public static final class UserAlreadyBlockedException extends DomainException {
        public UserAlreadyBlockedException(String userId) {
            super("El usuario " + userId + " ya está bloqueado");
        }
    }

    public static final class UserBlockedException extends DomainException {
        public UserBlockedException(String userId) {
            super("El usuario " + userId + " está bloqueado y no puede realizar esta acción");
        }
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Usuario no encontrado: " + id);
        }
    }
}
