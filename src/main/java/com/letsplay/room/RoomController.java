package com.letsplay.room;

import com.letsplay.realtime.RoomRealtimeHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final RoomPushService pushService;

    public RoomController(RoomService service, RoomRealtimeHub realtimeHub, RoomPushService pushService) {
        this.service = service;
        this.realtimeHub = realtimeHub;
        this.pushService=pushService;
    }

    @PostMapping(value="/{roomId}/push-subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Bật thông báo đẩy",description="Lưu Web Push subscription cho membership hiện tại để nhận thông báo đề xuất mới.")
    public void subscribePush(@PathVariable String roomId,@Valid @RequestBody PushSubscriptionRequest request,@RequestHeader(value="Authorization",required=false) String authorization){
        RoomAccess access=service.authorize(roomId,bearer(authorization));
        pushService.subscribe(roomId,access.member().getId(),request);
    }

    @PutMapping("/{roomId}/availability/options")
    @Operation(summary="Thiết lập poll giờ hẹn",description="Chủ phòng sửa tiêu đề poll và danh sách giờ để thành viên chọn nhiều thời điểm có thể tham gia.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse updateAvailabilityOptions(@PathVariable String roomId,@Valid @RequestBody AvailabilityOptionsRequest request,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.updateAvailabilityOptions(roomId,bearer(authorization),request);
    }

    @PostMapping("/{roomId}/availability/slots")
    @Operation(summary="Đề xuất khung giờ",description="Mọi thành viên có thể thêm các khung giờ mới vào poll; phiếu và thảo luận đang có được giữ nguyên.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse addAvailabilitySlots(@PathVariable String roomId,@Valid @RequestBody AddAvailabilitySlotsRequest request,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.addAvailabilitySlots(roomId,bearer(authorization),request);
    }

    @PostMapping("/{roomId}/availability/{slotId}/comments")
    @Operation(summary="Thảo luận khung giờ",description="Gửi bình luận ẩn danh trong khung giờ; chỉ chính người viết thấy nhãn Bạn.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse commentOnAvailabilitySlot(@PathVariable String roomId,@PathVariable String slotId,@Valid @RequestBody RoomCommentRequest request,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.commentOnAvailabilitySlot(roomId,slotId,bearer(authorization),request);
    }

    @PutMapping("/{roomId}/availability")
    @Operation(summary="Cập nhật giờ có thể tham gia",description="Thay toàn bộ các lựa chọn giờ của membership hiện tại; danh tính người chọn không được chia sẻ với thành viên khác.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse updateAvailabilityVotes(@PathVariable String roomId,@Valid @RequestBody AvailabilityVotesRequest request,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.updateAvailabilityVotes(roomId,bearer(authorization),request);
    }

    @PostMapping("/{roomId}/availability/{slotId}/select")
    @Operation(summary="Chốt giờ phù hợp",description="Chủ phòng đặt ngày và giờ của phòng theo khung giờ đã thống nhất nhiều người tham gia nhất.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse selectAvailabilitySlot(@PathVariable String roomId,@PathVariable String slotId,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.selectAvailabilitySlot(roomId,slotId,bearer(authorization));
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

    @PostMapping("/{roomId}/proposals/{proposalId}/comments")
    @Operation(summary="Thảo luận đề xuất",description="Gửi bình luận ẩn danh vào đề xuất để các thành viên trao đổi ngay trong phòng.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse commentOnProposal(@PathVariable String roomId,@PathVariable String proposalId,@Valid @RequestBody RoomCommentRequest request,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.commentOnProposal(roomId,proposalId,bearer(authorization),request);
    }

    @PostMapping("/{roomId}/phase")
    @Operation(summary="Đổi giai đoạn hoặc chốt phương án",description="Chủ phòng có thể chuyển COLLECTING ↔ VOTING trước khi chốt. Quay lại COLLECTING giữ nguyên phiếu đã bầu; phòng vẫn có thể tiếp tục bình chọn sau khi thêm đề xuất. FINALIZED là trạng thái cuối.")
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

    @PutMapping("/{roomId}/proposals/{proposalId}")
    @Operation(summary="Sửa đề xuất",description="Người tạo hoặc chủ phòng có thể sửa đề xuất trước khi chốt.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse updateProposal(@PathVariable String roomId,@PathVariable String proposalId,@Valid @RequestBody RoomRequests.CreateProposal request,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.updateProposal(roomId,proposalId,bearer(authorization),request);
    }

    @DeleteMapping("/{roomId}/proposals/{proposalId}")
    @Operation(summary="Xóa đề xuất",description="Người tạo hoặc chủ phòng có thể xóa đề xuất trước khi chốt.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse deleteProposal(@PathVariable String roomId,@PathVariable String proposalId,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.deleteProposal(roomId,proposalId,bearer(authorization));
    }

    @PutMapping("/{roomId}/proposals/{proposalId}/completion")
    @Operation(summary="Đánh dấu đề xuất đã thực hiện",description="Thành viên có thể cập nhật trạng thái đã thực hiện của phương án trong phòng.")
    @SecurityRequirement(name="GuestSession")
    public RoomResponse setProposalCompleted(@PathVariable String roomId,@PathVariable String proposalId,@Valid @RequestBody ProposalCompletionRequest request,@RequestHeader(value="Authorization",required=false) String authorization){
        return service.setProposalCompleted(roomId,proposalId,bearer(authorization),request.completed());
    }

    @DeleteMapping("/{roomId}/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Mời thành viên ra khỏi phòng",description="Chỉ chủ phòng mới được mời thành viên khác ra khỏi phòng.")
    @SecurityRequirement(name="GuestSession")
    public void kickMember(@PathVariable String roomId,@PathVariable String membershipId,@RequestHeader(value="Authorization",required=false) String authorization){
        service.kickMember(roomId,membershipId,bearer(authorization));
    }

    @GetMapping("/mine")
    @Operation(summary="Danh sách phòng của tôi",description="Hợp nhất phòng theo tài khoản và các guest token được gửi lên.")
    public java.util.List<RoomService.MyRoomSummary> mine(@RequestHeader(value="Authorization",required=false) String authorization,
            @RequestHeader(value="X-Guest-Token",required=false) String guestToken,
            @RequestHeader(value="X-Guest-Tokens",required=false) String guestTokens){
        java.util.List<String> tokens=new java.util.ArrayList<>();
        if(guestToken!=null&&!guestToken.isBlank())tokens.add(guestToken);
        if(guestTokens!=null&&!guestTokens.isBlank())for(String item:guestTokens.split(","))if(!item.isBlank())tokens.add(item.trim());
        return service.myRooms(bearer(authorization),tokens);
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
        return realtimeHub.subscribe(roomId,snapshot.member().id(),Math.max(0,sinceRevision),snapshot.revision());
    }
}
