package com.letsplay.media;

import java.time.Instant;
import java.util.List;

public class AlbumDtos {

    public record RoomAlbumView(
            String id,
            String roomId,
            String policyMode,
            Integer windowHours,
            Instant endedAt,
            Instant deadlineAt,
            int revision,
            String effectiveWriteState,
            String readOnlyReason,
            long visiblePhotoCount,
            long visibleAlbumCommentCount,
            String coverPhotoUrl
    ) {}

    public record PostEndPolicyRequest(
            String mode,
            Integer windowHours
    ) {}

    public record ReactionRequest(
            String type
    ) {}

    public record CommentRequest(
            String text,
            String clientMutationId
    ) {}

    public record CommentView(
            String id,
            MomentDtos.AuthorRef author,
            String text,
            Instant createdAt,
            boolean writtenByMe,
            String state
    ) {}

    public record ReactionSummary(
            String type,
            long count,
            List<MomentDtos.AuthorRef> recentUsers
    ) {}
}
