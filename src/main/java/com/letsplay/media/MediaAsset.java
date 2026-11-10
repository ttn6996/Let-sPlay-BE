package com.letsplay.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "owner_account_id", nullable = false, length = 36)
    private String ownerAccountId;

    @Column(name = "storage_provider", nullable = false, length = 32)
    private String storageProvider;

    @Column(name = "storage_file_id", nullable = false)
    private String storageFileId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "thumbnail_url", nullable = false, columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    private Integer width;
    private Integer height;

    @Column(length = 64)
    private String sha256;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected MediaAsset() {}

    public MediaAsset(String ownerAccountId, String storageProvider, String storageFileId,
                      String url, String thumbnailUrl, String contentType, long byteSize,
                      Integer width, Integer height, String sha256) {
        this.id = UUID.randomUUID().toString();
        this.ownerAccountId = ownerAccountId;
        this.storageProvider = storageProvider;
        this.storageFileId = storageFileId;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.width = width;
        this.height = height;
        this.sha256 = sha256;
        this.status = "READY";
        this.createdAt = Instant.now();
        this.readyAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getOwnerAccountId() { return ownerAccountId; }
    public String getStorageProvider() { return storageProvider; }
    public String getStorageFileId() { return storageFileId; }
    public String getUrl() { return url; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getContentType() { return contentType; }
    public long getByteSize() { return byteSize; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public String getSha256() { return sha256; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadyAt() { return readyAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public void setStatus(String status) { this.status = status; }
}
