package com.letsplay.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementCommentRepository extends JpaRepository<PlacementComment, String> {
    List<PlacementComment> findAllByPlacementIdAndStateOrderByCreatedAtAsc(String placementId, String state);
    long countByPlacementIdAndState(String placementId, String state);
}
