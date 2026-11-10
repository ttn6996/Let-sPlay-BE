package com.letsplay.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MomentRepository extends JpaRepository<Moment, String> {
    Optional<Moment> findByIdAndOwnerAccountId(String id, String ownerAccountId);
    Optional<Moment> findByMediaAssetId(String mediaAssetId);
    Optional<Moment> findByOwnerAccountIdAndClientMutationId(String ownerAccountId, String clientMutationId);
    List<Moment> findAllByOwnerAccountIdAndStatusOrderByCreatedAtDesc(String ownerAccountId, String status);
}
