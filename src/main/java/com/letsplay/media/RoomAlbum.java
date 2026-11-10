package com.letsplay.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "room_albums")
public class RoomAlbum {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "room_id", nullable = false, unique = true, length = 36)
    private String roomId;

    @Column(name = "policy_mode", nullable = false, length = 32)
    private String policyMode = "OPEN";

    @Column(name = "window_hours")
    private Integer windowHours;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(nullable = false)
    private int revision = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoomAlbum() {}

    public RoomAlbum(String roomId) {
        this.id = UUID.randomUUID().toString();
        this.roomId = roomId;
        this.policyMode = "OPEN";
        this.revision = 1;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getPolicyMode() { return policyMode; }
    public Integer getWindowHours() { return windowHours; }
    public Instant getEndedAt() { return endedAt; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public int getRevision() { return revision; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isWritable(Instant now) {
        if ("VIEW_ONLY".equalsIgnoreCase(policyMode)) {
            return false;
        }
        if ("WINDOWED".equalsIgnoreCase(policyMode)) {
            if (deadlineAt != null && now.isAfter(deadlineAt)) {
                return false;
            }
        }
        return true;
    }

    public void updatePolicy(String mode, Integer hours, Instant roomEndedAt) {
        this.policyMode = mode;
        this.windowHours = hours;
        this.updatedAt = Instant.now();
        this.revision++;

        if ("WINDOWED".equalsIgnoreCase(mode) && hours != null && hours > 0) {
            Instant baseTime = roomEndedAt != null ? roomEndedAt : this.endedAt;
            if (baseTime != null) {
                this.deadlineAt = baseTime.plus(hours, ChronoUnit.HOURS);
            }
        } else if ("VIEW_ONLY".equalsIgnoreCase(mode)) {
            this.deadlineAt = roomEndedAt != null ? roomEndedAt : Instant.now();
        } else {
            this.deadlineAt = null;
        }
    }

    public void onRoomEnded(Instant roomEndedAt) {
        this.endedAt = roomEndedAt;
        this.updatedAt = Instant.now();
        this.revision++;
        if ("WINDOWED".equalsIgnoreCase(policyMode) && windowHours != null && windowHours > 0) {
            this.deadlineAt = roomEndedAt.plus(windowHours, ChronoUnit.HOURS);
        } else if ("VIEW_ONLY".equalsIgnoreCase(policyMode)) {
            this.deadlineAt = roomEndedAt;
        }
    }

    public void onRoomReopened() {
        this.endedAt = null;
        this.deadlineAt = null;
        this.updatedAt = Instant.now();
        this.revision++;
    }
}
