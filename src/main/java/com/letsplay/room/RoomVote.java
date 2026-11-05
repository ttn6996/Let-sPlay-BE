package com.letsplay.room;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="room_votes",uniqueConstraints=@UniqueConstraint(name="uq_room_vote_proposal_member",columnNames={"proposal_id","membership_id"}))
public class RoomVote {
    @Id @Column(length=36) private String id;
    @Column(name="proposal_id",nullable=false,length=36) private String proposalId;
    @Column(name="membership_id",nullable=false,length=36) private String membershipId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected RoomVote() {}
    public RoomVote(String id,String proposalId,String membershipId){this.id=id;this.proposalId=proposalId;this.membershipId=membershipId;this.createdAt=Instant.now();}
    public String getProposalId(){return proposalId;} public String getMembershipId(){return membershipId;}
}
