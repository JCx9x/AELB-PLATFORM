package es.aelb.backendweb.domain.pricing.engine;

import es.aelb.backendweb.domain.category.ArmSide;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Conjunto de inscripciones que el atleta quiere realizar en una sola operación.
 *
 * Es el INPUT del motor de precios. Inmutable.
 */
public final class RegistrationBasket {

    private final List<RegistrationEntry> entries;

    private RegistrationBasket(List<RegistrationEntry> entries) {
        if (entries == null || entries.isEmpty())
            throw new IllegalArgumentException("El basket debe tener al menos una inscripción");
        this.entries = List.copyOf(entries);
    }

    public static RegistrationBasket of(List<RegistrationEntry> entries) {
        return new RegistrationBasket(entries);
    }

    public List<RegistrationEntry> getEntries() {
        return entries;
    }

    /** Lados de brazo distintos presentes en el basket (RIGHT, LEFT, o ambos). */
    public Set<ArmSide> getDistinctArmSides() {
        return entries.stream()
                .map(RegistrationEntry::armSide)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Tipos de categoría distintos presentes en el basket (p.ej. {"SENIOR", "MASTER"}). */
    public Set<String> getDistinctCategoryTypes() {
        return entries.stream()
                .map(RegistrationEntry::categoryTypeId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Entradas agrupadas por brazo (útil para validar max por brazo). */
    public List<RegistrationEntry> entriesForArm(ArmSide arm) {
        return entries.stream()
                .filter(e -> e.armSide() == arm)
                .toList();
    }

    public int size() { return entries.size(); }
}
