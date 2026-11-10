package com.letsplay.storage;

public record StoredFile(
        String fileId,
        String storageProvider,
        String url,
        String thumbnailUrl,
        long byteSize,
        String contentType
) {}
