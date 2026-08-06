package es.aelb.backendweb.application.document;

public record CreateDocumentCommand(
        String  title,
        String  description,
        String  fileBase64,
        String  fileName,
        boolean published,
        String  authorUserId
) {}
