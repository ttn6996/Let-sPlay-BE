package com.letsplay.room;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name="room_proposals")
public class RoomProposal {
    @Id @Column(length=36) private String id;
    @Column(name="room_id",nullable=false,length=36) private String roomId;
    @Column(name="created_by_membership_id",length=36) private String createdByMembershipId;
    @Column(nullable=false,length=120) private String title;
    @Column(length=500) private String description;
    @Column(name="scheduled_time") private LocalDateTime scheduledTime;
    @Column(length=200) private String location;
    @Column(name="estimated_cost_per_person") private Long estimatedCostPerPerson;
    private Double latitude;
    private Double longitude;
    @Column(nullable=false) private boolean completed;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected RoomProposal() {}
    public RoomProposal(String id,String roomId,String creator,String title,String description,LocalDateTime scheduledTime,String location,Long estimatedCostPerPerson,Double latitude,Double longitude) {
        this.id=id;this.roomId=roomId;this.createdByMembershipId=creator;this.title=title;this.description=description;this.scheduledTime=scheduledTime;this.location=location;this.estimatedCostPerPerson=estimatedCostPerPerson;this.latitude=latitude;this.longitude=longitude;this.createdAt=Instant.now();
    }
    public String getId(){return id;} public String getRoomId(){return roomId;} public String getCreatedByMembershipId(){return createdByMembershipId;}
    public String getTitle(){return title;} public String getDescription(){return description;} public LocalDateTime getScheduledTime(){return scheduledTime;} public Instant getCreatedAt(){return createdAt;} public String getLocation(){return location;} public Long getEstimatedCostPerPerson(){return estimatedCostPerPerson;} public Double getLatitude(){return latitude;} public Double getLongitude(){return longitude;}
    public void update(String title,String description,LocalDateTime scheduledTime,String location,Long estimatedCostPerPerson,Double latitude,Double longitude){this.title=title;this.description=description;this.scheduledTime=scheduledTime;this.location=location;this.estimatedCostPerPerson=estimatedCostPerPerson;this.latitude=latitude;this.longitude=longitude;}
    public boolean isCompleted(){return completed;}
    public void setCompleted(boolean completed){this.completed=completed;}
}
