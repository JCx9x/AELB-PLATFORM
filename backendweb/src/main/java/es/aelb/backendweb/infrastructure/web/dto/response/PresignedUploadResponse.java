package es.aelb.backendweb.infrastructure.web.dto.response;

public record PresignedUploadResponse(
        String uploadUrl,
        String key
) {}
