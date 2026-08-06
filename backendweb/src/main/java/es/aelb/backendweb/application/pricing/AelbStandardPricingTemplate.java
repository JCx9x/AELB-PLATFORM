package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.domain.pricing.PricingEffectType;
import es.aelb.backendweb.domain.pricing.RuleConditionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Template de configuración estándar AELB para campeonatos de armwrestling.
 *
 * Genera un SavePricingConfigCommand listo para aplicar a cualquier campeonato.
 * Los precios son parámetros → se puede crear una variante con precios distintos.
 *
 * ─── REGLAS GENERADAS ───────────────────────────────────────────────────────
 *
 *  Prio  Nombre                          Condición                  Efecto
 *  ───────────────────────────────────────────────────────────────────────────
 *   5    Infantil precio especial         AGE_MAX=15                 BUNDLE 10€
 *   10   Juniors-18 solo (bundle)         ONLY_TYPES=JUNIOR_18       BUNDLE 20€
 *   15   Juniors-18 + Senior (bundle)     ALL_TYPES=JUNIOR_18,SENIOR BUNDLE 30€
 *  100   Precio base por brazo            (siempre)                  BASE_PER_ARM 20€
 *  200   Suplemento categoría adicional   (siempre)                  SUPP_EXTRA_CAT 10€
 *
 *  Restricciones de elegibilidad:
 *    PRINCIPIANTE → maxExperienceYears=1, maxGoldMedals=1
 *    JUNIOR_18    → ageMax=18
 *    JUNIOR_23    → ageMax=23
 *    INFANTIL     → ageMax=15
 *
 *  Ejemplos verificados:
 *    Senior DR                           = 20€
 *    Senior DR + Master DR               = 30€  (1 brazo + 1 extra cat)
 *    Senior ambos brazos                 = 40€  (2 brazos)
 *    Senior + Master ambos brazos        = 50€  (2 brazos + 1 extra cat)
 *    Juniors-18 ambos brazos (solo)      = 20€  (bundle override)
 *    Juniors-18 + Senior (combo)         = 30€  (bundle override)
 *    Juniors-23 + Senior ambos brazos    = 50€  (2 brazos + 1 extra cat)
 *    Infantil (≤15 años)                 = 10€  (bundle override)
 */
public final class AelbStandardPricingTemplate {

    private AelbStandardPricingTemplate() {}

    public static SavePricingConfigCommand buildCommand(
            String championshipId,
            BigDecimal baseArmPrice,         // típico: 20.00
            BigDecimal extraCategorySupp,    // típico: 10.00
            BigDecimal junior18Bundle,       // típico: 20.00
            BigDecimal junior18SeniorBundle, // típico: 30.00
            BigDecimal infantilFixed         // típico: 10.00
    ) {
        return new SavePricingConfigCommand(
                championshipId,
                2,    // max categorías por brazo
                4,    // max total inscripciones
                "EUR",
                List.of(
                        // ── Bundles (prioridad alta) ────────────────────────────────
                        new SavePricingConfigCommand.RuleData(
                                "Infantil precio especial",
                                "Atletas hasta 15 años: inscripción a precio fijo",
                                5, true,
                                List.of(new SavePricingConfigCommand.ConditionData(RuleConditionType.AGE_MAX, "15")),
                                new SavePricingConfigCommand.EffectData(PricingEffectType.BUNDLE_FIXED_TOTAL, infantilFixed, "Inscripción infantil")
                        ),
                        new SavePricingConfigCommand.RuleData(
                                "Juniors-18 solo – ambos brazos",
                                "Atleta inscrito ÚNICAMENTE en Juniors-18 (1 ó 2 brazos): precio especial plano",
                                10, true,
                                List.of(new SavePricingConfigCommand.ConditionData(RuleConditionType.BASKET_CONTAINS_ONLY_TYPES, "JUNIOR_18")),
                                new SavePricingConfigCommand.EffectData(PricingEffectType.BUNDLE_FIXED_TOTAL, junior18Bundle, "Juniors-18 precio especial")
                        ),
                        new SavePricingConfigCommand.RuleData(
                                "Juniors-18 + Senior combo",
                                "Combinación Juniors-18 y Senior: precio de combo",
                                15, true,
                                List.of(new SavePricingConfigCommand.ConditionData(RuleConditionType.BASKET_CONTAINS_ALL_TYPES, "JUNIOR_18,SENIOR")),
                                new SavePricingConfigCommand.EffectData(PricingEffectType.BUNDLE_FIXED_TOTAL, junior18SeniorBundle, "Suplemento Juniors-18 + Senior")
                        ),
                        // ── Regla base (prioridad baja = se aplica si no hay bundle) ─
                        new SavePricingConfigCommand.RuleData(
                                "Precio base por brazo",
                                "Inscripción estándar: " + baseArmPrice + "€ por brazo registrado",
                                100, true,
                                List.of(),
                                new SavePricingConfigCommand.EffectData(PricingEffectType.BASE_PER_ARM, baseArmPrice, "Inscripción")
                        ),
                        // ── Suplemento categoría adicional ───────────────────────────
                        new SavePricingConfigCommand.RuleData(
                                "Suplemento categoría adicional",
                                "+" + extraCategorySupp + "€ por cada tipo de categoría adicional al primero",
                                200, true,
                                List.of(),
                                new SavePricingConfigCommand.EffectData(PricingEffectType.SUPPLEMENT_PER_EXTRA_CATEGORY, extraCategorySupp, "Categoría adicional")
                        )
                ),
                List.of(
                        // ── Restricciones de elegibilidad ────────────────────────────
                        new SavePricingConfigCommand.RestrictionData(
                                "PRINCIPIANTE", null, null, 1, 1,
                                Set.of(), Set.of()
                        ),
                        new SavePricingConfigCommand.RestrictionData(
                                "JUNIOR_18", null, 18, null, null,
                                Set.of(), Set.of()
                        ),
                        new SavePricingConfigCommand.RestrictionData(
                                "JUNIOR_23", null, 23, null, null,
                                Set.of(), Set.of()
                        ),
                        new SavePricingConfigCommand.RestrictionData(
                                "INFANTIL", null, 15, null, null,
                                Set.of(), Set.of()
                        )
                )
        );
    }

    /** Configuración con los precios por defecto de AELB. */
    public static SavePricingConfigCommand buildDefaultCommand(String championshipId) {
        return buildCommand(
                championshipId,
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("30.00"),
                new BigDecimal("10.00")
        );
    }
}
