package com.letsplay.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MomentPlacementRepository extends JpaRepository<MomentPlacement, String> {
    List<MomentPlacement> findAllByAlbumIdAndStatusOrderByCreatedAtDesc(String albumId, String status);
    List<MomentPlacement> findAllByMomentId(String momentId);
    List<MomentPlacement> findAllByMomentIdAndStatus(String momentId, String status);
    Optional<MomentPlacement> findByMomentIdAndKindAndStatus(String momentId, String kind, String status);
    long countByAlbumIdAndStatus(String albumId, String status);
}
