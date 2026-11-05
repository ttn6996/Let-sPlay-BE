package com.letsplay.room;

import java.time.LocalDate;
import java.time.LocalTime;

public record RoomPreview(String id, String name, String inviteCode, String status,String phase,long memberCount,
                          LocalDate eventDate, LocalTime eventTime, String region) {
    static RoomPreview from(Room room,long memberCount) {
        return new RoomPreview(room.getId(), room.getName(), room.getInviteCode(), room.getStatus(),room.getPhase(),memberCount,
                room.getEventDate(), room.getEventTime(), room.getRegion());
    }
}
