package com.letsplay.room;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="room_votes",uniqueConstraints=@UniqueConstraint(name="uq_room_vote_proposal_member",columnNames={"proposal_id","membership_id"}))
public class RoomVote {
    @Id @Column(length=36) private String id;
    @Column(name="proposal_id",nullable=false,length=36) private String proposalId;
    @Column(name="membership_id",nullable=false,length=36) private String membershipId;
    @Column(name="vote_value",nullable=false) private int voteValue = 2;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected RoomVote() {}
    public RoomVote(String id,String proposalId,String membershipId){
        this(id, proposalId, membershipId, 2);
    }
    public RoomVote(String id,String proposalId,String membershipId, int voteValue){
        this.id=id;this.proposalId=proposalId;this.membershipId=membershipId;this.voteValue=voteValue;this.createdAt=Instant.now();
    }
    public String getId(){return id;}
    public String getProposalId(){return proposalId;}
    public String getMembershipId(){return membershipId;}
    public int getVoteValue(){return voteValue;}
    public void setVoteValue(int voteValue){this.voteValue=voteValue;}
}
