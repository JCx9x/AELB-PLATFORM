package es.aelb.backendweb.infrastructure.email;

import es.aelb.backendweb.infrastructure.config.EmailProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailConfigurationValidatorTest {
    @Test void emailDeshabilitadoNoExigeConfiguracionSes() {
        EmailProperties properties = new EmailProperties();
        properties.setEnabled(false);
        properties.setAppEnv("pre");
        properties.setProvider("ses");

        assertDoesNotThrow(() -> new EmailConfigurationValidator(properties).validate());
    }

    @Test void preSesHabilitadoRequiereClaveYConfiguracionCompleta() {
        EmailProperties properties = new EmailProperties();
        properties.setEnabled(true);
        properties.setAppEnv("pre");
        properties.setProvider("ses");
        properties.setFrom("javiercanoramirez@gmail.com");
        properties.setSesRegion("eu-south-1");
        properties.setTokenEncryptionKey("pre-test-key");

        assertDoesNotThrow(() -> new EmailConfigurationValidator(properties).validate());

        properties.setTokenEncryptionKey("");
        assertThrows(IllegalStateException.class, () -> new EmailConfigurationValidator(properties).validate());
    }
}
