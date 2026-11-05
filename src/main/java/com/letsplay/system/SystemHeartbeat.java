package com.letsplay.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "system_heartbeat")
public class SystemHeartbeat {
    @Id
    @Column(length = 16)
    private String id;

    @Column(name = "last_ping_at", nullable = false)
    private Instant lastPingAt;

    @Column(name = "ping_count", nullable = false)
    private long pingCount;

    protected SystemHeartbeat() {}

    public String getId() { return id; }
    public Instant getLastPingAt() { return lastPingAt; }
    public long getPingCount() { return pingCount; }
}
