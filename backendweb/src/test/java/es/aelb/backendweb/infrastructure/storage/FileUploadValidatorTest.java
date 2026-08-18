package es.aelb.backendweb.infrastructure.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUploadValidatorTest {
    @Test
    void aceptaPdfConFirmaReal() {
        byte[] pdf = "%PDF-1.7\ncontenido".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(pdf, FileUploadValidator.decodePdf(Base64.getEncoder().encodeToString(pdf), "resultado.pdf"));
    }

    @Test
    void rechazaPdfConExtensionFalsa() {
        byte[] notPdf = "texto no pdf".getBytes(StandardCharsets.US_ASCII);
        assertThrows(IllegalArgumentException.class,
                () -> FileUploadValidator.decodePdf(Base64.getEncoder().encodeToString(notPdf), "resultado.pdf"));
    }

    @Test
    void rechazaExtensionNoPermitida() {
        byte[] pdf = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        assertThrows(IllegalArgumentException.class,
                () -> FileUploadValidator.decodeDocument(Base64.getEncoder().encodeToString(pdf), "script.exe"));
    }
}
