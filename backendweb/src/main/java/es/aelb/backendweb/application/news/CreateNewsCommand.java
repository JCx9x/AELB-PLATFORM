package es.aelb.backendweb.application.news;

public record CreateNewsCommand(
        String  title,
        String  content,
        String  imageKey,      // nullable — S3 object key
        boolean published,
        String  authorUserId
) {}
