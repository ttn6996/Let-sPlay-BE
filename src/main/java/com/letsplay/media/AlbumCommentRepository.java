package com.letsplay.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumCommentRepository extends JpaRepository<AlbumComment, String> {
    List<AlbumComment> findAllByAlbumIdAndStateOrderByCreatedAtAsc(String albumId, String state);
    long countByAlbumIdAndState(String albumId, String state);
}
