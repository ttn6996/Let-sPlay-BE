package com.letsplay.media;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "08. Media · Lưu trữ đa tầng", description = "Upload và quản lý tài nguyên ảnh hỗ trợ Google Drive, Cloudinary và Local storage fallback.")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tải ảnh lên hệ thống", description = "Lưu trữ ảnh qua chuỗi ưu tiên: Google Drive API -> Cloudinary -> Local backend storage.")
    @SecurityRequirement(name = "GuestSession")
    public MediaDtos.MediaAssetResponse uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return mediaService.upload(file, bearer(authorization));
    }

    @GetMapping("/media-assets/{mediaAssetId}")
    @Operation(summary = "Lấy thông tin media asset")
    @SecurityRequirement(name = "GuestSession")
    public MediaDtos.MediaAssetResponse getMediaAsset(
            @PathVariable String mediaAssetId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return mediaService.getAsset(mediaAssetId, bearer(authorization));
    }

    private static String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
    }
}
