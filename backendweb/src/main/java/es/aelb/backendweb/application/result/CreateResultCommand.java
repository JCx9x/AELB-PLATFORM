package es.aelb.backendweb.application.result;

public record CreateResultCommand(
        String  title,
        String  description,
        String  pdfBase64,    // raw Base64 recibido por la API; se guarda en S3, no en MySQL
        String  pdfFileName,
        boolean published,
        String  authorUserId
) {}
