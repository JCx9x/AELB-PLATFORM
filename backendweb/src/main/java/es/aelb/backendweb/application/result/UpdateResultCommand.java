package es.aelb.backendweb.application.result;

public record UpdateResultCommand(
        String  resultId,
        String  title,
        String  description,
        String  pdfBase64,   // null = keep existing PDF
        String  pdfFileName, // null = keep existing file name
        boolean published
) {}
