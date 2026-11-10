package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.infrastructure.storage.PresignedUrlService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StorageControllerTest {
    private static final long MAX = 5 * 1024 * 1024;

    @Test void jpegGeneraUrlConExtensionJpg() {
        PresignedUrlService presigner = presigner();
        var response = new StorageController(presigner).presignUpload("championships", "image/jpeg", 1024);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().key().matches("championships/[0-9a-f-]{36}\\.jpg"));
        verify(presigner).generateUploadUrl(startsWith("championships/"), eq("image/jpeg"), eq(1024L));
    }

    @Test void formatoNoPermitidoDevuelveMensajeClaro() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new StorageController(presigner()).presignUpload("championships", "image/heic", 1024));
        assertTrue(e.getMessage().contains("JPG, PNG, WebP o GIF"));
    }

    @Test void imagenDemasiadoGrandeIndicaElLimite() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new StorageController(presigner()).presignUpload("championships", "image/png", MAX + 1));
        assertTrue(e.getMessage().contains("5 MB"));
    }

    private PresignedUrlService presigner() {
        PresignedUrlService p = mock(PresignedUrlService.class);
        when(p.maxUploadBytes()).thenReturn(MAX);
        when(p.generateUploadUrl(anyString(), anyString(), anyLong())).thenReturn("https://s3.example/upload");
        return p;
    }
}
