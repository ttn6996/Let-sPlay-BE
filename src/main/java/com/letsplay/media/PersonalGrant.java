package com.letsplay.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "personal_grants")
public class PersonalGrant {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "placement_id", nullable = false, length = 36)
    private String placementId;

    @Column(name = "grantee_account_id", nullable = false, length = 36)
    private String granteeAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PersonalGrant() {}

    public PersonalGrant(String placementId, String granteeAccountId) {
        this.id = UUID.randomUUID().toString();
        this.placementId = placementId;
        this.granteeAccountId = granteeAccountId;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPlacementId() { return placementId; }
    public String getGranteeAccountId() { return granteeAccountId; }
    public Instant getCreatedAt() { return createdAt; }
}
