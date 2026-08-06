package es.aelb.backendweb.application.pricing;

import es.aelb.backendweb.application.shared.UseCase;
import es.aelb.backendweb.domain.shared.exception.DomainException;

/**
 * Aplica un template de configuración de precios a un campeonato.
 * Actualmente soporta el template "AELB_STANDARD".
 * Añadir nuevos templates: crear la clase de factory y registrarla en el switch.
 */
public class ApplyPricingTemplateUseCase implements UseCase<ApplyPricingTemplateUseCase.Command, String> {

    private final SavePricingConfigUseCase savePricingConfigUseCase;

    public ApplyPricingTemplateUseCase(SavePricingConfigUseCase savePricingConfigUseCase) {
        this.savePricingConfigUseCase = savePricingConfigUseCase;
    }

    public record Command(String championshipId, String templateName) {}

    @Override
    public String execute(Command cmd) {
        SavePricingConfigCommand config = switch (cmd.templateName().toUpperCase()) {
            case "AELB_STANDARD" -> AelbStandardPricingTemplate.buildDefaultCommand(cmd.championshipId());
            default -> throw new UnknownTemplateException(cmd.templateName());
        };
        return savePricingConfigUseCase.execute(config);
    }

    public static final class UnknownTemplateException extends DomainException {
        public UnknownTemplateException(String name) {
            super("Template de precios desconocido: " + name + ". Templates disponibles: AELB_STANDARD");
        }
    }
}
