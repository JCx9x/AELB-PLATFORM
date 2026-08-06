package es.aelb.backendweb.infrastructure.persistence.jpa.entity;

import es.aelb.backendweb.domain.category.ArmSide;
import es.aelb.backendweb.domain.category.Gender;
import es.aelb.backendweb.domain.category.CompetitionAgeCategory;
import es.aelb.backendweb.domain.category.Shift;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "categories")
public class CategoryJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "arm_side", nullable = false)
    private ArmSide armSide;

    @Column(name = "weight_limit", precision = 5, scale = 2)
    private BigDecimal weightLimit;

    @Column(name = "age_group", length = 50)
    private String ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_category", length = 40)
    private CompetitionAgeCategory ageCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", nullable = false)
    private Shift shift;

    public CategoryJpaEntity() {}

    public CategoryJpaEntity(String id, String name, Gender gender,
                              ArmSide armSide, BigDecimal weightLimit, String ageGroup, CompetitionAgeCategory ageCategory, Shift shift) {
        this.id          = id;
        this.name        = name;
        this.gender      = gender;
        this.armSide     = armSide;
        this.weightLimit = weightLimit;
        this.ageGroup    = ageGroup;
        this.ageCategory = ageCategory;
        this.shift       = shift;
    }

    public String     getId()          { return id; }
    public String     getName()        { return name; }
    public Gender     getGender()      { return gender; }
    public ArmSide    getArmSide()     { return armSide; }
    public BigDecimal getWeightLimit() { return weightLimit; }
    public String     getAgeGroup()    { return ageGroup; }
    public CompetitionAgeCategory getAgeCategory() { return ageCategory; }
    public Shift      getShift()       { return shift; }

    public void setId(String id)                   { this.id = id; }
    public void setName(String name)               { this.name = name; }
    public void setGender(Gender gender)           { this.gender = gender; }
    public void setArmSide(ArmSide armSide)        { this.armSide = armSide; }
    public void setWeightLimit(BigDecimal w)       { this.weightLimit = w; }
    public void setAgeGroup(String ageGroup)       { this.ageGroup = ageGroup; }
    public void setAgeCategory(CompetitionAgeCategory ageCategory) { this.ageCategory = ageCategory; }
    public void setShift(Shift shift)              { this.shift = shift; }
}
