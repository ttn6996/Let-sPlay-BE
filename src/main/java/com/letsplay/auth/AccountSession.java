package com.letsplay.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account_sessions")
public class AccountSession {
    @Id @Column(length = 36) private String id;
    @Column(name = "account_id", nullable = false, length = 36) private String accountId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected AccountSession() {}
    public AccountSession(String id, String accountId, String tokenHash, Instant expiresAt) {
        this.id=id; this.accountId=accountId; this.tokenHash=tokenHash; this.expiresAt=expiresAt; this.createdAt=Instant.now();
    }
    public String getAccountId() { return accountId; }
    public String getId() { return id; }
    public Instant getExpiresAt() { return expiresAt; }
}
