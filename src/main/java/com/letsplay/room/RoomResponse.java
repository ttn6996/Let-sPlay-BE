package com.letsplay.room;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

public record RoomResponse(
        String id,
        String name,
        String inviteCode,
        String phase,
        String status,
        LocalDate eventDate,
        LocalTime eventTime,
        String region,
        Instant createdAt,
        long revision,
        long memberCount,
        Member member,
        List<Member> members,
        String accessToken,
        String selectedProposalId,
        List<Proposal> proposals,
        List<AvailabilitySlot> availabilitySlots,
        String availabilityTitle) {
    public record Member(String id, String displayName, String role,boolean online) {}
    public record Voter(String displayName) {}
    public record Proposal(String id,boolean createdByMe,String title,String description,LocalDateTime scheduledTime,Instant createdAt,long voteCount,List<Voter> voters,boolean votedByMe,boolean completed) {}
    public record AvailabilitySlot(String id,LocalDateTime startsAt,long availableCount,boolean availableByMe,boolean selected) {}

}
