package com.letsplay.media;

import java.time.Instant;
import java.util.List;

public class MomentDtos {

    public record PersonalDestination(String audienceMode, List<String> recipientAccountIds) {}
    public record RoomDestination(String roomId) {}

    public record Destinations(
            PersonalDestination personal,
            RoomDestination room
    ) {}

    public record CreateMomentRequest(
            String clientMutationId,
            String mediaAssetId,
            String caption,
            Destinations destinations
    ) {}

    public record AddPlacementRequest(
            String clientMutationId,
            Destinations destinations
    ) {}

    public record PlacementRef(
            String id,
            String kind,
            String roomId,
            String albumId,
            String status
    ) {}

    public record MomentReceipt(
            String momentId,
            int revision,
            Instant publishedAt,
            List<PlacementRef> placements
    ) {}

    public record AuthorRef(
            String id,
            String displayName
    ) {}

    public record PhotoPlacementView(
            String placementId,
            String momentId,
            String kind,
            String roomId,
            String albumId,
            String url,
            String thumbnailUrl,
            String caption,
            Instant createdAt,
            AuthorRef author,
            long reactionCount,
            String myReaction,
            long commentCount
    ) {}

    public record MomentDetail(
            String momentId,
            String mediaAssetId,
            String url,
            String thumbnailUrl,
            String caption,
            String status,
            int revision,
            Instant publishedAt,
            List<PlacementRef> placements
    ) {}
}
