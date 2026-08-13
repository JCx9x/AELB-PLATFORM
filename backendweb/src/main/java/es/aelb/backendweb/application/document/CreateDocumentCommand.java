package es.aelb.backendweb.application.document;

public record CreateDocumentCommand(
        String  title,
        String  description,
        String  fileBase64,    // raw Base64 recibido por la API; se guarda en S3, no en MySQL
        String  fileName,
        boolean published,
        String  authorUserId
) {}
