package com.letsplay.realtime;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class RoomRealtimeHub {
    private final ConcurrentHashMap<String, RoomStream> streams = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String roomId,String membershipId,long sinceRevision,long currentRevision) {
        SseEmitter emitter=new SseEmitter(0L);
        RoomStream stream=streams.compute(roomId,(key,current) -> {
            RoomStream active=current==null?new RoomStream(currentRevision):current;
            synchronized(active) { active.emitters.put(emitter,membershipId); }
            return active;
        });
        Runnable cleanup=() -> cleanup(roomId,stream,emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        synchronized(stream) {
            long latest=Math.max(currentRevision,stream.lastRevision);
            if(!send(roomId,stream,emitter,SseEmitter.event().name("ready").id(Long.toString(latest)).data(new RoomReady(roomId,latest)))) return emitter;
            if(sinceRevision<latest) {
                RoomUpdate update=new RoomUpdate(roomId,latest,Instant.now());
                if(latest>stream.lastRevision){stream.lastRevision=latest;for(SseEmitter connected:stream.emitters.keySet())sendUpdate(roomId,stream,connected,update);}
                else sendUpdate(roomId,stream,emitter,update);
            }
            publishPresence(roomId,stream,membershipId,true);
        }
        return emitter;
    }

    public void publishRevision(String roomId,long revision) {
        RoomStream stream=streams.get(roomId); if(stream==null)return;
        synchronized(stream) {
            if(revision<=stream.lastRevision)return;
            stream.lastRevision=revision;
            RoomUpdate update=new RoomUpdate(roomId,revision,Instant.now());
            for(SseEmitter emitter:stream.emitters.keySet()) sendUpdate(roomId,stream,emitter,update);
        }
    }

    public void keepAlive() {
        streams.forEach((roomId,stream) -> {
            synchronized(stream) {
                for(SseEmitter emitter:stream.emitters.keySet()) {
                    try { emitter.send(SseEmitter.event().comment("keep-alive")); }
                    catch(IOException|IllegalStateException exception){cleanup(roomId,stream,emitter);}
                }
            }
        });
    }

    public Set<String> activeRoomIds(){return streams.entrySet().stream().filter(entry -> !entry.getValue().emitters.isEmpty()).map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());}
    public Set<String> onlineMembershipIds(String roomId){
        RoomStream stream=streams.get(roomId); if(stream==null)return Set.of();
        synchronized(stream){return Set.copyOf(stream.emitters.values());}
    }
    public void disconnectMembership(String roomId,String membershipId){
        RoomStream stream=streams.get(roomId);if(stream==null)return;
        stream.emitters.forEach((emitter,id)->{if(id.equals(membershipId))cleanup(roomId,stream,emitter);});
    }
    public void closeRoom(String roomId){
        RoomStream stream=streams.get(roomId);if(stream==null)return;
        stream.emitters.keySet().forEach(emitter->cleanup(roomId,stream,emitter));
    }
    private void sendUpdate(String roomId,RoomStream stream,SseEmitter emitter,RoomUpdate update){
        send(roomId,stream,emitter,SseEmitter.event().name("room.updated").id(Long.toString(update.revision())).data(update));
    }
    private boolean send(String roomId,RoomStream stream,SseEmitter emitter,SseEmitter.SseEventBuilder event){
        try { emitter.send(event); return true; }
        catch(IOException|IllegalStateException exception){cleanup(roomId,stream,emitter);return false;}
    }
    private void cleanup(String roomId,RoomStream stream,SseEmitter emitter){
        if(Thread.holdsLock(stream)) CompletableFuture.runAsync(() -> cleanupNow(roomId,stream,emitter));
        else cleanupNow(roomId,stream,emitter);
    }
    private void cleanupNow(String roomId,RoomStream stream,SseEmitter emitter){
        String membershipId=stream.emitters.get(emitter);
        streams.computeIfPresent(roomId,(key,current) -> {
            if(current!=stream)return current;
            synchronized(current){current.emitters.remove(emitter);return current.emitters.isEmpty()?null:current;}
        });
        if(membershipId!=null&&!onlineMembershipIds(roomId).contains(membershipId)) {
            synchronized(stream){publishPresence(roomId,stream,membershipId,false);}
        }
        try { emitter.complete(); } catch(IllegalStateException ignored) { }
    }
    private void publishPresence(String roomId,RoomStream stream,String membershipId,boolean online){
        PresenceUpdate update=new PresenceUpdate(roomId,membershipId,online,Set.copyOf(stream.emitters.values()),stream.lastRevision,Instant.now());
        var event=SseEmitter.event().name("room.presence").id(Long.toString(stream.lastRevision)).data(update);
        for(SseEmitter connected:stream.emitters.keySet())send(roomId,stream,connected,event);
    }
    public record RoomReady(String roomId,long revision){}
    public record RoomUpdate(String roomId,long revision,Instant changedAt){}
    public record PresenceUpdate(String roomId,String membershipId,boolean online,Set<String> onlineMembershipIds,long revision,Instant changedAt){}
    private static final class RoomStream {
        private final ConcurrentHashMap<SseEmitter,String> emitters=new ConcurrentHashMap<>();
        private long lastRevision;
        private RoomStream(long revision){lastRevision=revision;}
    }
}
