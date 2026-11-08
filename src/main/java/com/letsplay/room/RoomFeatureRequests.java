package com.letsplay.room;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class RoomFeatureRequests {
    private RoomFeatureRequests() {}
    public record Plan(@NotBlank @Size(max=100) String name, LocalDate eventDate, LocalTime eventTime,
                       @Size(max=120) String region, @Size(max=2000) String notes,
                       @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
                       @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
                       @Size(max=1000) String weatherFallback, boolean cancelled) {}
    public record Attendance(@NotBlank String status) {}
    public record Preferences(@Size(max=1000) String preferences, @PositiveOrZero Long budgetMin, @PositiveOrZero Long budgetMax) {}
    public record Ride(boolean hasVehicle, @Min(0) @Max(20) int seatsAvailable, @Size(max=160) String pickupArea, @Size(max=300) String note) {}
    public record Allocation(@NotBlank String membershipId, @PositiveOrZero long amount) {}
    public record Expense(@NotBlank @Size(max=160) String title, @Positive long totalAmount,
                          @Size(max=1000) String note, @NotEmpty List<@Valid Allocation> payers,
                          @NotEmpty List<@Valid Allocation> shares) {}
    public record SettlementStatus(@NotBlank String status) {}
    public record Settlement(@NotBlank String fromMembershipId,@NotBlank String toMembershipId,@Positive long amount) {}
    public record CalendarLink(@NotBlank @Size(max=255) String eventId) {}
}
