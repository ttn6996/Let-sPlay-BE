package com.letsplay.room;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms · Phòng", description = "Tạo phòng, xem trước mã mời và tham gia phòng.")
public class RoomController {
    private final RoomService service;

    public RoomController(RoomService service) { this.service = service; }

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
}
