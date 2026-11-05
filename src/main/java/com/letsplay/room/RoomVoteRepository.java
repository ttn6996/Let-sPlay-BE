package com.letsplay.room;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoomVoteRepository extends JpaRepository<RoomVote,String> {
    List<RoomVote> findAllByProposalIdIn(Collection<String> proposalIds);
    Optional<RoomVote> findByProposalIdAndMembershipId(String proposalId,String membershipId);
}
