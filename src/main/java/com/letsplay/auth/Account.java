package com.letsplay.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", length = 100) private String passwordHash;
    @Column(name = "google_subject", unique = true, length = 255) private String googleSubject;
    @Column(name = "display_name", nullable = false, length = 60) private String displayName;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected Account() {}
    public Account(String id, String email, String passwordHash, String displayName) {
        this.id = id; this.email = email; this.passwordHash = passwordHash; this.displayName = displayName; this.createdAt = Instant.now();
    }
    public Account(String id, String email, String passwordHash, String displayName, String googleSubject) {
        this(id,email,passwordHash,displayName); this.googleSubject=googleSubject;
    }
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getGoogleSubject() { return googleSubject; }
    public void linkGoogle(String subject) { this.googleSubject=subject; }
    String getPasswordHash() { return passwordHash; }
}
