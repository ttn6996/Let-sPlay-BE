package com.letsplay.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name="room_availability_votes")
public class RoomAvailabilityVote {
    @Id @Column(length=36) private String id;
    @Column(name="slot_id",nullable=false,length=36) private String slotId;
    @Column(name="membership_id",nullable=false,length=36) private String membershipId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected RoomAvailabilityVote(){}
    public RoomAvailabilityVote(String id,String slotId,String membershipId){this.id=id;this.slotId=slotId;this.membershipId=membershipId;this.createdAt=Instant.now();}
    public String getSlotId(){return slotId;} public String getMembershipId(){return membershipId;}
}
