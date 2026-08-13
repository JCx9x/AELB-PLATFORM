package es.aelb.backendweb.application.document;

public record UpdateDocumentCommand(
        String  documentId,
        String  title,
        String  description,
        String  fileBase64,   // null = keep existing file; non-null se sube a S3
        String  fileName,     // null = keep existing file name
        boolean published
) {}
