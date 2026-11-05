package com.letsplay.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;

    @Column(name = "creation_request_id", nullable = false, unique = true, length = 36)
    private String creationRequestId;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    @Column(length = 120)
    private String region;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private long revision;

    protected Room() {}

    public Room(String id, String name, String inviteCode, String creationRequestId,
                LocalDate eventDate, LocalTime eventTime, String region) {
        this.id = id;
        this.name = name;
        this.inviteCode = inviteCode;
        this.creationRequestId = creationRequestId;
        this.phase = "IDEAS";
        this.status = "OPEN";
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.region = region;
        this.createdAt = Instant.now();
        this.revision = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
    public String getCreationRequestId() { return creationRequestId; }
    public String getPhase() { return phase; }
    public String getStatus() { return status; }
    public LocalDate getEventDate() { return eventDate; }
    public LocalTime getEventTime() { return eventTime; }
    public String getRegion() { return region; }
    public Instant getCreatedAt() { return createdAt; }
    public long getRevision() { return revision; }
    public void bumpRevision() { this.revision++; }
}
