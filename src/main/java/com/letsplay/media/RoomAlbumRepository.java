package com.letsplay.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomAlbumRepository extends JpaRepository<RoomAlbum, String> {
    Optional<RoomAlbum> findByRoomId(String roomId);
}
