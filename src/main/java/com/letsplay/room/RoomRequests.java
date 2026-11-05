package com.letsplay.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.time.LocalDateTime;

public final class RoomRequests {
    private RoomRequests() {}

    public record CreateRoom(
            @NotNull(message = "Yêu cầu tạo phòng chưa hợp lệ.") UUID requestId,
            @NotNull(message = "Phiên khách chưa hợp lệ.") UUID guestId,
            @NotBlank(message = "Vui lòng nhập tên hiển thị.") @Size(max = 60, message = "Tên hiển thị tối đa 60 ký tự.") String displayName,
            @NotBlank(message = "Vui lòng nhập tên buổi hẹn.") @Size(max = 100, message = "Tên buổi hẹn tối đa 100 ký tự.") String roomName,
            LocalDate eventDate,
            LocalTime eventTime,
            @Size(max = 120, message = "Khu vực tối đa 120 ký tự.") String region) {}

    public record JoinRoom(
            @NotNull(message = "Phiên khách chưa hợp lệ.") UUID guestId,
            @NotBlank(message = "Vui lòng nhập tên hiển thị.") @Size(max = 60, message = "Tên hiển thị tối đa 60 ký tự.") String displayName,
            @NotBlank(message = "Vui lòng nhập mã tham gia.") @Size(min = 6, max = 8, message = "Mã tham gia gồm 6 đến 8 ký tự.") String inviteCode) {}

    public record PreviewRoom(
            @NotBlank(message = "Vui lòng nhập mã tham gia.") @Size(min = 6, max = 8, message = "Mã tham gia gồm 6 đến 8 ký tự.") String inviteCode) {}

    public record CreateProposal(
            @NotBlank @Size(max=120) String title,
            @Size(max=500) String description,
            LocalDateTime scheduledTime) {}

    public record ChangePhase(
            @NotBlank String phase,
            String selectedProposalId) {}
}
