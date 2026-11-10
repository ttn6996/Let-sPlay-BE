package com.letsplay.media;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "09. Khoảnh khắc · Moments", description = "Đăng ảnh, quản lý nơi chia sẻ cá nhân & phòng kèo.")
public class MomentController {

    private final MomentService momentService;

    public MomentController(MomentService momentService) {
        this.momentService = momentService;
    }

    @PostMapping("/moments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng khoảnh khắc mới", description = "Xuất bản ảnh lên album phòng hoặc dòng cá nhân.")
    @SecurityRequirement(name = "GuestSession")
    public MomentDtos.MomentReceipt createMoment(
            @RequestBody MomentDtos.CreateMomentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return momentService.publishMoment(request, bearer(authorization));
    }

    @PostMapping("/moments/{id}/placements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm nơi chia sẻ cho khoảnh khắc")
    @SecurityRequirement(name = "GuestSession")
    public MomentDtos.MomentReceipt addPlacements(
            @PathVariable String id,
            @RequestBody MomentDtos.AddPlacementRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return momentService.addPlacements(id, request, bearer(authorization));
    }

    @GetMapping("/me/moments")
    @Operation(summary = "Thư viện ảnh của tôi", description = "Danh sách tất cả khoảnh khắc do tôi chụp.")
    @SecurityRequirement(name = "GuestSession")
    public List<MomentDtos.MomentDetail> getMyMoments(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return momentService.getMyMoments(bearer(authorization));
    }

    @GetMapping("/moments/{id}")
    @Operation(summary = "Chi tiết một khoảnh khắc")
    @SecurityRequirement(name = "GuestSession")
    public MomentDtos.MomentDetail getMoment(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return momentService.getMoment(id, bearer(authorization));
    }

    @DeleteMapping("/moments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa khoảnh khắc", description = "Gỡ khỏi mọi nơi đăng (phòng và cá nhân).")
    @SecurityRequirement(name = "GuestSession")
    public void deleteMoment(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        momentService.deleteMoment(id, bearer(authorization));
    }

    @GetMapping("/photo-feed")
    @Operation(summary = "Bảng tin ảnh bạn bè", description = "Dòng thời gian ảnh cá nhân từ bản thân và bạn bè đã cấp quyền.")
    @SecurityRequirement(name = "GuestSession")
    public List<MomentDtos.PhotoPlacementView> getPhotoFeed(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return momentService.getPhotoFeed(bearer(authorization));
    }

    private static String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
    }
}
