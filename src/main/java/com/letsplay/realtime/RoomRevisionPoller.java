package com.letsplay.realtime;

import com.letsplay.room.RoomRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoomRevisionPoller {
    private static final Logger logger = LoggerFactory.getLogger(RoomRevisionPoller.class);
    private final RoomRepository rooms;
    private final RoomRealtimeHub hub;

    public RoomRevisionPoller(RoomRepository rooms, RoomRealtimeHub hub) {
        this.rooms = rooms;
        this.hub = hub;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    @Transactional(readOnly = true)
    public void publishChangedRooms() {
        Set<String> activeRoomIds = hub.activeRoomIds();
        if (activeRoomIds.isEmpty()) return;
        try {
            for (RoomRepository.RoomRevision room : rooms.findRevisions(activeRoomIds)) {
                hub.publishRevision(room.getId(), room.getRevision());
            }
        } catch (Exception exception) {
            logger.debug("Room revision polling failed: {}", exception.getClass().getSimpleName());
        }
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 15000)
    public void keepStreamsAlive() { hub.keepAlive(); }
}
