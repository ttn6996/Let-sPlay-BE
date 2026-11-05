package com.letsplay.realtime;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class RoomRealtimeHub {
    private final ConcurrentHashMap<String, RoomStream> streams = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String roomId, long sinceRevision, long currentRevision) {
        SseEmitter emitter = new SseEmitter(0L);
        RoomStream stream = streams.computeIfAbsent(roomId, ignored -> new RoomStream(currentRevision));
        stream.emitters.add(emitter);
        Runnable remove = () -> removeEmitter(roomId, stream, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            emitter.complete();
            remove.run();
        });
        emitter.onError(ignored -> remove.run());

        synchronized (stream) {
            long latestRevision = Math.max(currentRevision, stream.lastRevision);
            send(emitter, SseEmitter.event().name("ready").id(Long.toString(latestRevision))
                    .data(new RoomReady(roomId, latestRevision)));
            if (latestRevision > sinceRevision) {
                if (latestRevision > stream.lastRevision) {
                    stream.lastRevision = latestRevision;
                    broadcast(stream, new RoomUpdate(roomId, latestRevision, Instant.now()));
                } else {
                    sendUpdate(emitter, roomId, latestRevision);
                }
            }
        }
        return emitter;
    }

    public void publishRevision(String roomId, long revision) {
        RoomStream stream = streams.get(roomId);
        if (stream == null) return;
        synchronized (stream) {
            if (revision <= stream.lastRevision) return;
            stream.lastRevision = revision;
            broadcast(stream, new RoomUpdate(roomId, revision, Instant.now()));
        }
    }

    public void keepAlive() {
        streams.forEach((roomId, stream) -> {
            synchronized (stream) {
                for (SseEmitter emitter : stream.emitters) {
                    try {
                        emitter.send(SseEmitter.event().comment("keep-alive"));
                    } catch (IOException exception) {
                        emitter.completeWithError(exception);
                        removeEmitter(roomId, stream, emitter);
                    }
                }
            }
        });
    }

    public Set<String> activeRoomIds() { return Set.copyOf(streams.keySet()); }

    private void broadcast(RoomStream stream, RoomUpdate update) {
        for (SseEmitter emitter : stream.emitters) sendUpdate(emitter, update);
    }

    private static void sendUpdate(SseEmitter emitter, String roomId, long revision) {
        sendUpdate(emitter, new RoomUpdate(roomId, revision, Instant.now()));
    }

    private static void sendUpdate(SseEmitter emitter, RoomUpdate update) {
        send(emitter, SseEmitter.event().name("room.updated").id(Long.toString(update.revision())).data(update));
    }

    private static void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(String roomId, RoomStream stream, SseEmitter emitter) {
        stream.emitters.remove(emitter);
        if (stream.emitters.isEmpty()) streams.remove(roomId, stream);
    }

    public record RoomReady(String roomId, long revision) {}
    public record RoomUpdate(String roomId, long revision, Instant changedAt) {}

    private static final class RoomStream {
        private final CopyOnWriteArraySet<SseEmitter> emitters = new CopyOnWriteArraySet<>();
        private long lastRevision;

        private RoomStream(long lastRevision) { this.lastRevision = lastRevision; }
    }
}
