package com.letsplay.room;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPushSubscriptionRepository extends JpaRepository<RoomPushSubscription,String> {
    List<RoomPushSubscription> findAllByRoomId(String roomId);
    Optional<RoomPushSubscription> findByRoomIdAndEndpoint(String roomId,String endpoint);
    void deleteByRoomIdAndEndpoint(String roomId,String endpoint);
}
