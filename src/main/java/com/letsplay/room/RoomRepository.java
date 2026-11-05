package com.letsplay.room;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByInviteCode(String inviteCode);
    Optional<Room> findByCreationRequestId(String creationRequestId);
    boolean existsByInviteCode(String inviteCode);
}
