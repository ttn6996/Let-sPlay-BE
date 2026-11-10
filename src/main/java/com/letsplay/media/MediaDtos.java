package com.letsplay.media;

import java.time.Instant;
import java.util.Map;

public class MediaDtos {

    public record MediaAssetResponse(
            String mediaAssetId,
            String url,
            String thumbnailUrl,
            String status,
            long byteSize,
            String contentType,
            Integer width,
            Integer height,
            String storageProvider,
            Instant createdAt
    ) {}

    public record UploadSessionRequest(
            String source,
            String contentType,
            long byteSize,
            String sha256,
            Integer width,
            Integer height,
            Instant capturedAt,
            String clientTimezone
    ) {}

    public record UploadSessionResponse(
            String mediaAssetId,
            String uploadUrl,
            String method,
            Map<String, String> requiredHeaders,
            Instant uploadExpiresAt
    ) {}
}
