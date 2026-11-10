package com.letsplay.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
    Optional<MediaAsset> findByIdAndOwnerAccountId(String id, String ownerAccountId);
    List<MediaAsset> findAllByOwnerAccountIdOrderByCreatedAtDesc(String ownerAccountId);
}
