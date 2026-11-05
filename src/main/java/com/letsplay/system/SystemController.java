package com.letsplay.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/system")
@Tag(name = "System · Health & Cron", description = "Health check, ping nội bộ và ping dành cho cron bên thứ ba.")
public class SystemController {
    private final SystemService service;
    private final SystemPingKey pingKey;

    public SystemController(SystemService service, SystemPingKey pingKey) {
        this.service = service;
        this.pingKey = pingKey;
    }

    @GetMapping("/health")
    @Operation(summary = "Kiểm tra API và database", description = "Dùng làm health check endpoint khi deploy BE.")
    public HealthResponse health() {
        return new HealthResponse("ok", service.databaseReady());
    }

    @GetMapping("/self-ping")
    @Operation(summary = "Ghi nhận self-ping nội bộ", description = "Được backend gọi định kỳ mỗi 5 phút. Cần header nội bộ do tiến trình tự sinh.")
    public SystemService.PingResult selfPing(
            @Parameter(description = "Khóa ngẫu nhiên chỉ tồn tại trong tiến trình BE", required = true)
            @RequestHeader("X-Internal-Ping-Key") String key) {
        if (!pingKey.matches(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal ping key không hợp lệ.");
        }
        return service.recordPing(SystemService.PingSource.SELF);
    }

    @GetMapping("/cron-ping")
    @Operation(summary = "Cron ping công khai", description = "Endpoint mở cho cron-job.org hoặc dịch vụ tương tự. Mỗi request ghi thời điểm ping và tăng bộ đếm trong database.")
    public SystemService.PingResult cronPing() {
        return service.recordPing(SystemService.PingSource.CRON);
    }

    public record HealthResponse(String status, boolean database) {}
}
