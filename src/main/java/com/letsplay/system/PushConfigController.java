package com.letsplay.system;

import com.letsplay.room.RoomPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@Tag(name="02 - System and Cron")
public class PushConfigController {
    private final RoomPushService push;
    public PushConfigController(RoomPushService push){this.push=push;}
    @GetMapping("/public-config")
    @Operation(summary="Đọc cấu hình công khai",description="Trả khóa VAPID công khai để trình duyệt đăng ký nhận Web Push.")
    public PublicConfig publicConfig(){return new PublicConfig(push.publicKey());}
    public record PublicConfig(String vapidPublicKey){}
}
