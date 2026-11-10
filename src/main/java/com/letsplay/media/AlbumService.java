package com.letsplay.media;

import com.letsplay.auth.Account;
import com.letsplay.auth.AccountRepository;
import com.letsplay.room.Membership;
import com.letsplay.room.MembershipRepository;
import com.letsplay.room.Room;
import com.letsplay.room.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlbumService {

    private final RoomAlbumRepository albumRepo;
    private final RoomRepository roomRepo;
    private final MembershipRepository membershipRepo;
    private final MomentPlacementRepository placementRepo;
    private final MomentRepository momentRepo;
    private final MediaAssetRepository mediaAssetRepo;
    private final PlacementReactionRepository reactionRepo;
    private final PlacementCommentRepository placementCommentRepo;
    private final AlbumCommentRepository albumCommentRepo;
    private final AccountRepository accountRepo;
    private final PhotoAuthService photoAuth;

    public AlbumService(
            RoomAlbumRepository albumRepo,
            RoomRepository roomRepo,
            MembershipRepository membershipRepo,
            MomentPlacementRepository placementRepo,
            MomentRepository momentRepo,
            MediaAssetRepository mediaAssetRepo,
            PlacementReactionRepository reactionRepo,
            PlacementCommentRepository placementCommentRepo,
            AlbumCommentRepository albumCommentRepo,
            AccountRepository accountRepo,
            PhotoAuthService photoAuth
    ) {
        this.albumRepo = albumRepo;
        this.roomRepo = roomRepo;
        this.membershipRepo = membershipRepo;
        this.placementRepo = placementRepo;
        this.momentRepo = momentRepo;
        this.mediaAssetRepo = mediaAssetRepo;
        this.reactionRepo = reactionRepo;
        this.placementCommentRepo = placementCommentRepo;
        this.albumCommentRepo = albumCommentRepo;
        this.accountRepo = accountRepo;
        this.photoAuth = photoAuth;
    }

    @Transactional
    public AlbumDtos.RoomAlbumView getOrCreateRoomAlbum(String roomId, String token) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));

        RoomAlbum album = albumRepo.findByRoomId(roomId).orElseGet(() -> albumRepo.save(new RoomAlbum(roomId)));
        return toAlbumView(album, room);
    }

    @Transactional(readOnly = true)
    public List<MomentDtos.PhotoPlacementView> getAlbumPhotos(String albumId, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        RoomAlbum album = albumRepo.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy album."));

        List<MomentPlacement> placements = placementRepo.findAllByAlbumIdAndStatusOrderByCreatedAtDesc(albumId, "ACTIVE");
        if (placements.isEmpty()) {
            return List.of();
        }

        List<String> momentIds = placements.stream().map(MomentPlacement::getMomentId).distinct().toList();
        Map<String, Moment> momentMap = momentRepo.findAllById(momentIds).stream()
                .collect(Collectors.toMap(Moment::getId, m -> m));

        List<String> mediaAssetIds = momentMap.values().stream().map(Moment::getMediaAssetId).distinct().toList();
        Map<String, MediaAsset> mediaAssetMap = mediaAssetRepo.findAllById(mediaAssetIds).stream()
                .collect(Collectors.toMap(MediaAsset::getId, m -> m));

        List<String> ownerIds = momentMap.values().stream().map(Moment::getOwnerAccountId).distinct().toList();
        Map<String, Account> accountMap = accountRepo.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        return placements.stream().map(p -> {
            Moment m = momentMap.get(p.getMomentId());
            if (m == null) return null;
            MediaAsset asset = mediaAssetMap.get(m.getMediaAssetId());
            if (asset == null) return null;
            Account author = accountMap.get(m.getOwnerAccountId());
            String authorName = author != null ? author.getDisplayName() : "Thành viên";

            long reactionCount = reactionRepo.countByPlacementId(p.getId());
            String myReaction = reactionRepo.findByPlacementIdAndAccountId(p.getId(), user.accountId())
                    .map(PlacementReaction::getReactionType).orElse(null);
            long commentCount = placementCommentRepo.countByPlacementIdAndState(p.getId(), "ACTIVE");

            return new MomentDtos.PhotoPlacementView(
                    p.getId(),
                    m.getId(),
                    p.getKind(),
                    p.getRoomId(),
                    p.getAlbumId(),
                    asset.getUrl(),
                    asset.getThumbnailUrl(),
                    m.getCaption(),
                    p.getCreatedAt(),
                    new MomentDtos.AuthorRef(m.getOwnerAccountId(), authorName),
                    reactionCount,
                    myReaction,
                    commentCount
            );
        }).filter(Objects::nonNull).toList();
    }

    @Transactional
    public void removePlacement(String placementId, String token, String reason) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        MomentPlacement placement = placementRepo.findById(placementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh."));

        Moment moment = momentRepo.findById(placement.getMomentId()).orElse(null);
        boolean isOwner = moment != null && moment.getOwnerAccountId().equals(user.accountId());

        boolean isHost = false;
        if (placement.getRoomId() != null) {
            isHost = isUserHost(placement.getRoomId(), user);
        }

        if (!isOwner && !isHost) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ phòng hoặc tác giả mới có quyền gỡ ảnh khỏi album.");
        }

        placement.remove(user.accountId(), reason);
        placementRepo.save(placement);
    }

    @Transactional
    public AlbumDtos.RoomAlbumView updatePostEndPolicy(String albumId, String token, AlbumDtos.PostEndPolicyRequest request) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        RoomAlbum album = albumRepo.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy album."));
        Room room = roomRepo.findById(album.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));

        if (!isUserHost(room.getId(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ phòng mới có quyền thiết lập chính sách album sau kết thúc.");
        }

        String mode = request.mode() != null ? request.mode().toUpperCase() : "OPEN";
        Integer hours = request.windowHours();
        if ("WINDOWED".equals(mode)) {
            if (hours == null || hours < 1 || hours > 720) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời hạn đóng góp (windowHours) phải từ 1 đến 720 giờ.");
            }
        }

        album.updatePolicy(mode, hours, room.getEndedAt());
        album = albumRepo.save(album);
        return toAlbumView(album, room);
    }

    // Reaction operations
    @Transactional
    public void setPlacementReaction(String placementId, String token, String type) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        MomentPlacement placement = placementRepo.findById(placementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí ảnh."));

        if ("ROOM".equalsIgnoreCase(placement.getKind()) && placement.getAlbumId() != null) {
            RoomAlbum album = albumRepo.findById(placement.getAlbumId()).orElse(null);
            if (album != null && !album.isWritable(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Album đã đóng, không thể thả cảm xúc.");
            }
        }

        PlacementReaction reaction = reactionRepo.findByPlacementIdAndAccountId(placementId, user.accountId())
                .orElseGet(() -> new PlacementReaction(placementId, user.accountId(), type));
        reaction.setReactionType(type != null ? type.toUpperCase() : "HEART");
        reactionRepo.save(reaction);
    }

    @Transactional
    public void deletePlacementReaction(String placementId, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        reactionRepo.deleteByPlacementIdAndAccountId(placementId, user.accountId());
    }

    // Placement comments
    @Transactional
    public AlbumDtos.CommentView addPlacementComment(String placementId, String token, AlbumDtos.CommentRequest request) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        MomentPlacement placement = placementRepo.findById(placementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh."));

        if (!"ROOM".equalsIgnoreCase(placement.getKind())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ hỗ trợ bình luận đối với ảnh chia sẻ trong phòng.");
        }

        if (placement.getAlbumId() != null) {
            RoomAlbum album = albumRepo.findById(placement.getAlbumId()).orElse(null);
            if (album != null && !album.isWritable(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Album đã đóng, không thể gửi bình luận.");
            }
        }

        if (request.text() == null || request.text().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung bình luận không được để trống.");
        }

        PlacementComment comment = new PlacementComment(
                placementId,
                user.accountId(),
                request.text().trim(),
                request.clientMutationId()
        );
        comment = placementCommentRepo.save(comment);

        return new AlbumDtos.CommentView(
                comment.getId(),
                new MomentDtos.AuthorRef(user.accountId(), user.displayName()),
                comment.getText(),
                comment.getCreatedAt(),
                true,
                comment.getState()
        );
    }

    @Transactional(readOnly = true)
    public List<AlbumDtos.CommentView> getPlacementComments(String placementId, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        List<PlacementComment> list = placementCommentRepo.findAllByPlacementIdAndStateOrderByCreatedAtAsc(placementId, "ACTIVE");
        if (list.isEmpty()) return List.of();

        List<String> authorIds = list.stream().map(PlacementComment::getAuthorAccountId).distinct().toList();
        Map<String, Account> accounts = accountRepo.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        return list.stream().map(c -> {
            Account a = accounts.get(c.getAuthorAccountId());
            String name = a != null ? a.getDisplayName() : "Thành viên";
            return new AlbumDtos.CommentView(
                    c.getId(),
                    new MomentDtos.AuthorRef(c.getAuthorAccountId(), name),
                    c.getText(),
                    c.getCreatedAt(),
                    c.getAuthorAccountId().equals(user.accountId()),
                    c.getState()
            );
        }).toList();
    }

    @Transactional
    public void deletePlacementComment(String commentId, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        PlacementComment comment = placementCommentRepo.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận."));

        if (!comment.getAuthorAccountId().equals(user.accountId())) {
            // Check if user is host of room
            MomentPlacement placement = placementRepo.findById(comment.getPlacementId()).orElse(null);
            if (placement == null || placement.getRoomId() == null || !isUserHost(placement.getRoomId(), user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa bình luận này.");
            }
        }

        comment.markDeleted();
        placementCommentRepo.save(comment);
    }

    // Album comments
    @Transactional
    public AlbumDtos.CommentView addAlbumComment(String albumId, String token, AlbumDtos.CommentRequest request) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        RoomAlbum album = albumRepo.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy album."));

        if (!album.isWritable(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Album đã đóng, không thể gửi bình luận.");
        }

        if (request.text() == null || request.text().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung bình luận không được để trống.");
        }

        AlbumComment comment = new AlbumComment(
                albumId,
                user.accountId(),
                request.text().trim(),
                request.clientMutationId()
        );
        comment = albumCommentRepo.save(comment);

        return new AlbumDtos.CommentView(
                comment.getId(),
                new MomentDtos.AuthorRef(user.accountId(), user.displayName()),
                comment.getText(),
                comment.getCreatedAt(),
                true,
                comment.getState()
        );
    }

    @Transactional(readOnly = true)
    public List<AlbumDtos.CommentView> getAlbumComments(String albumId, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        List<AlbumComment> list = albumCommentRepo.findAllByAlbumIdAndStateOrderByCreatedAtAsc(albumId, "ACTIVE");
        if (list.isEmpty()) return List.of();

        List<String> authorIds = list.stream().map(AlbumComment::getAuthorAccountId).distinct().toList();
        Map<String, Account> accounts = accountRepo.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        return list.stream().map(c -> {
            Account a = accounts.get(c.getAuthorAccountId());
            String name = a != null ? a.getDisplayName() : "Thành viên";
            return new AlbumDtos.CommentView(
                    c.getId(),
                    new MomentDtos.AuthorRef(c.getAuthorAccountId(), name),
                    c.getText(),
                    c.getCreatedAt(),
                    c.getAuthorAccountId().equals(user.accountId()),
                    c.getState()
            );
        }).toList();
    }

    @Transactional
    public void deleteAlbumComment(String commentId, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        AlbumComment comment = albumCommentRepo.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bình luận."));

        if (!comment.getAuthorAccountId().equals(user.accountId())) {
            RoomAlbum album = albumRepo.findById(comment.getAlbumId()).orElse(null);
            if (album == null || !isUserHost(album.getRoomId(), user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa bình luận này.");
            }
        }

        comment.markDeleted();
        albumCommentRepo.save(comment);
    }

    private AlbumDtos.RoomAlbumView toAlbumView(RoomAlbum album, Room room) {
        boolean writable = album.isWritable(Instant.now());
        String effectiveWriteState = writable ? "OPEN" : "READ_ONLY";
        String readOnlyReason = writable ? null : ("VIEW_ONLY".equalsIgnoreCase(album.getPolicyMode())
                ? "Album chỉ xem sau khi kết thúc chuyến đi" : "Album đã hết thời hạn đóng góp ảnh");

        long photoCount = placementRepo.countByAlbumIdAndStatus(album.getId(), "ACTIVE");
        long commentCount = albumCommentRepo.countByAlbumIdAndState(album.getId(), "ACTIVE");

        String coverUrl = null;
        List<MomentPlacement> latest = placementRepo.findAllByAlbumIdAndStatusOrderByCreatedAtDesc(album.getId(), "ACTIVE");
        if (!latest.isEmpty()) {
            Moment m = momentRepo.findById(latest.get(0).getMomentId()).orElse(null);
            if (m != null) {
                MediaAsset a = mediaAssetRepo.findById(m.getMediaAssetId()).orElse(null);
                if (a != null) {
                    coverUrl = a.getThumbnailUrl();
                }
            }
        }

        return new AlbumDtos.RoomAlbumView(
                album.getId(),
                album.getRoomId(),
                album.getPolicyMode(),
                album.getWindowHours(),
                album.getEndedAt() != null ? album.getEndedAt() : room.getEndedAt(),
                album.getDeadlineAt(),
                album.getRevision(),
                effectiveWriteState,
                readOnlyReason,
                photoCount,
                commentCount,
                coverUrl
        );
    }

    private boolean isUserHost(String roomId, UserIdentity user) {
        return membershipRepo.findAllByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .anyMatch(m -> "HOST".equalsIgnoreCase(m.getRole()) &&
                        (user.accountId().equals(m.getAccountId()) || user.displayName().equals(m.getDisplayName())));
    }
}
