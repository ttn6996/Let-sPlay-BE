package com.letsplay.system;

import com.letsplay.room.RoomPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/system")
@Tag(name="02 - System and Cron")
public class PushConfigController {
    private final RoomPushService push;
    private final String googleClientId;
    public PushConfigController(RoomPushService push,@Value("${app.google.client-id:}") String googleClientId){this.push=push;this.googleClientId=googleClientId;}
    @GetMapping("/public-config")
    @Operation(summary="Đọc cấu hình công khai",description="Trả cấu hình client không bí mật cho Google Sign-In và Web Push.")
    public PublicConfig publicConfig(){return new PublicConfig(push.publicKey(),googleClientId);}
    public record PublicConfig(String vapidPublicKey,String googleClientId){}
}
