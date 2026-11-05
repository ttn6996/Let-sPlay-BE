package com.letsplay.room;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomAvailabilityVoteRepository extends JpaRepository<RoomAvailabilityVote,String> {
    List<RoomAvailabilityVote> findAllBySlotIdIn(Collection<String> slotIds);
    void deleteAllBySlotIdIn(Collection<String> slotIds);
}
