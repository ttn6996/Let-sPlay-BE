package com.letsplay.room;

import com.letsplay.realtime.RoomRealtimeHub;
import java.security.Security;
import java.util.UUID;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomPushService {
    private final RoomPushSubscriptionRepository subscriptions;
    private final RoomRealtimeHub realtime;
    private final String publicKey, privateKey, subject;

    public RoomPushService(RoomPushSubscriptionRepository subscriptions, RoomRealtimeHub realtime,
            @Value("${app.push.vapid-public-key:}") String publicKey,
            @Value("${app.push.vapid-private-key:}") String privateKey, @Value("${app.push.vapid-subject:mailto:admin@letsplay.app}") String subject) {
        this.subscriptions=subscriptions; this.realtime=realtime;
        this.publicKey=publicKey; this.privateKey=privateKey; this.subject=subject;
        if (Security.getProvider("BC")==null) Security.addProvider(new BouncyCastleProvider());
    }

    public String publicKey(){return publicKey;}

    @Transactional
    public void subscribe(String roomId,String membershipId,PushSubscriptionRequest request) {
        if(!isSupportedPushEndpoint(request.endpoint()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Push endpoint không hợp lệ hoặc chưa được hỗ trợ.");
        RoomPushSubscription subscription=subscriptions.findByRoomIdAndEndpoint(roomId,request.endpoint()).orElse(null);
        subscriptions.save(new RoomPushSubscription(subscription==null?UUID.randomUUID().toString():subscription.getId(),roomId,
                membershipId,request.endpoint(),request.keys().p256dh(),request.keys().auth()));
    }

    private static boolean isSupportedPushEndpoint(String value) {
        try {
            var uri=java.net.URI.create(value);
            String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getUserInfo()!=null||host.contains(":"))return false;
            String normalized=host.toLowerCase(java.util.Locale.ROOT);
            return normalized.equals("fcm.googleapis.com")||normalized.equals("web.push.apple.com")
                    ||normalized.endsWith(".push.services.mozilla.com");
        } catch(IllegalArgumentException exception){return false;}
    }

    @Transactional
    public void notifyProposal(String roomId,String proposalId,String title) {
        realtime.publishProposalCreated(roomId,proposalId,title);
        notifyRoom(roomId,"Đề xuất mới","Có thành viên vừa thêm: "+title);
    }

    public void notifyRoom(String roomId,String title,String body) {
        if(publicKey.isBlank()||privateKey.isBlank()) return;
        String safeTitle=escapeJson(title),safeBody=escapeJson(body);
        String payload="{\"title\":\""+safeTitle+"\",\"body\":\""+safeBody+"\",\"url\":\"/rooms/"+UUID.fromString(roomId)+"\"}";
        for(RoomPushSubscription subscription:subscriptions.findAllByRoomId(roomId)) {
            try {
                var push=new PushService(publicKey,privateKey,subject);
                var result=push.send(new Notification(subscription.getEndpoint(),subscription.getP256dhKey(),subscription.getAuthKey(),payload));
                if(result.getStatusLine().getStatusCode()==404||result.getStatusLine().getStatusCode()==410)
                    subscriptions.deleteByRoomIdAndEndpoint(roomId,subscription.getEndpoint());
            } catch(Exception exception) {
                // Push delivery failures must not roll back an already-created proposal.
            }
        }
    }

    private static String escapeJson(String value){return value.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
}
