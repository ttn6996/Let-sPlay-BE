package com.letsplay.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name="room_availability_slots")
public class RoomAvailabilitySlot {
    @Id @Column(length=36) private String id;
    @Column(name="room_id",nullable=false,length=36) private String roomId;
    @Column(name="starts_at",nullable=false) private LocalDateTime startsAt;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected RoomAvailabilitySlot(){}
    public RoomAvailabilitySlot(String id,String roomId,LocalDateTime startsAt){this.id=id;this.roomId=roomId;this.startsAt=startsAt;this.createdAt=Instant.now();}
    public String getId(){return id;} public String getRoomId(){return roomId;} public LocalDateTime getStartsAt(){return startsAt;}
}
