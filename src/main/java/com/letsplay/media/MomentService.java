package com.letsplay.media;

import com.letsplay.auth.Account;
import com.letsplay.auth.AccountRepository;
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
public class MomentService {

    private final MomentRepository momentRepo;
    private final MediaAssetRepository mediaAssetRepo;
    private final MomentPlacementRepository placementRepo;
    private final PersonalGrantRepository grantRepo;
    private final RoomAlbumRepository albumRepo;
    private final RoomRepository roomRepo;
    private final AccountRepository accountRepo;
    private final PlacementReactionRepository reactionRepo;
    private final PlacementCommentRepository commentRepo;
    private final PhotoAuthService photoAuth;

    public MomentService(
            MomentRepository momentRepo,
            MediaAssetRepository mediaAssetRepo,
            MomentPlacementRepository placementRepo,
            PersonalGrantRepository grantRepo,
            RoomAlbumRepository albumRepo,
            RoomRepository roomRepo,
            AccountRepository accountRepo,
            PlacementReactionRepository reactionRepo,
            PlacementCommentRepository commentRepo,
            PhotoAuthService photoAuth
    ) {
        this.momentRepo = momentRepo;
        this.mediaAssetRepo = mediaAssetRepo;
        this.placementRepo = placementRepo;
        this.grantRepo = grantRepo;
        this.albumRepo = albumRepo;
        this.roomRepo = roomRepo;
        this.accountRepo = accountRepo;
        this.reactionRepo = reactionRepo;
        this.commentRepo = commentRepo;
        this.photoAuth = photoAuth;
    }

    @Transactional
    public MomentDtos.MomentReceipt publishMoment(MomentDtos.CreateMomentRequest request, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);

        if (request.mediaAssetId() == null || request.mediaAssetId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu mediaAssetId.");
        }

        // Idempotency replay check
        if (request.clientMutationId() != null && !request.clientMutationId().isBlank()) {
            Optional<Moment> existing = momentRepo.findByOwnerAccountIdAndClientMutationId(user.accountId(), request.clientMutationId());
            if (existing.isPresent()) {
                Moment m = existing.get();
                List<MomentPlacement> places = placementRepo.findAllByMomentId(m.getId());
                return toReceipt(m, places);
            }
        }

        MediaAsset asset = mediaAssetRepo.findById(request.mediaAssetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy media asset."));

        if (!asset.getOwnerAccountId().equals(user.accountId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải chủ sở hữu của file ảnh này.");
        }

        if (momentRepo.findByMediaAssetId(asset.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ảnh này đã được đăng thành khoảnh khắc trước đó.");
        }

        if (request.destinations() == null ||
                (request.destinations().personal() == null && request.destinations().room() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần chọn ít nhất một nơi chia sẻ (personal hoặc room).");
        }

        Room room = null;
        RoomAlbum album = null;
        if (request.destinations().room() != null) {
            String roomId = request.destinations().room().roomId();
            room = roomRepo.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng " + roomId));
            album = albumRepo.findByRoomId(roomId).orElseGet(() -> albumRepo.save(new RoomAlbum(roomId)));

            if (!album.isWritable(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Album phòng đã đóng, không thể đăng thêm ảnh.");
            }
        }

        // Create Moment
        String caption = request.caption() != null ? request.caption().trim() : "";
        Moment moment = new Moment(user.accountId(), asset.getId(), caption, request.clientMutationId());
        moment = momentRepo.save(moment);

        List<MomentPlacement> createdPlacements = new ArrayList<>();

        if (room != null && album != null) {
            MomentPlacement roomPlacement = MomentPlacement.forRoom(moment.getId(), room.getId(), album.getId());
            createdPlacements.add(placementRepo.save(roomPlacement));
        }

        if (request.destinations().personal() != null) {
            MomentPlacement personalPlacement = MomentPlacement.forPersonal(moment.getId());
            personalPlacement = placementRepo.save(personalPlacement);
            createdPlacements.add(personalPlacement);

            List<String> recipients = request.destinations().personal().recipientAccountIds();
            if (recipients != null && !recipients.isEmpty()) {
                for (String recipientId : recipients) {
                    if (recipientId != null && !recipientId.isBlank() && !recipientId.equals(user.accountId())) {
                        grantRepo.save(new PersonalGrant(personalPlacement.getId(), recipientId));
                    }
                }
            }
        }

        return toReceipt(moment, createdPlacements);
    }

    @Transactional
    public MomentDtos.MomentReceipt addPlacements(String momentId, MomentDtos.AddPlacementRequest request, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        Moment moment = momentRepo.findByIdAndOwnerAccountId(momentId, user.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoảnh khắc của bạn."));

        if ("DELETED".equalsIgnoreCase(moment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khoảnh khắc đã bị xóa.");
        }

        List<MomentPlacement> existing = placementRepo.findAllByMomentIdAndStatus(momentId, "ACTIVE");
        boolean hasRoom = existing.stream().anyMatch(p -> "ROOM".equalsIgnoreCase(p.getKind()));
        boolean hasPersonal = existing.stream().anyMatch(p -> "PERSONAL".equalsIgnoreCase(p.getKind()));

        List<MomentPlacement> created = new ArrayList<>();

        if (request.destinations().room() != null && !hasRoom) {
            String roomId = request.destinations().room().roomId();
            Room room = roomRepo.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng " + roomId));
            RoomAlbum album = albumRepo.findByRoomId(roomId).orElseGet(() -> albumRepo.save(new RoomAlbum(roomId)));
            if (!album.isWritable(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Album phòng đã đóng.");
            }
            MomentPlacement roomPlacement = MomentPlacement.forRoom(moment.getId(), room.getId(), album.getId());
            created.add(placementRepo.save(roomPlacement));
        }

        if (request.destinations().personal() != null && !hasPersonal) {
            MomentPlacement personalPlacement = MomentPlacement.forPersonal(moment.getId());
            personalPlacement = placementRepo.save(personalPlacement);
            created.add(personalPlacement);

            List<String> recipients = request.destinations().personal().recipientAccountIds();
            if (recipients != null) {
                for (String rec : recipients) {
                    grantRepo.save(new PersonalGrant(personalPlacement.getId(), rec));
                }
            }
        }

        moment.bumpRevision();
        momentRepo.save(moment);

        List<MomentPlacement> all = placementRepo.findAllByMomentId(momentId);
        return toReceipt(moment, all);
    }

    @Transactional(readOnly = true)
    public List<MomentDtos.MomentDetail> getMyMoments(String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        List<Moment> moments = momentRepo.findAllByOwnerAccountIdAndStatusOrderByCreatedAtDesc(user.accountId(), "PUBLISHED");
        if (moments.isEmpty()) return List.of();

        List<String> mediaAssetIds = moments.stream().map(Moment::getMediaAssetId).distinct().toList();
        Map<String, MediaAsset> assetMap = mediaAssetRepo.findAllById(mediaAssetIds).stream()
                .collect(Collectors.toMap(MediaAsset::getId, a -> a));

        return moments.stream().map(m -> {
            MediaAsset asset = assetMap.get(m.getMediaAssetId());
            String url = asset != null ? asset.getUrl() : "";
            String thumb = asset != null ? asset.getThumbnailUrl() : "";
            List<MomentPlacement> placements = placementRepo.findAllByMomentId(m.getId());
            List<MomentDtos.PlacementRef> refs = placements.stream()
                    .map(p -> new MomentDtos.PlacementRef(p.getId(), p.getKind(), p.getRoomId(), p.getAlbumId(), p.getStatus()))
                    .toList();

            return new MomentDtos.MomentDetail(
                    m.getId(),
                    m.getMediaAssetId(),
                    url,
                    thumb,
                    m.getCaption(),
                    m.getStatus(),
                    m.getRevision(),
                    m.getPublishedAt(),
                    refs
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public MomentDtos.MomentDetail getMoment(String id, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        Moment m = momentRepo.findByIdAndOwnerAccountId(id, user.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoảnh khắc."));

        MediaAsset asset = mediaAssetRepo.findById(m.getMediaAssetId()).orElse(null);
        String url = asset != null ? asset.getUrl() : "";
        String thumb = asset != null ? asset.getThumbnailUrl() : "";
        List<MomentPlacement> placements = placementRepo.findAllByMomentId(m.getId());
        List<MomentDtos.PlacementRef> refs = placements.stream()
                .map(p -> new MomentDtos.PlacementRef(p.getId(), p.getKind(), p.getRoomId(), p.getAlbumId(), p.getStatus()))
                .toList();

        return new MomentDtos.MomentDetail(
                m.getId(),
                m.getMediaAssetId(),
                url,
                thumb,
                m.getCaption(),
                m.getStatus(),
                m.getRevision(),
                m.getPublishedAt(),
                refs
        );
    }

    @Transactional
    public void deleteMoment(String momentId, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        Moment moment = momentRepo.findByIdAndOwnerAccountId(momentId, user.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoảnh khắc của bạn."));

        moment.markDeleted();
        momentRepo.save(moment);

        List<MomentPlacement> placements = placementRepo.findAllByMomentId(momentId);
        for (MomentPlacement p : placements) {
            p.remove(user.accountId(), "Xóa khoảnh khắc");
            placementRepo.save(p);
        }
    }

    @Transactional(readOnly = true)
    public List<MomentDtos.PhotoPlacementView> getPhotoFeed(String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);

        // 1. Own personal placements
        List<Moment> myMoments = momentRepo.findAllByOwnerAccountIdAndStatusOrderByCreatedAtDesc(user.accountId(), "PUBLISHED");
        Set<String> myMomentIds = myMoments.stream().map(Moment::getId).collect(Collectors.toSet());

        // 2. Granted personal placements
        List<PersonalGrant> grants = grantRepo.findAllByGranteeAccountIdOrderByCreatedAtDesc(user.accountId());
        Set<String> grantedPlacementIds = grants.stream().map(PersonalGrant::getPlacementId).collect(Collectors.toSet());

        List<MomentPlacement> feedPlacements = new ArrayList<>();
        for (String mId : myMomentIds) {
            placementRepo.findByMomentIdAndKindAndStatus(mId, "PERSONAL", "ACTIVE")
                    .ifPresent(feedPlacements::add);
        }
        for (String pId : grantedPlacementIds) {
            placementRepo.findById(pId)
                    .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                    .ifPresent(p -> {
                        if (feedPlacements.stream().noneMatch(existing -> existing.getId().equals(p.getId()))) {
                            feedPlacements.add(p);
                        }
                    });
        }

        feedPlacements.sort(Comparator.comparing(MomentPlacement::getCreatedAt).reversed());

        List<String> momentIds = feedPlacements.stream().map(MomentPlacement::getMomentId).distinct().toList();
        Map<String, Moment> momentMap = momentRepo.findAllById(momentIds).stream()
                .collect(Collectors.toMap(Moment::getId, m -> m));

        List<String> mediaAssetIds = momentMap.values().stream().map(Moment::getMediaAssetId).distinct().toList();
        Map<String, MediaAsset> assetMap = mediaAssetRepo.findAllById(mediaAssetIds).stream()
                .collect(Collectors.toMap(MediaAsset::getId, a -> a));

        List<String> ownerIds = momentMap.values().stream().map(Moment::getOwnerAccountId).distinct().toList();
        Map<String, Account> accountMap = accountRepo.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        return feedPlacements.stream().map(p -> {
            Moment m = momentMap.get(p.getMomentId());
            if (m == null) return null;
            MediaAsset asset = assetMap.get(m.getMediaAssetId());
            if (asset == null) return null;
            Account author = accountMap.get(m.getOwnerAccountId());
            String authorName = author != null ? author.getDisplayName() : "Bạn bè";

            long reactionCount = reactionRepo.countByPlacementId(p.getId());
            String myReaction = reactionRepo.findByPlacementIdAndAccountId(p.getId(), user.accountId())
                    .map(PlacementReaction::getReactionType).orElse(null);

            return new MomentDtos.PhotoPlacementView(
                    p.getId(),
                    m.getId(),
                    p.getKind(),
                    null,
                    null,
                    asset.getUrl(),
                    asset.getThumbnailUrl(),
                    m.getCaption(),
                    p.getCreatedAt(),
                    new MomentDtos.AuthorRef(m.getOwnerAccountId(), authorName),
                    reactionCount,
                    myReaction,
                    0
            );
        }).filter(Objects::nonNull).toList();
    }

    private MomentDtos.MomentReceipt toReceipt(Moment m, List<MomentPlacement> placements) {
        List<MomentDtos.PlacementRef> refs = placements.stream()
                .map(p -> new MomentDtos.PlacementRef(p.getId(), p.getKind(), p.getRoomId(), p.getAlbumId(), p.getStatus()))
                .toList();
        return new MomentDtos.MomentReceipt(m.getId(), m.getRevision(), m.getPublishedAt(), refs);
    }
}
