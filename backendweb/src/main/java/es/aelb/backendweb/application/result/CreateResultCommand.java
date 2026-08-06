package es.aelb.backendweb.application.result;

public record CreateResultCommand(
        String  title,
        String  description,
        String  pdfBase64,    // raw base64 of original PDF (server will GZip-compress)
        String  pdfFileName,
        boolean published,
        String  authorUserId
) {}
