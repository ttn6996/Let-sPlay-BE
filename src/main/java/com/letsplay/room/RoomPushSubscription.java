package com.letsplay.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "room_push_subscriptions")
public class RoomPushSubscription {
    @Id @Column(length = 36) private String id;
    @Column(name = "room_id", nullable = false, length = 36) private String roomId;
    @Column(name = "membership_id", nullable = false, length = 36) private String membershipId;
    @Column(nullable = false, columnDefinition = "text") private String endpoint;
    @Column(name = "p256dh_key", nullable = false, columnDefinition = "text") private String p256dhKey;
    @Column(name = "auth_key", nullable = false, length = 200) private String authKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected RoomPushSubscription() {}
    public RoomPushSubscription(String id, String roomId, String membershipId, String endpoint, String p256dhKey, String authKey) {
        this.id=id; this.roomId=roomId; this.membershipId=membershipId; this.endpoint=endpoint;
        this.p256dhKey=p256dhKey; this.authKey=authKey; this.createdAt=Instant.now();
    }
    public String getId(){return id;} public String getRoomId(){return roomId;} public String getMembershipId(){return membershipId;}
    public String getEndpoint(){return endpoint;} public String getP256dhKey(){return p256dhKey;} public String getAuthKey(){return authKey;}
}
