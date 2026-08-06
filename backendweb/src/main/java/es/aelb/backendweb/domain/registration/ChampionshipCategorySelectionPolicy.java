package es.aelb.backendweb.domain.registration;

import es.aelb.backendweb.domain.category.ArmSide;
import es.aelb.backendweb.domain.category.Category;
import es.aelb.backendweb.domain.category.Shift;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Regla común para las categorías que un atleta puede elegir en cada turno. */
public final class ChampionshipCategorySelectionPolicy {
    private ChampionshipCategorySelectionPolicy() {}

    public static boolean isValid(Collection<Category> categories) {
        Map<Shift, List<Category>> byShift = categories.stream()
                .collect(Collectors.groupingBy(Category::getShift));
        return byShift.values().stream().allMatch(ChampionshipCategorySelectionPolicy::isValidForShift);
    }

    public static boolean canAdd(Collection<Category> current, Category candidate) {
        return isValid(java.util.stream.Stream.concat(current.stream(), java.util.stream.Stream.of(candidate)).toList());
    }

    private static boolean isValidForShift(List<Category> categories) {
        if (categories.size() <= 1) return true;
        if (categories.size() > 2) return false;
        Category first = categories.getFirst();
        Category second = categories.getLast();
        boolean sameAgeCategory = first.getAgeCategory() != null || second.getAgeCategory() != null
                ? first.getAgeCategory() == second.getAgeCategory()
                : Objects.equals(first.getAgeGroup(), second.getAgeGroup());
        boolean sameBase = sameAgeCategory
                && first.getGender() == second.getGender()
                && Objects.equals(first.getWeightLimit(), second.getWeightLimit());
        boolean differentArms = first.getArmSide() != second.getArmSide();
        boolean individualArms = first.getArmSide() != ArmSide.BOTH && second.getArmSide() != ArmSide.BOTH;
        return sameBase && differentArms && individualArms;
    }
}
