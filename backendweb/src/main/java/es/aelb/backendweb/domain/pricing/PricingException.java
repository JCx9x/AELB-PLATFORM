package es.aelb.backendweb.domain.pricing;

import es.aelb.backendweb.domain.shared.exception.DomainException;

public class PricingException extends DomainException {

    public PricingException(String message) { super(message); }

    public static final class ConfigNotFoundException extends PricingException {
        public ConfigNotFoundException(String championshipId) {
            super("No existe configuración de precios para el campeonato: " + championshipId);
        }
    }

    public static final class DuplicateConfigException extends PricingException {
        public DuplicateConfigException(String championshipId) {
            super("Ya existe una configuración de precios para el campeonato: " + championshipId);
        }
    }
}
