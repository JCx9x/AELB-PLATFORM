package es.aelb.backendweb.application.result;

public record UpdateResultCommand(
        String  resultId,
        String  title,
        String  description,
        String  pdfBase64,   // null = keep existing PDF; non-null se sube a S3
        String  pdfFileName, // null = keep existing file name
        boolean published
) {}
