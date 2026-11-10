package es.aelb.backendweb.infrastructure.email;

import es.aelb.backendweb.application.email.EmailMessage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AelbEmailTemplateTest {
    private final AelbEmailTemplate template = new AelbEmailTemplate("https://aelb.es/");

    @Test void identificaALaAelbYAvisaDeQueNoSeResponda() {
        EmailMessage m = template.render("user@example.com", content("Ana", "Open AELB 2026"));

        assertEquals("AELB · Inscripción confirmada", m.subject());
        assertTrue(m.html().contains("Asociación Española de Lucha de Brazos"));
        assertTrue(m.html().contains("Correo generado automáticamente"));
        assertTrue(m.html().contains("no respondas a este correo"));
        assertTrue(m.plainText().startsWith("ASOCIACIÓN ESPAÑOLA DE LUCHA DE BRAZOS (AELB)"));
        assertTrue(m.plainText().contains(AelbEmailTemplate.AUTOMATIC_NOTICE));
        assertTrue(m.html().contains("href=\"https://aelb.es/privacidad\""));
    }

    @Test void escapaLosDatosDelUsuarioYNoDejaMarcadoresSinSustituir() {
        EmailMessage m = template.render("user@example.com", content("<b>{{content}}</b>", "<script>x</script>"));

        assertFalse(m.html().contains("<script>"));
        assertTrue(m.html().contains("Hola &lt;b&gt;{{content}}&lt;/b&gt;,"));
        assertFalse(m.html().replace("Hola &lt;b&gt;{{content}}", "").contains("{{"));
    }

    private AelbEmailTemplate.Content content(String name, String championship) {
        return new AelbEmailTemplate.Content("Inscripción confirmada", "Campeonatos", "Inscripción confirmada", name,
                List.of("Tu inscripción ha quedado registrada correctamente."),
                List.of(new AelbEmailTemplate.Detail("Campeonato", championship)),
                "Ver campeonatos", "https://aelb.es/campeonatos", null);
    }
}
