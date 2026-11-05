package com.letsplay.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "room_memberships")
public class Membership {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "guest_id_hash", length = 64)
    private String guestIdHash;

    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(name = "access_token_hash", unique = true, length = 64)
    private String accessTokenHash;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(nullable = false, length = 10)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Membership() {}

    public Membership(String id, String roomId, String guestIdHash, String accessTokenHash,
                      String displayName, String role) {
        this.id = id;
        this.roomId = roomId;
        this.guestIdHash = guestIdHash;
        this.accessTokenHash = accessTokenHash;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public void refreshSession(String accessTokenHash, String displayName) {
        this.accessTokenHash = accessTokenHash;
        this.displayName = displayName;
    }

    public void claim(String accountId) { this.accountId = accountId; this.guestIdHash = null; this.accessTokenHash = null; }

    public Membership(String id, String roomId, String accountId, String displayName, String role, boolean accountMember) {
        this.id = id; this.roomId = roomId; this.accountId = accountId; this.displayName = displayName;
        this.role = role; this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getAccountId() { return accountId; }
}
