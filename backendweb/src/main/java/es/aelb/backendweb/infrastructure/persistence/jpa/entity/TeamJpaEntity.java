package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class TeamJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "responsible_name", length = 100)
    private String responsibleName;

    @Column(name = "responsible_last_name", length = 100)
    private String responsibleLastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String locality;

    @Lob
    @Column(name = "logo_data", columnDefinition = "LONGTEXT")
    private String logoData;

    @Column(name = "logo_type", length = 50)
    private String logoType;

    public TeamJpaEntity() {}

    public TeamJpaEntity(String id, String name, String responsibleName, String responsibleLastName,
                         String phone, String province, String locality,
                         String logoData, String logoType) {
        this.id                  = id;
        this.name                = name;
        this.responsibleName     = responsibleName;
        this.responsibleLastName = responsibleLastName;
        this.phone               = phone;
        this.province            = province;
        this.locality            = locality;
        this.logoData            = logoData;
        this.logoType            = logoType;
    }

    public String getId()                  { return id; }
    public String getName()                { return name; }
    public String getResponsibleName()     { return responsibleName; }
    public String getResponsibleLastName() { return responsibleLastName; }
    public String getPhone()               { return phone; }
    public String getProvince()            { return province; }
    public String getLocality()            { return locality; }
    public String getLogoData()            { return logoData; }
    public String getLogoType()            { return logoType; }

    public void setId(String id)                           { this.id = id; }
    public void setName(String name)                       { this.name = name; }
    public void setResponsibleName(String v)               { this.responsibleName = v; }
    public void setResponsibleLastName(String v)           { this.responsibleLastName = v; }
    public void setPhone(String phone)                     { this.phone = phone; }
    public void setProvince(String province)               { this.province = province; }
    public void setLocality(String locality)               { this.locality = locality; }
    public void setLogoData(String logoData)               { this.logoData = logoData; }
    public void setLogoType(String logoType)               { this.logoType = logoType; }
}
