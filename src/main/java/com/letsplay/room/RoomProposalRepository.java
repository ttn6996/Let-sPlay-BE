package com.letsplay.room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoomProposalRepository extends JpaRepository<RoomProposal,String> {
    List<RoomProposal> findAllByRoomIdOrderByCreatedAtAsc(String roomId);
}
