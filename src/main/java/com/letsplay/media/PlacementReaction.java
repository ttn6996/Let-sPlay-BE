package com.letsplay.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "placement_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_placement_reaction", columnNames = {"placement_id", "account_id"})
})
public class PlacementReaction {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "placement_id", nullable = false, length = 36)
    private String placementId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "reaction_type", nullable = false, length = 32)
    private String reactionType = "HEART";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlacementReaction() {}

    public PlacementReaction(String placementId, String accountId, String reactionType) {
        this.id = UUID.randomUUID().toString();
        this.placementId = placementId;
        this.accountId = accountId;
        this.reactionType = reactionType != null ? reactionType : "HEART";
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPlacementId() { return placementId; }
    public String getAccountId() { return accountId; }
    public String getReactionType() { return reactionType; }
    public Instant getCreatedAt() { return createdAt; }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }
}
