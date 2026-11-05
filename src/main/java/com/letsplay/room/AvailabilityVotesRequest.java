package com.letsplay.room;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AvailabilityVotesRequest(@NotNull @Size(max=30) List<@NotNull String> slotIds) {}
