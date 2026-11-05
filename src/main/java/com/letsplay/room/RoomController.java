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
    public RoomResponse create(@Valid @RequestBody RoomRequests.CreateRoom request, @RequestHeader(value="Authorization",required=false) String authorization) {
        String token=bearer(authorization);
        return token==null?service.create(request):service.createForAccount(request,token);
    }

    @PostMapping("/join")
    @Operation(summary = "Tham gia phòng", description = "Tạo hoặc tiếp tục membership bằng guestId và mã mời hợp lệ.")
    public RoomResponse join(@Valid @RequestBody RoomRequests.JoinRoom request, @RequestHeader(value="Authorization",required=false) String authorization) {
        String token=bearer(authorization);
        return token==null?service.join(request):service.joinForAccount(request,token);
    }

    @PostMapping("/{roomId}/claim")
    @Operation(summary="Lưu phòng khách vào tài khoản",description="Gắn membership của phiên khách vào tài khoản để mở phòng trên thiết bị khác.")
    public void claim(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String authorization,
                      @RequestHeader(value="X-Guest-Token",required=false) String guestToken,
                      @RequestHeader(value="X-Guest-Access-Token",required=false) String legacyGuestToken) {
        service.claim(roomId,bearer(authorization),guestToken!=null?guestToken:legacyGuestToken);
    }

    @PostMapping("/{roomId}/proposals")
    @Operation(summary="Thêm đề xuất",description="Thêm hoạt động hoặc khung giờ khi phòng đang ở giai đoạn góp ý.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse addProposal(@PathVariable String roomId,@Valid @RequestBody RoomRequests.CreateProposal request,@RequestHeader(value="Authorization",required=false) String authorization) {
        return service.addProposal(roomId,bearer(authorization),request);
    }

    @PostMapping("/{roomId}/proposals/{proposalId}/vote")
    @Operation(summary="Bình chọn hoặc bỏ bình chọn",description="Bật/tắt phiếu của thành viên trong giai đoạn bình chọn.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse toggleVote(@PathVariable String roomId,@PathVariable String proposalId,@RequestHeader(value="Authorization",required=false) String authorization) {
        return service.toggleVote(roomId,proposalId,bearer(authorization));
    }

    @PostMapping("/{roomId}/phase")
    @Operation(summary="Đổi giai đoạn hoặc chốt phương án",description="Chỉ chủ phòng được chuyển COLLECTING → VOTING → FINALIZED.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse changePhase(@PathVariable String roomId,@Valid @RequestBody RoomRequests.ChangePhase request,@RequestHeader(value="Authorization",required=false) String authorization) {
        return service.changePhase(roomId,bearer(authorization),request);
    }

    @PostMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Rời phòng",description="Thành viên rời phòng và các phiếu của họ được xóa theo membership.")
    @SecurityRequirement(name="GuestSession")
    public void leave(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String authorization) {
        service.leave(roomId,bearer(authorization));
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
        String token = bearer(authorization);
        return service.get(roomId, token);
    }

    private static String bearer(String authorization) { return authorization!=null && authorization.startsWith("Bearer ")?authorization.substring(7):null; }

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
