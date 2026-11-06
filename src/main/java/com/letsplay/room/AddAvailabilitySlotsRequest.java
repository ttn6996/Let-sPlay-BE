package com.letsplay.room;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record AddAvailabilitySlotsRequest(@NotNull @Size(min = 1, max = 30) List<@NotNull LocalDateTime> startsAt) {}
