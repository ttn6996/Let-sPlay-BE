package com.letsplay.room;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomAvailabilitySlotRepository extends JpaRepository<RoomAvailabilitySlot,String> {
    List<RoomAvailabilitySlot> findAllByRoomIdOrderByStartsAtAsc(String roomId);
    void deleteAllByRoomId(String roomId);
}
