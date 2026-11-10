package com.letsplay.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalGrantRepository extends JpaRepository<PersonalGrant, String> {
    List<PersonalGrant> findAllByGranteeAccountIdOrderByCreatedAtDesc(String granteeAccountId);
    List<PersonalGrant> findAllByPlacementId(String placementId);
    boolean existsByPlacementIdAndGranteeAccountId(String placementId, String granteeAccountId);
}
