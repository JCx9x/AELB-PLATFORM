package es.aelb.backendweb.domain.team;

import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.team.valueobject.TeamId;

public class Team extends AggregateRoot<TeamId> {

    private String name;
    private String responsibleName;
    private String responsibleLastName;
    private String phone;
    private String province;
    private String locality;
    private String logoBase64;
    private String logoType;

    private Team(TeamId id, String name, String responsibleName, String responsibleLastName,
                 String phone, String province, String locality,
                 String logoBase64, String logoType) {
        super(id);
        this.name                = name;
        this.responsibleName     = responsibleName;
        this.responsibleLastName = responsibleLastName;
        this.phone               = phone;
        this.province            = province;
        this.locality            = locality;
        this.logoBase64          = logoBase64;
        this.logoType            = logoType;
    }

    public static Team create(String name, String responsibleName, String responsibleLastName,
                              String phone, String province, String locality,
                              String logoBase64, String logoType) {
        validateName(name);
        return new Team(TeamId.generate(), name, responsibleName, responsibleLastName,
                phone, province, locality, logoBase64, logoType);
    }

    public static Team reconstitute(TeamId id, String name, String responsibleName,
                                    String responsibleLastName, String phone,
                                    String province, String locality,
                                    String logoBase64, String logoType) {
        return new Team(id, name, responsibleName, responsibleLastName, phone, province, locality,
                logoBase64, logoType);
    }

    public void update(String name, String responsibleName, String responsibleLastName,
                       String phone, String province, String locality,
                       String logoBase64, String logoType) {
        validateName(name);
        this.name                = name;
        this.responsibleName     = responsibleName;
        this.responsibleLastName = responsibleLastName;
        this.phone               = phone;
        this.province            = province;
        this.locality            = locality;
        if (logoBase64 != null) {
            this.logoBase64 = logoBase64;
            this.logoType   = logoType;
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo no puede estar vacío");
        }
    }

    public String  getName()                { return name; }
    public String  getResponsibleName()     { return responsibleName; }
    public String  getResponsibleLastName() { return responsibleLastName; }
    public String  getPhone()               { return phone; }
    public String  getProvince()            { return province; }
    public String  getLocality()            { return locality; }
    public boolean hasLogo()                { return logoBase64 != null && !logoBase64.isBlank(); }
    public String  getLogoBase64()          { return logoBase64; }
    public String  getLogoType()            { return logoType; }

    public static final class NameAlreadyExistsException extends DomainException {
        public NameAlreadyExistsException(String name) {
            super("Ya existe un equipo con el nombre: " + name);
        }
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Equipo no encontrado: " + id);
        }
    }
}
