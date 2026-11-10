package es.aelb.backendweb.infrastructure.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.aelb.backendweb.infrastructure.config.StripeProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code describeError} es la única lógica no trivial añadida en H-02/H-10 (extracción y
 * truncado del error de Stripe para loguearlo). El resto del adaptador hace llamadas HTTPS
 * reales a api.stripe.com y no se mockea aquí para no introducir una reestructuración
 * (cliente/URL inyectables) fuera del alcance de esta corrección.
 */
class StripeCheckoutGatewayAdapterTest {

    private StripeCheckoutGatewayAdapter adapter() {
        return new StripeCheckoutGatewayAdapter(new StripeProperties(), new ObjectMapper(), new SimpleMeterRegistry());
    }

    @Test
    void extraeCodigoTipoYMensajeDeUnErrorDeStripe() {
        String body = "{\"error\":{\"type\":\"card_error\",\"code\":\"amount_too_small\",\"message\":\"Amount must be at least 50 cents.\"}}";
        String described = adapter().describeError(body);
        assertEquals("code=amount_too_small type=card_error message=Amount must be at least 50 cents.", described);
    }

    @Test
    void cuerpoNoParseableNoLanzaYDevuelveSoloElTamano() {
        String described = adapter().describeError("<html>502 Bad Gateway</html>");
        assertTrue(described.contains("unparseable response body"));
        assertTrue(described.contains(String.valueOf("<html>502 Bad Gateway</html>".length())));
    }

    @Test
    void truncaMensajesLargosA300Caracteres() {
        String longMessage = "x".repeat(500);
        String body = "{\"error\":{\"type\":\"api_error\",\"code\":\"\",\"message\":\"" + longMessage + "\"}}";
        String described = adapter().describeError(body);
        assertEquals(300, described.length());
    }

    @Test
    void nuncaIncluyeLaClaveSecretaAunqueApareceraEnElCuerpo() {
        // Un cuerpo "contaminado" con algo que parezca una clave de Stripe no debería
        // colarse salvo que la propia API de Stripe lo devuelva dentro de error.message
        // (nunca ocurre en la práctica: Stripe no eco su propia Authorization). Se verifica
        // aquí que describeError solo mira code/type/message, no el cuerpo completo.
        String body = "{\"secret_leak_test\":\"sk_live_should_never_appear\",\"error\":{\"type\":\"api_error\",\"code\":\"\",\"message\":\"ok\"}}";
        String described = adapter().describeError(body);
        assertFalse(described.contains("sk_live_should_never_appear"));
    }
}
