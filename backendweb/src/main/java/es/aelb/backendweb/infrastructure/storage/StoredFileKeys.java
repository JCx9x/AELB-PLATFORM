package es.aelb.backendweb.infrastructure.storage;

import java.util.Locale;

public final class StoredFileKeys {

    private StoredFileKeys() {}

    public static String document(String id, String fileName) {
        return "documents/" + id + "/" + safeFileName(fileName, "documento.bin");
    }

    public static String result(String id, String fileName) {
        return "results/" + id + "/" + safeFileName(fileName, "resultado.pdf");
    }

    public static String contentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    private static String safeFileName(String fileName, String fallback) {
        if (fileName == null || fileName.isBlank()) return fallback;
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
