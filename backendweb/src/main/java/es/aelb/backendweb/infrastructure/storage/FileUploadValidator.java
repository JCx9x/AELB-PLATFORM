package es.aelb.backendweb.infrastructure.storage;

import java.util.Base64;
import java.util.Locale;

/** Valida el contenido binario antes de materializarlo y enviarlo al almacenamiento. */
public final class FileUploadValidator {
    public static final int MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;
    public static final int MAX_RESULT_BYTES = 20 * 1024 * 1024;

    private FileUploadValidator() {}

    public static byte[] decodeDocument(String encoded, String fileName) {
        byte[] bytes = decode(encoded, MAX_DOCUMENT_BYTES, "documento");
        String extension = extension(fileName);
        if (!SetOf.DOCUMENT_EXTENSIONS.contains(extension) || !matchesMagic(bytes, extension)) {
            throw new IllegalArgumentException("El tipo real del documento no coincide con una extensión permitida");
        }
        return bytes;
    }

    public static byte[] decodePdf(String encoded, String fileName) {
        byte[] bytes = decode(encoded, MAX_RESULT_BYTES, "PDF");
        if (!"pdf".equals(extension(fileName)) || !hasPrefix(bytes, new byte[]{ '%', 'P', 'D', 'F', '-' })) {
            throw new IllegalArgumentException("El archivo de resultados debe ser un PDF válido");
        }
        return bytes;
    }

    private static byte[] decode(String encoded, int maxBytes, String kind) {
        if (encoded == null || encoded.isBlank()) throw new IllegalArgumentException("El " + kind + " no puede estar vacío");
        // Evita entregar cadenas Base64 arbitrariamente grandes al decoder.
        long maximumEncodedLength = ((long) maxBytes + 2) / 3 * 4 + 4;
        if (encoded.length() > maximumEncodedLength) throw new IllegalArgumentException("El " + kind + " supera el tamaño máximo permitido");
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El contenido Base64 no es válido");
        }
        if (bytes.length == 0 || bytes.length > maxBytes) throw new IllegalArgumentException("El " + kind + " supera el tamaño máximo permitido");
        return bytes;
    }

    private static String extension(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";
        String safeName = fileName.replace('\\', '/');
        if (safeName.contains("..") || safeName.lastIndexOf('/') >= 0) return "";
        int dot = safeName.lastIndexOf('.');
        return dot < 0 ? "" : safeName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean matchesMagic(byte[] bytes, String extension) {
        return switch (extension) {
            case "pdf" -> hasPrefix(bytes, new byte[]{ '%', 'P', 'D', 'F', '-' });
            case "png" -> hasPrefix(bytes, new byte[]{ (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
            case "jpg", "jpeg" -> bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case "gif" -> hasPrefix(bytes, new byte[]{ 'G', 'I', 'F', '8' });
            case "docx", "xlsx" -> hasPrefix(bytes, new byte[]{ 'P', 'K', 0x03, 0x04 });
            default -> false;
        };
    }

    private static boolean hasPrefix(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (bytes[i] != prefix[i]) return false;
        return true;
    }

    private static final class SetOf {
        private static final java.util.Set<String> DOCUMENT_EXTENSIONS =
                java.util.Set.of("pdf", "png", "jpg", "jpeg", "gif", "docx", "xlsx");
    }
}
