package com.letsplay.room;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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
        String accessToken) {
    public record Member(String id, String displayName, String role) {}

    static RoomResponse from(Room room, Membership member, String accessToken, long memberCount) {
        return new RoomResponse(room.getId(), room.getName(), room.getInviteCode(), room.getPhase(),
                room.getStatus(), room.getEventDate(), room.getEventTime(), room.getRegion(),
                room.getCreatedAt(), room.getRevision(), memberCount,
                new Member(member.getId(), member.getDisplayName(), member.getRole()),
                accessToken);
    }
}
