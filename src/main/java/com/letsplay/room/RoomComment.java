package com.letsplay.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "room_comments")
public class RoomComment {
    @Id @Column(length = 36) private String id;
    @Column(name = "room_id", nullable = false, length = 36) private String roomId;
    @Column(name = "membership_id", nullable = false, length = 36) private String membershipId;
    @Column(name = "proposal_id", length = 36) private String proposalId;
    @Column(name = "availability_slot_id", length = 36) private String availabilitySlotId;
    @Column(nullable = false, length = 500) private String content;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected RoomComment() {}

    public RoomComment(String id, String roomId, String membershipId, String proposalId, String availabilitySlotId, String content) {
        this.id = id;
        this.roomId = roomId;
        this.membershipId = membershipId;
        this.proposalId = proposalId;
        this.availabilitySlotId = availabilitySlotId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getMembershipId() { return membershipId; }
    public String getProposalId() { return proposalId; }
    public String getAvailabilitySlotId() { return availabilitySlotId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
