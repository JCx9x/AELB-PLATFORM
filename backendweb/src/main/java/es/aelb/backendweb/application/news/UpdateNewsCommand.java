package es.aelb.backendweb.application.news;

public record UpdateNewsCommand(
        String  newsId,
        String  title,
        String  content,
        String  imageKey,      // null = keep existing image
        boolean removeImage,   // true = delete image
        boolean published
) {}
