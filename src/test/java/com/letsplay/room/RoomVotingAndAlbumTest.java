package com.letsplay.room;

import com.letsplay.media.RoomAlbum;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomVotingAndAlbumTest {

    @Test
    void test3LevelVotingAndBordaScore() {
        RoomVote voteStrong = new RoomVote("v1", "p1", "m1", 2);
        RoomVote voteSupport = new RoomVote("v2", "p1", "m2", 1);
        RoomVote votePass = new RoomVote("v3", "p1", "m3", 0);

        List<RoomVote> votes = List.of(voteStrong, voteSupport, votePass);
        long bordaScore = votes.stream().mapToLong(RoomVote::getVoteValue).sum();

        assertThat(bordaScore).isEqualTo(3); // 2 + 1 + 0 = 3
    }

    @Test
    void testMultiStageItinerary() {
        Room room = new Room("r1", "Trip to Da Lat", "DL1234", "req1", null, null, "Da Lat");
        room.setSelectedProposalIds("prop1,prop2,prop3");

        assertThat(room.getSelectedProposalIds()).isEqualTo("prop1,prop2,prop3");
        List<String> list = List.of(room.getSelectedProposalIds().split(","));
        assertThat(list).containsExactly("prop1", "prop2", "prop3");
    }

    @Test
    void testRoomAlbumPolicyLifecycle() {
        RoomAlbum album = new RoomAlbum("r1");
        Instant now = Instant.now();

        // 1. OPEN policy: always writable
        assertThat(album.getPolicyMode()).isEqualTo("OPEN");
        assertThat(album.isWritable(now)).isTrue();

        // 2. VIEW_ONLY policy: not writable
        album.updatePolicy("VIEW_ONLY", null, now);
        assertThat(album.isWritable(now)).isFalse();

        // 3. WINDOWED policy: writable within window, closed after deadline
        Instant endedAt = now.minus(10, ChronoUnit.HOURS);
        album.updatePolicy("WINDOWED", 24, endedAt); // 24 hours window, 14 hours remaining
        assertThat(album.isWritable(now)).isTrue();

        // After window expires (e.g. 25 hours after endedAt)
        Instant future = endedAt.plus(25, ChronoUnit.HOURS);
        assertThat(album.isWritable(future)).isFalse();
    }
}
