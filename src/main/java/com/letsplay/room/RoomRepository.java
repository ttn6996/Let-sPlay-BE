package com.letsplay.room;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByInviteCode(String inviteCode);
    Optional<Room> findByCreationRequestId(String creationRequestId);
    boolean existsByInviteCode(String inviteCode);

    @Query("select room.id as id, room.revision as revision from Room room where room.id in :roomIds")
    List<RoomRevision> findRevisions(@Param("roomIds") Collection<String> roomIds);

    interface RoomRevision {
        String getId();
        long getRevision();
    }
}
