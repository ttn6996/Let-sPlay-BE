package com.letsplay.room;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomCommentRepository extends JpaRepository<RoomComment, String> {
    List<RoomComment> findAllByProposalIdInOrderByCreatedAtAsc(Collection<String> proposalIds);
    List<RoomComment> findAllByAvailabilitySlotIdInOrderByCreatedAtAsc(Collection<String> slotIds);
}
