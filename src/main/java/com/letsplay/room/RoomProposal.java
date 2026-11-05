package com.letsplay.room;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name="room_proposals")
public class RoomProposal {
    @Id @Column(length=36) private String id;
    @Column(name="room_id",nullable=false,length=36) private String roomId;
    @Column(name="created_by_membership_id",nullable=false,length=36) private String createdByMembershipId;
    @Column(nullable=false,length=120) private String title;
    @Column(length=500) private String description;
    @Column(name="scheduled_time") private LocalDateTime scheduledTime;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected RoomProposal() {}
    public RoomProposal(String id,String roomId,String creator,String title,String description,LocalDateTime scheduledTime) {
        this.id=id;this.roomId=roomId;this.createdByMembershipId=creator;this.title=title;this.description=description;this.scheduledTime=scheduledTime;this.createdAt=Instant.now();
    }
    public String getId(){return id;} public String getRoomId(){return roomId;} public String getCreatedByMembershipId(){return createdByMembershipId;}
    public String getTitle(){return title;} public String getDescription(){return description;} public LocalDateTime getScheduledTime(){return scheduledTime;} public Instant getCreatedAt(){return createdAt;}
}
