package es.aelb.backendweb.domain.quota;

import es.aelb.backendweb.domain.shared.exception.DomainException;
import java.math.BigDecimal;

/** Precio configurable para un tramo de edad en un año concreto. */
public class AnnualQuotaPrice {
    private final int year;
    private final AgeCategory ageCategory;
    private final BigDecimal amount;

    public AnnualQuotaPrice(int year, AgeCategory ageCategory, BigDecimal amount) {
        if (year < 2020 || year > 2100) throw new IllegalArgumentException("Año de cuota inválido: " + year);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El importe no puede ser negativo");
        this.year = year;
        this.ageCategory = ageCategory;
        this.amount = amount;
    }
    public int getYear() { return year; }
    public AgeCategory getAgeCategory() { return ageCategory; }
    public BigDecimal getAmount() { return amount; }

    public static final class NotConfiguredException extends DomainException {
        public NotConfiguredException(int year, AgeCategory category) {
            super("No hay precio configurado para " + category.getLabel() + " en " + year);
        }
    }
}
