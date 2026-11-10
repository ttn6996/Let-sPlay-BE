package com.letsplay.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "placement_comments")
public class PlacementComment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "placement_id", nullable = false, length = 36)
    private String placementId;

    @Column(name = "author_account_id", nullable = false, length = 36)
    private String authorAccountId;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false, length = 32)
    private String state = "ACTIVE"; // "ACTIVE" or "DELETED"

    @Column(name = "client_mutation_id", length = 64)
    private String clientMutationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlacementComment() {}

    public PlacementComment(String placementId, String authorAccountId, String text, String clientMutationId) {
        this.id = UUID.randomUUID().toString();
        this.placementId = placementId;
        this.authorAccountId = authorAccountId;
        this.text = text;
        this.state = "ACTIVE";
        this.clientMutationId = clientMutationId;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPlacementId() { return placementId; }
    public String getAuthorAccountId() { return authorAccountId; }
    public String getText() { return text; }
    public String getState() { return state; }
    public String getClientMutationId() { return clientMutationId; }
    public Instant getCreatedAt() { return createdAt; }

    public void markDeleted() {
        this.state = "DELETED";
    }
}
