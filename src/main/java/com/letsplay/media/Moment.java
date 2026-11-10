package com.letsplay.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moments")
public class Moment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "owner_account_id", nullable = false, length = 36)
    private String ownerAccountId;

    @Column(name = "media_asset_id", nullable = false, length = 36)
    private String mediaAssetId;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(nullable = false)
    private int revision;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "client_mutation_id", length = 64)
    private String clientMutationId;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Moment() {}

    public Moment(String ownerAccountId, String mediaAssetId, String caption, String clientMutationId) {
        this.id = UUID.randomUUID().toString();
        this.ownerAccountId = ownerAccountId;
        this.mediaAssetId = mediaAssetId;
        this.caption = caption;
        this.revision = 1;
        this.status = "PUBLISHED";
        this.clientMutationId = clientMutationId;
        this.createdAt = Instant.now();
        this.publishedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getOwnerAccountId() { return ownerAccountId; }
    public String getMediaAssetId() { return mediaAssetId; }
    public String getCaption() { return caption; }
    public int getRevision() { return revision; }
    public String getStatus() { return status; }
    public String getClientMutationId() { return clientMutationId; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public void bumpRevision() { this.revision++; }
    public void markDeleted() {
        this.status = "DELETED";
        this.deletedAt = Instant.now();
        bumpRevision();
    }
}
