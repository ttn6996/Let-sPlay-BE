package com.letsplay.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moment_placements")
public class MomentPlacement {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "moment_id", nullable = false, length = 36)
    private String momentId;

    @Column(nullable = false, length = 32)
    private String kind; // "PERSONAL" or "ROOM"

    @Column(name = "room_id", length = 36)
    private String roomId;

    @Column(name = "album_id", length = 36)
    private String albumId;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE"; // "ACTIVE" or "REMOVED"

    @Column(name = "removed_by_id", length = 36)
    private String removedById;

    @Column(name = "removed_reason", length = 500)
    private String removedReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected MomentPlacement() {}

    public static MomentPlacement forPersonal(String momentId) {
        MomentPlacement p = new MomentPlacement();
        p.id = UUID.randomUUID().toString();
        p.momentId = momentId;
        p.kind = "PERSONAL";
        p.status = "ACTIVE";
        p.createdAt = Instant.now();
        return p;
    }

    public static MomentPlacement forRoom(String momentId, String roomId, String albumId) {
        MomentPlacement p = new MomentPlacement();
        p.id = UUID.randomUUID().toString();
        p.momentId = momentId;
        p.kind = "ROOM";
        p.roomId = roomId;
        p.albumId = albumId;
        p.status = "ACTIVE";
        p.createdAt = Instant.now();
        return p;
    }

    public String getId() { return id; }
    public String getMomentId() { return momentId; }
    public String getKind() { return kind; }
    public String getRoomId() { return roomId; }
    public String getAlbumId() { return albumId; }
    public String getStatus() { return status; }
    public String getRemovedById() { return removedById; }
    public String getRemovedReason() { return removedReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRemovedAt() { return removedAt; }

    public void remove(String removedById, String reason) {
        this.status = "REMOVED";
        this.removedById = removedById;
        this.removedReason = reason;
        this.removedAt = Instant.now();
    }
}
