package com.letsplay.media;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "10. Album phòng & Tương tác", description = "Xem ảnh album, chính sách sau kết thúc, cảm xúc và bình luận.")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping("/rooms/{roomId}/album")
    @Operation(summary = "Xem thông tin album phòng")
    @SecurityRequirement(name = "GuestSession")
    public AlbumDtos.RoomAlbumView getRoomAlbum(
            @PathVariable String roomId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return albumService.getOrCreateRoomAlbum(roomId, bearer(authorization));
    }

    @GetMapping("/albums/{albumId}/photos")
    @Operation(summary = "Danh sách ảnh trong album phòng")
    @SecurityRequirement(name = "GuestSession")
    public List<MomentDtos.PhotoPlacementView> getAlbumPhotos(
            @PathVariable String albumId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return albumService.getAlbumPhotos(albumId, bearer(authorization));
    }

    @DeleteMapping("/placements/{placementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Gỡ ảnh khỏi album", description = "Chủ phòng hoặc tác giả có thể gỡ ảnh khỏi album.")
    @SecurityRequirement(name = "GuestSession")
    public void removePlacement(
            @PathVariable String placementId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        albumService.removePlacement(placementId, bearer(authorization), reason != null ? reason : "Gỡ bởi người dùng");
    }

    @PatchMapping("/albums/{albumId}/post-end-policy")
    @Operation(summary = "Cập nhật chính sách album sau kết thúc (Host)", description = "Thiết lập OPEN, WINDOWED hoặc VIEW_ONLY.")
    @SecurityRequirement(name = "GuestSession")
    public AlbumDtos.RoomAlbumView updatePostEndPolicy(
            @PathVariable String albumId,
            @RequestBody AlbumDtos.PostEndPolicyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return albumService.updatePostEndPolicy(albumId, bearer(authorization), request);
    }

    @PutMapping("/placements/{placementId}/my-reaction")
    @Operation(summary = "Thả cảm xúc vào ảnh", description = "HEART, LAUGH, WOW, FIRE, SAD")
    @SecurityRequirement(name = "GuestSession")
    public void setReaction(
            @PathVariable String placementId,
            @RequestBody AlbumDtos.ReactionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        albumService.setPlacementReaction(placementId, bearer(authorization), request.type());
    }

    @DeleteMapping("/placements/{placementId}/my-reaction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hủy cảm xúc của tôi trên ảnh")
    @SecurityRequirement(name = "GuestSession")
    public void deleteReaction(
            @PathVariable String placementId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        albumService.deletePlacementReaction(placementId, bearer(authorization));
    }

    @PostMapping("/placements/{placementId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bình luận trên ảnh phòng")
    @SecurityRequirement(name = "GuestSession")
    public AlbumDtos.CommentView addPlacementComment(
            @PathVariable String placementId,
            @RequestBody AlbumDtos.CommentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return albumService.addPlacementComment(placementId, bearer(authorization), request);
    }

    @GetMapping("/placements/{placementId}/comments")
    @Operation(summary = "Danh sách bình luận trên ảnh")
    @SecurityRequirement(name = "GuestSession")
    public List<AlbumDtos.CommentView> getPlacementComments(
            @PathVariable String placementId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return albumService.getPlacementComments(placementId, bearer(authorization));
    }

    @DeleteMapping("/photo-comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa bình luận trên ảnh")
    @SecurityRequirement(name = "GuestSession")
    public void deletePlacementComment(
            @PathVariable String commentId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        albumService.deletePlacementComment(commentId, bearer(authorization));
    }

    @PostMapping("/albums/{albumId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bình luận chung cho toàn bộ album phòng")
    @SecurityRequirement(name = "GuestSession")
    public AlbumDtos.CommentView addAlbumComment(
            @PathVariable String albumId,
            @RequestBody AlbumDtos.CommentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return albumService.addAlbumComment(albumId, bearer(authorization), request);
    }

    @GetMapping("/albums/{albumId}/comments")
    @Operation(summary = "Danh sách bình luận chung của album phòng")
    @SecurityRequirement(name = "GuestSession")
    public List<AlbumDtos.CommentView> getAlbumComments(
            @PathVariable String albumId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return albumService.getAlbumComments(albumId, bearer(authorization));
    }

    @DeleteMapping("/album-comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa bình luận của album")
    @SecurityRequirement(name = "GuestSession")
    public void deleteAlbumComment(
            @PathVariable String commentId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        albumService.deleteAlbumComment(commentId, bearer(authorization));
    }

    private static String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
    }
}
