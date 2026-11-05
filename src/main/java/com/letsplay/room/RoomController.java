package com.letsplay.room;

import com.letsplay.realtime.RoomRealtimeHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms · Phòng", description = "Tạo phòng, xem trước mã mời và tham gia phòng.")
public class RoomController {
    private final RoomService service;
    private final RoomRealtimeHub realtimeHub;

    public RoomController(RoomService service, RoomRealtimeHub realtimeHub) {
        this.service = service;
        this.realtimeHub = realtimeHub;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo phòng", description = "Tạo phòng ở giai đoạn góp ý và cấp quyền chủ phòng cho phiên khách hiện tại.")
    public RoomResponse create(@Valid @RequestBody RoomRequests.CreateRoom request) {
        return service.create(request);
    }

    @PostMapping("/join")
    @Operation(summary = "Tham gia phòng", description = "Tạo hoặc tiếp tục membership bằng guestId và mã mời hợp lệ.")
    public RoomResponse join(@Valid @RequestBody RoomRequests.JoinRoom request) {
        return service.join(request);
    }

    @PostMapping("/preview")
    @Operation(summary = "Xem trước phòng", description = "Kiểm tra mã mời trước khi yêu cầu người dùng nhập tên hiển thị.")
    public RoomPreview preview(@Valid @RequestBody RoomRequests.PreviewRoom request) {
        return service.preview(request);
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Đọc thông tin phòng", description = "Yêu cầu Bearer access token đã cấp khi tạo hoặc tham gia phòng.")
    @SecurityRequirement(name = "GuestSession")
    public RoomResponse get(@PathVariable String roomId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
        return service.get(roomId, token);
    }

    @GetMapping(value = "/{roomId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Nghe thay đổi phòng theo thời gian thực", description = "SSE có kiểm tra membership. Gửi Last-Event-ID bằng revision gần nhất để nhận bù thay đổi khi reconnect.")
    @SecurityRequirement(name = "GuestSession")
    public SseEmitter events(
            @PathVariable String roomId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : null;
        RoomResponse snapshot = service.get(roomId, token);
        long sinceRevision = snapshot.revision();
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                sinceRevision = Math.min(Long.parseLong(lastEventId), snapshot.revision());
            } catch (NumberFormatException exception) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Last-Event-ID phải là revision hợp lệ.");
            }
        }
        return realtimeHub.subscribe(roomId, Math.max(0, sinceRevision), snapshot.revision());
    }
}
