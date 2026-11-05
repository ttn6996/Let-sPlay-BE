package com.letsplay.room;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, String> {
    long countByRoomId(String roomId);
    Optional<Membership> findByRoomIdAndGuestIdHash(String roomId, String guestIdHash);
    Optional<Membership> findByRoomIdAndAccessTokenHash(String roomId, String accessTokenHash);
}
