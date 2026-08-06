package es.aelb.backendweb.domain.category;

import es.aelb.backendweb.domain.shared.exception.DomainException;
import es.aelb.backendweb.domain.shared.valueobject.AggregateRoot;
import es.aelb.backendweb.domain.category.valueobject.CategoryId;

import java.math.BigDecimal;

public class Category extends AggregateRoot<CategoryId> {

    private String     name;
    private Gender     gender;
    private ArmSide    armSide;
    private BigDecimal weightLimit;
    private String     ageGroup;
    private CompetitionAgeCategory ageCategory;
    private Shift      shift;

    private Category(CategoryId id, Gender gender,
                     ArmSide armSide, BigDecimal weightLimit, String ageGroup,
                     CompetitionAgeCategory ageCategory, Shift shift) {
        super(id);
        this.gender      = gender;
        this.armSide     = armSide;
        this.weightLimit = weightLimit;
        this.ageCategory = ageCategory != null ? ageCategory : CompetitionAgeCategory.fromIdentifier(ageGroup).orElse(null);
        this.ageGroup    = this.ageCategory != null ? this.ageCategory.name() : ageGroup;
        this.shift       = this.ageCategory != null ? this.ageCategory.getShift() : shift;
        this.name        = titleFor(gender, armSide, weightLimit, this.ageGroup, this.ageCategory);
    }

    public static Category create(Gender gender, ArmSide armSide,
                                  BigDecimal weightLimit, String ageGroup,
                                  CompetitionAgeCategory ageCategory, Shift shift) {
        return new Category(CategoryId.generate(), gender, armSide, weightLimit, ageGroup, ageCategory, shift);
    }

    public static Category reconstitute(CategoryId id, String name, Gender gender,
                                        ArmSide armSide, BigDecimal weightLimit, String ageGroup,
                                        CompetitionAgeCategory ageCategory, Shift shift) {
        return new Category(id, gender, armSide, weightLimit, ageGroup, ageCategory, shift);
    }

    public void update(Gender gender, ArmSide armSide,
                       BigDecimal weightLimit, String ageGroup,
                       CompetitionAgeCategory ageCategory, Shift shift) {
        this.gender      = gender;
        this.armSide     = armSide;
        this.weightLimit = weightLimit;
        this.ageCategory = ageCategory != null ? ageCategory : CompetitionAgeCategory.fromIdentifier(ageGroup).orElse(null);
        this.ageGroup    = this.ageCategory != null ? this.ageCategory.name() : ageGroup;
        this.shift       = this.ageCategory != null ? this.ageCategory.getShift() : shift;
        this.name        = titleFor(gender, armSide, weightLimit, this.ageGroup, this.ageCategory);
    }

    public static String titleFor(Gender gender, ArmSide armSide, BigDecimal weightLimit,
                                  String ageGroup, CompetitionAgeCategory ageCategory) {
        CompetitionAgeCategory effectiveAgeCategory = ageCategory != null ? ageCategory
                : CompetitionAgeCategory.fromIdentifier(ageGroup).orElse(null);
        String ageLabel = effectiveAgeCategory != null ? effectiveAgeCategory.getDisplayName()
                : (ageGroup == null || ageGroup.isBlank() ? "Categoría" : ageGroup);
        String genderLabel = switch (gender) {
            case MALE -> "Masculino";
            case FEMALE -> "Femenino";
            case OPEN -> "Open";
        };
        String armLabel = switch (armSide) {
            case RIGHT -> "Derecho";
            case LEFT -> "Izquierdo";
            case BOTH -> "Ambos brazos";
        };
        String weightLabel = weightLimit == null ? ""
                : " -" + weightLimit.stripTrailingZeros().toPlainString() + "kg";
        return ageLabel + " " + genderLabel + " " + armLabel + weightLabel;
    }

    public String     getName()        { return name; }
    public Gender     getGender()      { return gender; }
    public ArmSide    getArmSide()     { return armSide; }
    public BigDecimal getWeightLimit() { return weightLimit; }
    public String     getAgeGroup()    { return ageGroup; }
    public CompetitionAgeCategory getAgeCategory() { return ageCategory; }
    public Shift      getShift()       { return shift; }

    public static final class NameAlreadyExistsException extends DomainException {
        public NameAlreadyExistsException(String name) {
            super("Ya existe una categoría con el nombre: " + name);
        }
    }

    public static final class NotFoundException extends DomainException {
        public NotFoundException(String id) {
            super("Categoría no encontrada: " + id);
        }
    }
}
